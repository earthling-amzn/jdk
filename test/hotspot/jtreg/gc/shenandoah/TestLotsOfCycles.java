/*
 * Copyright (c) 2017, 2018, Red Hat, Inc. All rights reserved.
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

/*
 * @test id=passive
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+XenandoahDegeneratedGC
 *      -Dtarget=10000
 *      TestLotsOfCycles
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:-XenandoahDegeneratedGC
 *      -Dtarget=10000
 *      TestLotsOfCycles
 */

/*
 * @test id=aggressive
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      -Dtarget=1000
 *      TestLotsOfCycles
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      -Dtarget=1000
 *      TestLotsOfCycles
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -Dtarget=1000
 *      TestLotsOfCycles
 */

/*
 * @test id=adaptive
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      -Dtarget=10000
 *      TestLotsOfCycles
 */

/*
 * @test id=static
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=static
 *      -Dtarget=10000
 *      TestLotsOfCycles
 */

/*
 * @test id=compact
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=compact
 *      -Dtarget=1000
 *     TestLotsOfCycles
 */

/*
 * @test id=iu-aggressive
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      -Dtarget=1000
 *      TestLotsOfCycles
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      -Dtarget=1000
 *      TestLotsOfCycles
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -Dtarget=1000
 *      TestLotsOfCycles
 */

/*
 * @test id=iu
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      -Dtarget=10000
 *      TestLotsOfCycles
 */

public class TestLotsOfCycles {

    static final long TARGET_MB = Long.getLong("target", 10_000); // 10 Gb allocation, around 1K cycles to handle
    static final long STRIDE = 100_000;

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        long count = TARGET_MB * 1024 * 1024 / 16;
        for (long c = 0; c < count; c += STRIDE) {
            for (long s = 0; s < STRIDE; s++) {
                sink = new Object();
            }
            Thread.sleep(1);
        }
    }

}
