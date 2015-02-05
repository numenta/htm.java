package sample;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.ThreadLocalRandom;

public class SampleBenchmark {

    @State(Scope.Thread)
    public static class Point {

        private static final double MAX_VALUE = 10_000;
        public final double a, b;

        public Point() {
            a = ThreadLocalRandom.current().nextDouble(MAX_VALUE);
            b = ThreadLocalRandom.current().nextDouble(MAX_VALUE);
        }
    }

    @Benchmark
     public double measureHypot_baseline(Point p) {
        return Math.sqrt(p.a * p.a + p.b * p.b);
    }

//    @Benchmark
//    public double measureHypot_direct(Point p) {
//        return Math.hypot(p.a, p.b);
//    }
//
//    @Benchmark
//    public double measureHypot_wrapped(Point p) {
//        return SampleClass.foo(p.a, p.b);
//    }
//
//    @Benchmark
//    public double measureLog_direct(Point p) {
//        return Math.pow(p.a, p.b);
//    }
//
//    @Benchmark
//    public double measureLog_wrapped(Point p) {
//        return SampleClass.bar(p.a, p.b);
//    }

}
