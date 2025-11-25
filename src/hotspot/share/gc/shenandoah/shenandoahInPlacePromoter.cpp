/*
 * Copyright Amazon.com Inc. or its affiliates. All Rights Reserved.
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

#include "gc/shared/plab.hpp"
#include "gc/shenandoah/shenandoahFreeSet.hpp"
#include "gc/shenandoah/shenandoahGenerationalHeap.hpp"
#include "gc/shenandoah/shenandoahHeapRegion.hpp"
#include "gc/shenandoah/shenandoahInPlacePromoter.hpp"
#include "gc/shenandoah/shenandoahOldGeneration.hpp"
#include "gc/shenandoah/shenandoahMarkingContext.hpp"

ShenandoahInPlacePromoter::RegionPromotions::RegionPromotions(ShenandoahFreeSet* free_set)
  : _low_idx(free_set->max_regions())
  , _high_idx(-1)
  , _regions(0)
  , _bytes(0)
  , _free_set(free_set)
{
}

void ShenandoahInPlacePromoter::RegionPromotions::increment(idx_t region_index, size_t remnant_bytes) {
  if (region_index < _low_idx) {
    _low_idx = region_index;
  }
  if (region_index > _high_idx) {
    _high_idx = region_index;
  }
  _regions++;
  _bytes += remnant_bytes;
}

void ShenandoahInPlacePromoter::RegionPromotions::update_free_set(ShenandoahFreeSetPartitionId partition_id) const {
  if (_regions > 0) {
    _free_set->shrink_interval_if_range_modifies_either_boundary(partition_id, _low_idx, _high_idx, _regions);
  }
}

ShenandoahInPlacePromoter::ShenandoahInPlacePromoter(const ShenandoahGenerationalHeap* heap)
  : _old_garbage_threshold(ShenandoahHeapRegion::region_size_bytes() * ShenandoahOldGarbageThreshold / 100)
  , _pip_used_threshold(ShenandoahHeapRegion::region_size_bytes() * ShenandoahGenerationalMinPIPUsage / 100)
  , _heap(heap)
  , _free_set(_heap->free_set())
  , _marking_context(_heap->marking_context())
  , _mutator_regions(_free_set)
  , _collector_regions(_free_set)
  , _pip_padding_bytes(0)
{
}

bool ShenandoahInPlacePromoter::is_eligible(const ShenandoahHeapRegion* region) const {
  return region->garbage() < _old_garbage_threshold && region->used() > _pip_used_threshold;
}

void ShenandoahInPlacePromoter::prepare(ShenandoahHeapRegion* r) {
  HeapWord* tams = _marking_context->top_at_mark_start(r);
  HeapWord* original_top = r->top();

  if (_heap->is_concurrent_mark_in_progress() || tams != original_top) {
    // We do not promote this region (either in place or by copy) because it has received new allocations.
    // During evacuation, we exclude from promotion regions for which age > tenure threshold, garbage < garbage-threshold,
    // used > pip_used_threshold, and get_top_before_promote() != tams.
    //  TODO: Such a region should have had its age reset to zero when it was used for allocation?
    return;
  }

  // No allocations from this region have been made during concurrent mark. It meets all the criteria
  // for in-place-promotion. Though we only need the value of top when we fill the end of the region,
  // we use this field to indicate that this region should be promoted in place during the evacuation
  // phase.
  r->save_top_before_promote();
  size_t remnant_bytes = r->free();
  size_t remnant_words = remnant_bytes / HeapWordSize;
  assert(ShenandoahHeap::min_fill_size() <= PLAB::min_size(), "Implementation makes invalid assumptions");
  if (remnant_words >= ShenandoahHeap::min_fill_size()) {
    ShenandoahHeap::fill_with_object(original_top, remnant_words);
    // Fill the remnant memory within this region to assure no allocations prior to promote in place.  Otherwise,
    // newly allocated objects will not be parsable when promote in place tries to register them.  Furthermore, any
    // new allocations would not necessarily be eligible for promotion.  This addresses both issues.
    r->set_top(r->end());
    // The region r is either in the Mutator or Collector partition if remnant_words > heap()->plab_min_size.
    // Otherwise, the region is in the NotFree partition.
    const idx_t i = r->index();
    ShenandoahFreeSetPartitionId p = _free_set->membership(i);
    if (p == ShenandoahFreeSetPartitionId::Mutator) {
      _mutator_regions.increment(i, remnant_bytes);
    } else if (p == ShenandoahFreeSetPartitionId::Collector) {
      _collector_regions.increment(i, remnant_bytes);
    } else {
      assert((p == ShenandoahFreeSetPartitionId::NotFree) && (remnant_words < _heap->plab_min_size()),
             "Should be NotFree if not in Collector or Mutator partitions");
      // In this case, the memory is already counted as used and the region has already been retired.  There is
      // no need for further adjustments to used.  Further, the remnant memory for this region will not be
      // unallocated or made available to OldCollector after pip.
      remnant_bytes = 0;
    }

    _pip_padding_bytes += remnant_bytes;
    _free_set->prepare_to_promote_in_place(i, remnant_bytes);
  } else {
    // Since the remnant is so small that this region has already been retired, we don't have to worry about any
    // accidental allocations occurring within this region before the region is promoted in place.

    // This region was already not in the Collector or Mutator set, so no need to remove it.
    assert(_free_set->membership(r->index()) == ShenandoahFreeSetPartitionId::NotFree, "sanity");
  }
}

void ShenandoahInPlacePromoter::update_free_set() const {
  _heap->old_generation()->set_pad_for_promote_in_place(_pip_padding_bytes);

  if (_mutator_regions._regions + _collector_regions._regions > 0) {
    _free_set->account_for_pip_regions(_mutator_regions._regions, _mutator_regions._bytes,
                         _collector_regions._regions, _collector_regions._bytes);
  }

  // Retire any regions that have been selected for promote in place
  _mutator_regions.update_free_set(ShenandoahFreeSetPartitionId::Mutator);
  _collector_regions.update_free_set(ShenandoahFreeSetPartitionId::Collector);
}
