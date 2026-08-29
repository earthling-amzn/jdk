/*
 * Copyright (c) 2016, 2021, Red Hat, Inc. All rights reserved.
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


#include "gc/shenandoah/shenandoahHeap.inline.hpp"
#include "gc/shenandoah/shenandoahTaskqueue.inline.hpp"

void ShenandoahObjToScanQueueSet::clear() {
  for (uint index = 0, num_queues = size(); index < num_queues; ++index) {
    ShenandoahObjToScanQueue* q = queue(index);
    assert(q != nullptr, "Sanity");
    q->clear();
  }
}

bool ShenandoahObjToScanQueueSet::is_empty() {
  for (uint index = 0, num_queues = size(); index < num_queues; ++index) {
    ShenandoahObjToScanQueue* q = queue(index);
    assert(q != nullptr, "Sanity");
    if (!q->is_empty()) {
      return false;
    }
  }
  return true;
}

void ShenandoahTerminatorTerminator::retire() {
  _retired = true;
}

bool ShenandoahTerminatorTerminator::can_work() const {
  // Can work if the thread cannot be cancelled (i.e., STW worker) or the thread has not been retired,
  // and it is not being held in reserve.
  return !_cancellable || (!_retired && WorkerThread::worker_id() < _heap->control_thread()->concurrent_worker_count());
}

// Return true means: withdraw offer to terminate, go back and look for work.
bool ShenandoahTerminatorTerminator::should_exit_termination(size_t tasks) {
  if (_heap->cancelled_gc()) {
    // If GC is cancelled, every worker will see the cancellation in the work loop and exit the work loop.
    return true;
  }

  // Else, true if there are tasks _and_ this worker is allowed to work. In this way we can keep
  // reserved or retired workers idle.
  return tasks > 0 && can_work();
}
