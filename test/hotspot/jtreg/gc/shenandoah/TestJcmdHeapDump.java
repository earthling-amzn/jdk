/*
 * Copyright (c) 2021, Red Hat, Inc. All rights reserved.
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
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:+XenandoahDegeneratedGC
 *      TestJcmdHeapDump
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=passive
 *      -XX:-XenandoahDegeneratedGC
 *      TestJcmdHeapDump
 */

/*
 * @test id=aggressive
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      TestJcmdHeapDump
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      TestJcmdHeapDump
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=aggressive
 *      TestJcmdHeapDump
 */

/*
 * @test id=adaptive
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=adaptive
 *      -Dtarget=10000
 *      TestJcmdHeapDump
 */

/*
 * @test id=static
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=static
 *      TestJcmdHeapDump
 */

/*
 * @test id=compact
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCHeuristics=compact
 *     TestJcmdHeapDump
 */

/*
 * @test id=iu-aggressive
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 * @requires vm.gc.Xenandoah
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahOOMDuringEvacALot
 *      TestJcmdHeapDump
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      -XX:+XenandoahAllocFailureALot
 *      TestJcmdHeapDump
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu -XX:XenandoahGCHeuristics=aggressive
 *      TestJcmdHeapDump
 */

/*
 * @test id=iu
 * @requires vm.gc.Xenandoah
 * @library /test/lib
 * @modules jdk.attach/com.sun.tools.attach
 *
 * @run main/othervm/timeout=480 -Xmx16m -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseXenandoahGC -XX:XenandoahGCMode=iu
 *      TestJcmdHeapDump
 */

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.File;

public class TestJcmdHeapDump {
    public static void main(String[] args) {
        long pid = ProcessHandle.current().pid();
        JDKToolLauncher jcmd = JDKToolLauncher.createUsingTestJDK("jcmd");
        jcmd.addToolArg(String.valueOf(pid));
        jcmd.addToolArg("GC.heap_dump");
        String dumpFileName = "myheapdump" + String.valueOf(pid);
        jcmd.addToolArg(dumpFileName);

        try {
            ProcessBuilder pb = new ProcessBuilder(jcmd.getCommand());
            Process jcmdProc = pb.start();

            OutputAnalyzer output = new OutputAnalyzer(jcmdProc);
            jcmdProc.waitFor();
            output.shouldHaveExitValue(0);
        } catch (Exception e) {
            throw new RuntimeException("Test failed: " + e);
        }

        File f = new File(dumpFileName);
        if (f.exists()) {
            f.delete();
        } else {
            throw new RuntimeException("Dump file not created");
        }
    }
}
