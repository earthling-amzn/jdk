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
 * @test id=default
 * @key randomness
 * @requires vm.gc.Xenandoah
 * @library /test/lib
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:+XenandoahVerify
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=50
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=90
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=99
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=100
 *                   TestHumongousThreshold
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:+XenandoahVerify
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=50
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=90
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=99
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=100
 *                   TestHumongousThreshold
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:XenandoahHumongousThreshold=90 -XX:XenandoahGCHeuristics=aggressive
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:XenandoahHumongousThreshold=90 -XX:XenandoahGCHeuristics=aggressive
 *                   TestHumongousThreshold
 */

/*
 * @test id=16b
 * @key randomness
 * @requires vm.gc.Xenandoah
 * @requires vm.bits == "64"
 * @library /test/lib
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=50
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=90
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=99
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=100
 *                   TestHumongousThreshold
 *
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=50
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=90
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=99
 *                   TestHumongousThreshold
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions -XX:+UseXenandoahGC -Xmx1g
 *                   -XX:-UseTLAB -XX:ObjectAlignmentInBytes=16 -XX:+XenandoahVerify -XX:XenandoahHumongousThreshold=100
 *                   TestHumongousThreshold
 */

import java.util.Random;
import jdk.test.lib.Utils;

public class TestHumongousThreshold {

    static final long TARGET_MB = Long.getLong("target", 20_000); // 20 Gb allocation

    static volatile Object sink;

    public static void main(String[] args) throws Exception {
        final int min = 0;
        final int max = 384 * 1024;
        long count = TARGET_MB * 1024 * 1024 / (16 + 4 * (min + (max - min) / 2));

        Random r = Utils.getRandomInstance();
        for (long c = 0; c < count; c++) {
            sink = new int[min + r.nextInt(max - min)];
        }
    }

}
