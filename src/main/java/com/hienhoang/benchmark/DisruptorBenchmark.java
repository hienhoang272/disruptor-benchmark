package com.hienhoang.benchmark;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.BusySpinWaitStrategy;
import com.lmax.disruptor.LiteBlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * @author Hien Hoang (hienhoang2702@gmail.com)
 */
@BenchmarkMode({Mode.AverageTime})
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Fork(1)
public class DisruptorBenchmark {
    @Param({"16", "24"})
    int bufferSizePower;
    @Param({"BLOCKING_WAIT_STRATEGY",
            "BUSY_SPIN_WAIT_STRATEGY",
            "LITE_BLOCKING_WAIT_STRATEGY",
            "SLEEPING_WAIT_STRATEGY",
            "YIELDING_WAIT_STRATEGY"})
    WaitStrategyEnum waitStrategyEnum;
    //    @Param({"SINGLE", "MULTI"})
    ProducerType producerType = ProducerType.SINGLE;
    @Param({"1", "10", "100"})
    int numConsumers;
    private Disruptor<Event> disruptor;

    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(DisruptorBenchmark.class.getSimpleName())
                .forks(1)
                .build();

        new Runner(options).run();
    }

    @Setup
    @SuppressWarnings("unchecked")
    public void init(Blackhole blackhole) {
        int bufferSize = 1 << (bufferSizePower);
        disruptor = new Disruptor<>(Event::new, bufferSize,
                DaemonThreadFactory.INSTANCE, ProducerType.SINGLE,
                waitStrategyEnum.waitStrategy);
        WorkHandler[] workHandlers = new WorkHandler[numConsumers];
        for (int i = 0; i < workHandlers.length; i++) {
            workHandlers[i] = event -> {
            };
        }
        disruptor.handleEventsWithWorkerPool(workHandlers);
        disruptor.start();
    }

    @Benchmark
    public void publish() throws InterruptedException {
        final RingBuffer<Event> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        Event event = ringBuffer.get(sequence);
        event.value = 234872374L;
        ringBuffer.publish(sequence);
    }

    public enum WaitStrategyEnum {
        BLOCKING_WAIT_STRATEGY(new BlockingWaitStrategy()),
        BUSY_SPIN_WAIT_STRATEGY(new BusySpinWaitStrategy()),
        LITE_BLOCKING_WAIT_STRATEGY(new LiteBlockingWaitStrategy()),
        SLEEPING_WAIT_STRATEGY(new SleepingWaitStrategy()),
        YIELDING_WAIT_STRATEGY(new YieldingWaitStrategy());

        public final WaitStrategy waitStrategy;

        WaitStrategyEnum(WaitStrategy waitStrategy) {
            this.waitStrategy = waitStrategy;
        }
    }

    static class Event {
        public long value;
    }

}
