/*
 * Copyright (c) 2005, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */
package com.hienhoang.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(5)
public class FalseSharingBenchmark {

    /*
     * One of the unusual thing that can bite you back is false sharing.
     * If two threads access (and possibly modify) the adjacent values
     * in memory, chances are, they are modifying the values on the same
     * cache line. This can yield significant (artificial) slowdowns.
     *
     * JMH helps you to alleviate this: @States are automatically padded.
     * This padding does not extend to the State internals though,
     * as we will see in this example. You have to take care of this on
     * your own.
     */

    /*
     * Suppose we have two threads:
     *   a) innocuous reader which blindly reads its own field
     *   b) furious writer which updates its own field
     */

    /*
     * BASELINE EXPERIMENT:
     * Because of the false sharing, both reader and writer will experience
     * penalties.
     */

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + FalseSharingBenchmark.class.getSimpleName() + ".*")
                .threads(Runtime.getRuntime().availableProcessors())
                .build();

        new Runner(opt).run();
    }

    @Benchmark
    @Group("baseline")
    public int reader(StateBaseline s) {
        return s.readOnly;
    }

    @Benchmark
    @Group("baseline")
    public void writer(StateBaseline s) {
        s.writeOnly++;
    }

    /*
     * APPROACH 1: PADDING
     *
     * We can try to alleviate some of the effects with padding.
     * This is not versatile because JVMs can freely rearrange the
     * field order.
     */

    @Benchmark
    @Group("padded")
    public int reader(StatePadded s) {
        return (int) s.readOnly;
    }

    @Benchmark
    @Group("padded")
    public void writer(StatePadded s) {
        s.writeOnly++;
    }

    @Benchmark
    @Group("hierarchy")
    public int reader(StateHierarchy s) {
        return s.readOnly;
    }

    /*
     * APPROACH 2: CLASS HIERARCHY TRICK
     *
     * We can alleviate false sharing with this convoluted hierarchy trick,
     * using the fact that superclass fields are usually laid out first.
     * In this construction, the protected field will be squashed between
     * paddings.
     */

    @Benchmark
    @Group("hierarchy")
    public void writer(StateHierarchy s) {
        s.writeOnly++;
    }

//    @Benchmark
//    @Group("sparse")
//    public int reader(StateArray s) {
//        return s.arr[0];
//    }

//    @Benchmark
//    @Group("sparse")
//    public void writer(StateArray s) {
//        s.arr[64]++;
//    }

//    @Benchmark
//    @Group("contended")
//    public int reader(StateContended s) {
//        return s.readOnly;
//    }
//
//    @Benchmark
//    @Group("contended")
//    public void writer(StateContended s) {
//        s.writeOnly++;
//    }

    @State(Scope.Group)
    public static class StateBaseline {
        int readOnly;
        int writeOnly;
    }

    @State(Scope.Group)
    public static class StatePadded {
        long readOnly;
        long p01, p02, p03, p04, p05, p06, p07, p08;
        long p11, p12, p13, p14, p15, p16, p17, p18;
        long writeOnly;
        long q01, q02, q03, q04, q05, q06, q07, q08;
        long q11, q12, q13, q14, q15, q16, q17, q18;
    }

    /*
     * APPROACH 3: ARRAY TRICK
     *
     * This trick relies on the contiguous allocation of an array.
     * Instead of placing the fields in the class, we mangle them
     * into the array at very sparse offsets.
     */

    public static class StateHierarchy_1 {
        int readOnly;
    }

    public static class StateHierarchy_2 extends StateHierarchy_1 {
        long p01, p02, p03, p04, p05, p06, p07, p08;
        long p11, p12, p13, p14, p15, p16, p17, p18;
        long p_01, p_02, p_03, p_04, p_05, p_06, p_07, p_08;
        long p_11, p_12, p_13, p_14, p_15, p_16, p_17, p_18;
    }

    public static class StateHierarchy_3 extends StateHierarchy_2 {
        int writeOnly;
    }

    /*
     * APPROACH 4:
     *
     * @Contended (since JDK 8):
     *  Uncomment the annotation if building with JDK 8.
     *  Remember to flip -XX:-RestrictContended to enable.
     */

    public static class StateHierarchy_4 extends StateHierarchy_3 {
        long q01, q02, q03, q04, q05, q06, q07, q08;
        long q11, q12, q13, q14, q15, q16, q17, q18;
        long q_01, q_02, q_03, q_04, q_05, q_06, q_07, q_08;
        long q_11, q_12, q_13, q_14, q_15, q_16, q_17, q_18;
    }

    @State(Scope.Group)
    public static class StateHierarchy extends StateHierarchy_4 {
    }

    @State(Scope.Group)
    public static class StateArray {
        int[] arr = new int[128];
    }

    /*
     * ============================== HOW TO RUN THIS TEST: ====================================
     *
     * Note the slowdowns.
     *
     * You can run this test:
     *
     * a) Via the command line:
     *    $ mvn clean install
     *    $ java -jar target/benchmarks.jar ".*JMHSample_22.*" -t $CPU
     *
     * b) Via the Java API:
     */

    @State(Scope.Group)
    public static class StateContended {
        int readOnly;

//        @sun.misc.Contended
        int writeOnly;
    }

}
