package com.hienhoang.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Hien Hoang (hienhoang2702@gmail.com)
 */
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
public class ThreadPoolExecutorBenchmark {
    BlockingQueue<Runnable> queue;
    ThreadPoolExecutor threadPoolExecutor;
//    @Param({"1", "10", "100"})
    @Param({"1000"})
    int numConsumers;
//    @Param({"16", "24"})
    @Param({"12"})
    int poolSizePower2;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(ThreadPoolExecutorBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(options).run();
    }

    @Setup
    public void setup() {
//        queue = new ArrayBlockingQueue<>(1 << poolSizePower2);
        queue = new LinkedBlockingQueue<>(1 << poolSizePower2);
        threadPoolExecutor = new ThreadPoolExecutor(128,
                numConsumers, 0, TimeUnit.MILLISECONDS, queue);
    }

    @Benchmark
    public void publish(Blackhole blackhole) {
        try { threadPoolExecutor.submit(() -> { });
        } catch (Exception e) {}
    }

    @TearDown
    public void tearDown() {
        queue.clear(); threadPoolExecutor.shutdown();
    }

}
