/*
 * Copyright (c) 2016, 2018, Red Hat, Inc. All rights reserved.
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

package gc.stress.gcbasher;

import java.io.IOException;

/*
 * @test id=passive
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Xenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+XenandoahVerify -XX:+XenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+XenandoahVerify -XX:-XenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=aggressive
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Xenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=adaptive
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Xenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      -XX:+XenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=compact
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=compact
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=iu-aggressive
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Xenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=iu
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient
 * @summary Stress the Xenandoah GC by trying to make old objects more likely to be garbage than young objects.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      -XX:+XenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=passive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahVerify -XX:+XenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahVerify -XX:-XenandoahDegeneratedGC
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=aggressive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=adaptive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=compact-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=compact
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=iu-aggressive-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahOOMDuringEvacALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahAllocFailureALot
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */

/*
 * @test id=iu-deopt-nmethod
 * @key stress
 * @library /
 * @requires vm.gc.Xenandoah
 * @requires vm.flavor == "server" & !vm.emulatedClient & vm.opt.ClassUnloading != false
 * @summary Stress Xenandoah GC with nmethod barrier forced deoptimization enabled.
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      -XX:+XenandoahVerify
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 *
 * @run main/othervm/timeout=200 -Xlog:gc*=info,nmethod+barrier=trace -Xmx1g -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      -XX:+DeoptimizeNMethodBarriersALot -XX:-Inline
 *      gc.stress.gcbasher.TestGCBasherWithXenandoah 120000
 */


public class TestGCBasherWithXenandoah {
    public static void main(String[] args) throws IOException {
        TestGCBasher.main(args);
    }
}
