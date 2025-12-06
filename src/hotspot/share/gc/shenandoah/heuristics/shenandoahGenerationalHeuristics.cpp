/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
 * Copyright (c) 2025, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */

#include "gc/shenandoah/heuristics/shenandoahGenerationalHeuristics.hpp"
#include "gc/shenandoah/shenandoahCollectionSet.hpp"
#include "gc/shenandoah/shenandoahCollectorPolicy.hpp"
#include "gc/shenandoah/shenandoahGeneration.hpp"
#include "gc/shenandoah/shenandoahGenerationalHeap.inline.hpp"
#include "gc/shenandoah/shenandoahInPlacePromoter.hpp"
#include "gc/shenandoah/shenandoahOldGeneration.hpp"
#include "gc/shenandoah/shenandoahYoungGeneration.hpp"
#include "gc/shenandoah/shenandoahTrace.hpp"
#include "logging/log.hpp"
#include "utilities/quickSort.hpp"

ShenandoahGenerationalHeuristics::ShenandoahGenerationalHeuristics(ShenandoahGeneration* generation)
        : ShenandoahAdaptiveHeuristics(generation), _generation(generation) {
}

void ShenandoahGenerationalHeuristics::choose_collection_set(ShenandoahCollectionSet* collection_set) {
  ShenandoahGenerationalHeap* heap = ShenandoahGenerationalHeap::heap();

  // Find the amount that will be promoted, regions that will be promoted in
  // place, and preselect older regions that will be promoted by evacuation.
  compute_evacuation_budgets(heap, collection_set);

  // Filter, sort and select remaining regions. Collect immediate garbage.
  filter_regions(heap, collection_set);

  if (!collection_set->is_empty()) {
    // Adjust evacuation budgets to reflect what is actually going to be evacuated.
    // Any unused budget will be returned to mutators when the freeset is rebuilt.
    adjust_evacuation_budgets(heap, collection_set);
  }
}

void ShenandoahGenerationalHeuristics::filter_regions(ShenandoahGenerationalHeap* heap, ShenandoahCollectionSet* collection_set) {
  // Check all pinned regions have updated status before choosing the collection set.
  heap->assert_pinned_region_status(_generation);

  // Step 1. Build up the region candidates we care about, rejecting losers and accepting winners right away.
  const size_t region_size_bytes = ShenandoahHeapRegion::region_size_bytes();
  const size_t num_regions = heap->num_regions();

  RegionData* candidates = _region_data;

  size_t cand_idx = 0;
  size_t preselected_candidates = 0;

  size_t total_garbage = 0;

  size_t immediate_garbage = 0;
  size_t immediate_regions = 0;

  size_t free = 0;
  size_t free_regions = 0;

  // This counts number of humongous regions that we intend to promote in this cycle.
  size_t humongous_regions_promoted = 0;
  // This counts number of regular regions that will be promoted in place.
  size_t regular_regions_promoted_in_place = 0;
  // This counts bytes of memory used by regular regions to be promoted in place.
  size_t regular_regions_promoted_usage = 0;
  // This counts bytes of memory free in regular regions to be promoted in place.
  size_t regular_regions_promoted_free = 0;
  // This counts bytes of garbage memory in regular regions to be promoted in place.
  size_t regular_regions_promoted_garbage = 0;

  for (size_t i = 0; i < num_regions; i++) {
    ShenandoahHeapRegion* region = heap->get_region(i);
    if (!_generation->contains(region)) {
      continue;
    }
    size_t garbage = region->garbage();
    total_garbage += garbage;
    if (region->is_empty()) {
      free_regions++;
      free += region_size_bytes;
    } else if (region->is_regular()) {
      if (!region->has_live()) {
        // We can recycle it right away and put it in the free set.
        immediate_regions++;
        immediate_garbage += garbage;
        region->make_trash_immediate();
      } else {
        // This is our candidate for later consideration.
        if (collection_set->is_in(i)) {
          // We have "preselected" this region for evacuation when we were computing promotion reserves
          assert(heap->is_tenurable(region), "Preselection filter");
          preselected_candidates++;
        } else if (region->is_young() && heap->is_tenurable(region)) {
          // Note that for GLOBAL GC, region may be OLD, and OLD regions do not qualify for pre-selection

          // This region is old enough to be promoted but it was not preselected, either because its garbage is below
          // ShenandoahOldGarbageThreshold so it will be promoted in place, or because there is not sufficient room
          // in old gen to hold the evacuated copies of this region's live data.  In both cases, we choose not to
          // place this region into the collection set.
          if (region->get_top_before_promote() != nullptr) {
            // Region was included for promotion-in-place
            regular_regions_promoted_in_place++;
            regular_regions_promoted_usage += region->used_before_promote();
            regular_regions_promoted_free += region->free();
            regular_regions_promoted_garbage += region->garbage();
          }
        } else {
          candidates[cand_idx].set_region_and_garbage(region, garbage);
          cand_idx++;
        }
      }
    } else if (region->is_humongous_start()) {
      // Reclaim humongous regions here, and count them as the immediate garbage
#ifdef ASSERT
      bool reg_live = region->has_live();
      bool bm_live = _generation->complete_marking_context()->is_marked(cast_to_oop(region->bottom()));
      assert(reg_live == bm_live,
             "Humongous liveness and marks should agree. Region live: %s; Bitmap live: %s; Region Live Words: %zu",
             BOOL_TO_STR(reg_live), BOOL_TO_STR(bm_live), region->get_live_data_words());
#endif
      if (!region->has_live()) {
        heap->trash_humongous_region_at(region);

        // Count only the start. Continuations would be counted on "trash" path
        immediate_regions++;
        immediate_garbage += garbage;
      } else {
        if (region->is_young() && heap->is_tenurable(region)) {
          oop obj = cast_to_oop(region->bottom());
          size_t humongous_regions = ShenandoahHeapRegion::required_regions(obj->size() * HeapWordSize);
          humongous_regions_promoted += humongous_regions;
        }
      }
    } else if (region->is_trash()) {
      // Count in just trashed humongous continuations
      immediate_regions++;
      immediate_garbage += garbage;
    }
  }
  heap->old_generation()->set_expected_humongous_region_promotions(humongous_regions_promoted);
  heap->old_generation()->set_expected_regular_region_promotions(regular_regions_promoted_in_place);
  log_info(gc, ergo)("Planning to promote in place %zu humongous regions and %zu"
                     " regular regions, spanning a total of %zu used bytes",
                     humongous_regions_promoted, regular_regions_promoted_in_place,
                     humongous_regions_promoted * ShenandoahHeapRegion::region_size_bytes() +
                     regular_regions_promoted_usage);

  // Step 2. Look back at garbage statistics, and decide if we want to collect anything,
  // given the amount of immediately reclaimable garbage. If we do, figure out the collection set.

  assert (immediate_garbage <= total_garbage,
          "Cannot have more immediate garbage than total garbage: %zu%s vs %zu%s",
          byte_size_in_proper_unit(immediate_garbage), proper_unit_for_byte_size(immediate_garbage),
          byte_size_in_proper_unit(total_garbage), proper_unit_for_byte_size(total_garbage));

  size_t immediate_percent = (total_garbage == 0) ? 0 : (immediate_garbage * 100 / total_garbage);

  bool doing_promote_in_place = (humongous_regions_promoted + regular_regions_promoted_in_place > 0);
  if (doing_promote_in_place || (preselected_candidates > 0) || (immediate_percent <= ShenandoahImmediateThreshold)) {
    // Only young collections need to prime the collection set.
    if (_generation->is_young()) {
      heap->old_generation()->heuristics()->prime_collection_set(collection_set);
    }

    // Call the subclasses to add young-gen regions into the collection set.
    choose_collection_set_from_regiondata(collection_set, candidates, cand_idx, immediate_garbage + free);
  }

  if (collection_set->has_old_regions()) {
    heap->shenandoah_policy()->record_mixed_cycle();
  }

  collection_set->summarize(total_garbage, immediate_garbage, immediate_regions);

  ShenandoahTracer::report_evacuation_info(collection_set,
                                           free_regions,
                                           humongous_regions_promoted,
                                           regular_regions_promoted_in_place,
                                           regular_regions_promoted_garbage,
                                           regular_regions_promoted_free,
                                           immediate_regions,
                                           immediate_garbage);
}

// Here's the algebra.
// Let SOEP = ShenandoahOldEvacRatioPercent,
//     OE = old evac,
//     YE = young evac, and
//     TE = total evac = OE + YE
// By definition:
//            SOEP/100 = OE/TE
//                     = OE/(OE+YE)
//  => SOEP/(100-SOEP) = OE/((OE+YE)-OE)         // componendo-dividendo: If a/b = c/d, then a/(b-a) = c/(d-c)
//                     = OE/YE
//  =>              OE = YE*SOEP/(100-SOEP)
size_t get_maximum_old_evacuation_reserve(size_t maximum_young_evacuation_reserve, size_t old_available) {
  // We have to be careful in the event that SOEP is set to 100 by the user.
  assert(ShenandoahOldEvacRatioPercent <= 100, "Error");
  if (ShenandoahOldEvacRatioPercent == 100) {
    return old_available;
  }

  const size_t ratio_of_old_in_collection_set = (maximum_young_evacuation_reserve * ShenandoahOldEvacRatioPercent) / (100 - ShenandoahOldEvacRatioPercent);
  return MIN2(ratio_of_old_in_collection_set, old_available);
}

void ShenandoahGenerationalHeuristics::compute_evacuation_budgets(ShenandoahGenerationalHeap* const heap, ShenandoahCollectionSet* collection_set) {
  assert(collection_set->is_empty(), "Collection set must be empty here");

  shenandoah_assert_generational();

  ShenandoahOldGeneration* const old_generation = heap->old_generation();
  ShenandoahYoungGeneration* const young_generation = heap->young_generation();

  // During initialization and phase changes, it is more likely that fewer objects die young and old-gen
  // memory is not yet full (or is in the process of being replaced).  During these times especially, it
  // is beneficial to loan memory from old-gen to young-gen during the evacuation and update-refs phases
  // of execution.

  // Calculate EvacuationReserve before PromotionReserve.  Evacuation is more critical than promotion.
  // If we cannot evacuate old-gen, we will not be able to reclaim old-gen memory.  Promotions are less
  // critical.  If we cannot promote, there may be degradation of young-gen memory because old objects
  // accumulate there until they can be promoted.  This increases the young-gen marking and evacuation work.

  // First priority is to reclaim the easy garbage out of young-gen.

  // maximum_young_evacuation_reserve is upper bound on memory to be evacuated out of young
  const size_t maximum_young_evacuation_reserve = (young_generation->max_capacity() * ShenandoahEvacReserve) / 100;
  size_t young_evacuation_reserve = MIN2(maximum_young_evacuation_reserve, young_generation->available_with_reserve());

  // maximum_old_evacuation_reserve is an upper bound on memory evacuated from old and evacuated to old (promoted),
  // clamped by the old generation space available.
  const size_t old_available = old_generation->available();
  const size_t maximum_old_evacuation_reserve = get_maximum_old_evacuation_reserve(maximum_young_evacuation_reserve, old_available);


  log_debug(gc, cset)("max_young_evac_reserver: " PROPERFMT", max_old_evac_reserve: " PROPERFMT ", old_available: " PROPERFMT,
                      PROPERFMTARGS(maximum_young_evacuation_reserve), PROPERFMTARGS(maximum_old_evacuation_reserve), PROPERFMTARGS(old_available));

  // Second priority is to reclaim garbage out of old-gen if there are old-gen collection candidates.  Third priority
  // is to promote as much as we have room to promote.  However, if old-gen memory is in short supply, this means young
  // GC is operating under "duress" and was unable to transfer the memory that we would normally expect.  In this case,
  // old-gen will refrain from compacting itself in order to allow a quicker young-gen cycle (by avoiding the update-refs
  // through ALL of old-gen).  If there is some memory available in old-gen, we will use this for promotions as promotions
  // do not add to the update-refs burden of GC.

  size_t old_evacuation_reserve, old_promo_reserve;
  if (_generation->is_global()) {
    // Global GC is typically triggered by user invocation of System.gc(), and typically indicates that there is lots
    // of garbage to be reclaimed because we are starting a new phase of execution.  Marking for global GC may take
    // significantly longer than typical young marking because we must mark through all old objects.  To expedite
    // evacuation and update-refs, we give emphasis to reclaiming garbage first, wherever that garbage is found.
    // Global GC will adjust generation sizes to accommodate the collection set it chooses.

    // Set old_promo_reserve to enforce that no regions are preselected for promotion.  Such regions typically
    // have relatively high memory utilization.  We still call select_aged_regions() because this will prepare for
    // promotions in place, if relevant.
    old_promo_reserve = 0;

    // Dedicate all available old memory to old_evacuation reserve.  This may be small, because old-gen is only
    // expanded based on an existing mixed evacuation workload at the end of the previous GC cycle.  We'll expand
    // the budget for evacuation of old during GLOBAL cset selection.
    old_evacuation_reserve = maximum_old_evacuation_reserve;
  } else if (old_generation->has_unprocessed_collection_candidates()) {
    // We reserved all old-gen memory at end of previous GC to hold anticipated evacuations to old-gen.  If this is
    // mixed evacuation, reserve all of this memory for compaction of old-gen and do not promote.  Prioritize compaction
    // over promotion in order to defragment OLD so that it will be better prepared to efficiently receive promoted memory.
    old_evacuation_reserve = maximum_old_evacuation_reserve;
    old_promo_reserve = 0;
  } else {
    // Make all old-evacuation memory for promotion, but if we can't use it all for promotion, we'll allow some evacuation.
    old_evacuation_reserve = 0;
    old_promo_reserve = maximum_old_evacuation_reserve;
  }
  assert(old_evacuation_reserve <= old_available, "Error");

  // We see too many old-evacuation failures if we force ourselves to evacuate into regions that are not initially empty.
  // So we limit the old-evacuation reserve to unfragmented memory.  Even so, old-evacuation is free to fill in nooks and
  // crannies within existing partially used regions and it generally tries to do so.
  const size_t old_free_unfragmented = old_generation->free_unaffiliated_regions() * ShenandoahHeapRegion::region_size_bytes();
  if (old_evacuation_reserve > old_free_unfragmented) {
    const size_t delta = old_evacuation_reserve - old_free_unfragmented;
    old_evacuation_reserve -= delta;
    // Let promo consume fragments of old-gen memory if not global
    if (!_generation->is_global()) {
      old_promo_reserve += delta;
    }
  }

  // Preselect regions for promotion by evacuation (obtaining the live data to seed promoted_reserve),
  // and identify regions that will promote in place. These use the tenuring threshold.
  const size_t consumed_by_advance_promotion = select_aged_regions(old_promo_reserve, heap, collection_set);
  assert(consumed_by_advance_promotion <= maximum_old_evacuation_reserve, "Cannot promote more than available old-gen memory");
  assert(consumed_by_advance_promotion <= old_promo_reserve, "Cannot promote more than was reserved");

  log_info(gc, ergo)("Initial evacuation reserves: young: " PROPERFMT ", promotion: " PROPERFMT ", old: " PROPERFMT,
                     PROPERFMTARGS(young_evacuation_reserve), PROPERFMTARGS(consumed_by_advance_promotion), PROPERFMTARGS(old_evacuation_reserve));

  // If any regions have been selected for promotion in place, this has the effect of decreasing available within mutator
  // and collector partitions, due to padding of remnant memory within each promoted in place region.  This will affect
  // young_evacuation_reserve but not old_evacuation_reserve or consumed_by_advance_promotion.  So recompute.
  young_evacuation_reserve = MIN2(young_evacuation_reserve, young_generation->available_with_reserve());

  // Note that unused old_promo_reserve might not be entirely consumed_by_advance_promotion.  Do not transfer this
  // to old_evacuation_reserve because this memory is likely very fragmented, and we do not want to increase the likelihood
  // of old evacuation failure.
  young_generation->set_evacuation_reserve(young_evacuation_reserve);
  old_generation->set_evacuation_reserve(old_evacuation_reserve);
  old_generation->set_promoted_reserve(consumed_by_advance_promotion);

  // There is no need to expand OLD because all memory used here was set aside at end of previous GC, except in the
  // case of a GLOBAL gc.  During choose_collection_set() of GLOBAL, old will be expanded on demand.
}

// Having chosen the collection set, adjust the budgets for generational mode based on its composition.  Note
// that young_generation->available() now knows about recently discovered immediate garbage.
//
void ShenandoahGenerationalHeuristics::adjust_evacuation_budgets(ShenandoahGenerationalHeap* const heap, ShenandoahCollectionSet* const collection_set) {
  shenandoah_assert_generational();
  // We may find that old_evacuation_reserve is not fully consumed, in which case we may be able to transfer old
  // unaffiliated regions back to young.

  // The role of adjust_evacuation_budgets() is to compute the correct value of regions to transfer to young and to make
  // effective use of this memory, including the remnant memory within these regions that may result from rounding loan to
  // integral number of regions.  Excess memory that is available to be loaned is applied to an allocation supplement,
  // which allows mutators to allocate memory beyond the current capacity of young-gen on the promise that the loan
  // will be repaid as soon as we finish updating references for the recently evacuated collection set.

  ShenandoahYoungGeneration* const young_generation = heap->young_generation();
  const size_t young_bytes_to_evacuate = collection_set->get_live_bytes_in_untenurable_regions();
  const size_t anticipated_bytes_needed_for_young = (size_t) (ShenandoahEvacWaste * double(young_bytes_to_evacuate));
  assert(anticipated_bytes_needed_for_young <= young_generation->available_with_reserve(), "Cannot evacuate more than is available in young");
  young_generation->set_evacuation_reserve(anticipated_bytes_needed_for_young);

  ShenandoahOldGeneration* const old_generation = heap->old_generation();
  const size_t old_bytes_to_evacuate = collection_set->get_live_bytes_in_old_regions();
  const size_t anticipated_bytes_needed_for_old = (size_t) (ShenandoahOldEvacWaste * double(old_bytes_to_evacuate));
  const size_t old_evacuation_reserve = old_generation->get_evacuation_reserve();

  if (anticipated_bytes_needed_for_old > old_evacuation_reserve) {
    // This should only happen due to round-off errors when enforcing ShenandoahOldEvacWaste
    assert(anticipated_bytes_needed_for_old <= (33 * old_evacuation_reserve) / 32,
           "Round-off errors should be less than 3.125%%, committed: %zu, reserved: %zu",
           anticipated_bytes_needed_for_old, old_evacuation_reserve);
    // Leave old_evac_reserve as previously configured, it is already maxed out.
  } else if (anticipated_bytes_needed_for_old < old_evacuation_reserve) {
    // This happens if the old-gen collection consumes less than full budget.
    log_debug(gc, cset)("Shrinking old evac reserve to match anticipated need: " PROPERFMT, PROPERFMTARGS(anticipated_bytes_needed_for_old));
    old_generation->set_evacuation_reserve(anticipated_bytes_needed_for_old);
  }

  old_generation->reset_promoted_expended();

#ifdef ASSERT
  const size_t region_size_bytes = ShenandoahHeapRegion::region_size_bytes();

  size_t old_available = old_generation->available();
  const size_t promoted_reserve = old_generation->get_promoted_reserve();
  const size_t old_consumed = anticipated_bytes_needed_for_old + promoted_reserve;
  if (_generation->is_global() && old_available < old_consumed) {
    // The global heuristic may transfer young regions to the old generation to allow more old evacuations.
    // It will increase the old evacuation reserve when it does this, but old available will be adjusted
    // when the free set is rebuilt (after this method exits).
    old_available = old_consumed;
  }

  assert(old_available >= old_consumed, "Cannot consume (%zu) more than is available (%zu)", old_consumed, old_available);

  const size_t excess_old = old_available - old_consumed;
  const size_t unaffiliated_old_regions = old_generation->free_unaffiliated_regions();
  const size_t unaffiliated_old = unaffiliated_old_regions * region_size_bytes;
  assert(old_available >= unaffiliated_old,
         "Unaffiliated old (%zu is %zu * %zu) is a subset of old available (%zu)",
         unaffiliated_old, unaffiliated_old_regions, region_size_bytes, old_available);
  log_debug(gc, cset)("excess_old is: %zu, unaffiliated_old_regions is: %zu", excess_old, unaffiliated_old_regions);
#endif


  log_info(gc, ergo)("Adjusted evacuation reserves: young: " PROPERFMT ", promotion: " PROPERFMT ", old: " PROPERFMT,
                     PROPERFMTARGS(young_generation->get_evacuation_reserve()),
                     PROPERFMTARGS(old_generation->get_promoted_reserve()),
                     PROPERFMTARGS(old_generation->get_evacuation_reserve()));
}

typedef struct {
  ShenandoahHeapRegion* _region;
  size_t _live_data;
} AgedRegionData;

static int compare_by_aged_live(AgedRegionData a, AgedRegionData b) {
  if (a._live_data < b._live_data)
    return -1;
  else if (a._live_data > b._live_data)
    return 1;
  else return 0;
}

inline void assert_no_in_place_promotions() {
#ifdef ASSERT
  class ShenandoahNoInPlacePromotions : public ShenandoahHeapRegionClosure {
  public:
    void heap_region_do(ShenandoahHeapRegion *r) override {
      assert(r->get_top_before_promote() == nullptr,
             "Region %zu should not be ready for in-place promotion", r->index());
    }
  } cl;
  ShenandoahHeap::heap()->heap_region_iterate(&cl);
#endif
}

// Preselect for inclusion into the collection set regions whose age is at or above tenure age which contain more than
// ShenandoahOldGarbageThreshold amounts of garbage.  We identify these regions by setting the appropriate entry of
// the collection set's preselected regions array to true.  All entries are initialized to false before calling this
// function.
//
// During the subsequent selection of the collection set, we give priority to these promotion set candidates.
// Without this prioritization, we found that the aged regions tend to be ignored because they typically have
// much less garbage and much more live data than the recently allocated "eden" regions.  When aged regions are
// repeatedly excluded from the collection set, the amount of live memory within the young generation tends to
// accumulate and this has the undesirable side effect of causing young-generation collections to require much more
// CPU and wall-clock time.
//
// A second benefit of treating aged regions differently than other regions during collection set selection is
// that this allows us to more accurately budget memory to hold the results of evacuation.  Memory for evacuation
// of aged regions must be reserved in the old generation.  Memory for evacuation of all other regions must be
// reserved in the young generation.
size_t ShenandoahGenerationalHeuristics::select_aged_regions(const size_t old_promotion_reserve, ShenandoahGenerationalHeap* heap, ShenandoahCollectionSet* collection_set) {
  // There should be no regions configured for subsequent in-place-promotions carried over from the previous cycle.
  assert_no_in_place_promotions();

  ShenandoahInPlacePromotionPlanner promoter(heap);
  const size_t num_regions = heap->num_regions();
  size_t candidates = 0;
  ResourceMark rm;
  AgedRegionData* sorted_regions = NEW_RESOURCE_ARRAY(AgedRegionData, num_regions);

  for (size_t i = 0; i < num_regions; i++) {
    ShenandoahHeapRegion* const r = heap->get_region(i);
    if (r->is_empty() || !r->has_live() || !r->is_young() || !r->is_regular()) {
      // skip over regions that aren't regular young with some live data
      continue;
    }

    if (heap->is_tenurable(r)) {
      // All the objects in this region are tenurable. Will we promote the entire region in-place or will
      // we promote individual objects during evacuation?
      if (promoter.is_eligible(r)) {
        // We prefer to promote this region in place because it has a small amount of garbage and a large usage.
        // Note that if this region has been used recently for allocation, it will not be promoted and it will
        // not be selected for promotion by evacuation.
        promoter.prepare(r);
      } else {
        // Record this promotion-eligible candidate region. After sorting and selecting the best candidates below,
        // we may still decide to exclude this promotion-eligible region from the current collection set.
        sorted_regions[candidates]._region = r;
        sorted_regions[candidates]._live_data = r->get_live_data_bytes();
        ++candidates;
      }
    }
  }

  // Adjust the free set to accommodate regions that will be promoted in place
  promoter.update_free_set();

  // Sort in increasing order according to live data bytes.  Note that candidates represents the number of regions
  // that qualify to be promoted by evacuation.
  size_t old_consumed = 0;
  if (candidates > 0) {
    size_t selected_regions = 0;
    size_t selected_live = 0;
    QuickSort::sort<AgedRegionData>(sorted_regions, candidates, compare_by_aged_live);
    for (size_t i = 0; i < candidates; i++) {
      ShenandoahHeapRegion* const region = sorted_regions[i]._region;
      const size_t region_live_data = sorted_regions[i]._live_data;
      const size_t promotion_need = (size_t) (region_live_data * ShenandoahPromoEvacWaste);
      if (old_consumed + promotion_need > old_promotion_reserve) {
        break;
      }

      old_consumed += promotion_need;
      heap->collection_set()->add_region(region);
      selected_regions++;
      selected_live += region_live_data;
    }

    log_debug(gc, ergo)("Preselected %zu regions containing " PROPERFMT " live data,"
                        " consuming: " PROPERFMT " of budgeted: " PROPERFMT,
                        selected_regions, PROPERFMTARGS(selected_live), PROPERFMTARGS(old_consumed), PROPERFMTARGS(old_promotion_reserve));
  }

  const uint tenuring_threshold = heap->age_census()->tenuring_threshold();
  const size_t tenurable_this_cycle = heap->age_census()->get_tenurable_bytes(tenuring_threshold);
  size_t tenurable_next_cycle = heap->age_census()->get_tenurable_bytes(tenuring_threshold - 1);

  // Don't include the bytes we expect to promote in this cycle, in the next
  assert(tenurable_next_cycle >= tenurable_this_cycle,
         "Tenurable next cycle (" PROPERFMT ") should include tenurable this cycle (" PROPERFMT ")",
         PROPERFMTARGS(tenurable_next_cycle), PROPERFMTARGS(tenurable_this_cycle));

  tenurable_next_cycle -= tenurable_this_cycle;

  log_info(gc, ergo)("Tenurable next cycle: " PROPERFMT ", tenurable this cycle: " PROPERFMT ", selected for promotion: " PROPERFMT ,
                     PROPERFMTARGS(tenurable_next_cycle), PROPERFMTARGS(tenurable_this_cycle), PROPERFMTARGS(old_consumed));

  heap->old_generation()->set_promotion_potential(tenurable_next_cycle);

  assert(old_consumed <= old_promotion_reserve, "Consumed more (%zu) than we reserved (%zu)", old_consumed, old_promotion_reserve);

  // old_consumed may exceed tenurable_this_cycle because it has been scaled by ShenandoahPromoEvacWaste.
  old_consumed = MAX2(old_consumed, tenurable_this_cycle);
  return MIN2(old_consumed, old_promotion_reserve);
}

