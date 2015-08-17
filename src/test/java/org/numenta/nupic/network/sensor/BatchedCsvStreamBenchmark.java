/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.BatchedCsvStream;


/**
 * Used to compare the default JDK parallel streaming performance
 * with the custom batched streaming performance. Each call to the 
 * various "timeXXX" methods executes a call to a method which does
 * some body of work on the contents of the stream from a parallel
 * stream. This test proves that given the optimal batched size, the
 * resultant parallel stream will outperform the default JDK parallel
 * strategy. However this depends on the amount of work being done by 
 * the cpu being greater than the time it takes to process the IO. If
 * not, then there is no opportunity for parallelization. If so, then
 * the Fork/Join framework has an opportunity to split up the work into
 * Threads represented by the available cores. Batching makes sure that
 * in this case the cores have sufficient work where there aren't any
 * sitting around with nothing to do, and that the tasks are dividing up
 * well so that there is no contention for tasks.
 * 
 * @author David Ray
 *
 */
public class BatchedCsvStreamBenchmark
{
    static double sink;
    static int jdkTotal;
    static int batchTotal;
    static double jdkTotalTime;
    static double batchTotalTime;

    public static void main(String[] args) throws IOException, URISyntaxException {
        System.out.println("path = " + ResourceLocator.path("rec-center-hourly.csv"));
        final Path inputPath = Paths.get(ResourceLocator.uri("rec-center-hourly.csv"));

        for (int i = 0; i < 3; i++) {
            System.out.println("\n======================  TEST: " + i + "  ==========================");

            System.out.println("JDK Stream Start");
            timeJDKParallelStream(Files.lines(inputPath));
        }
        for (int i = 0; i < 3; i++) {
            System.out.println("\n======================  TEST: " + i + "  ==========================");

            System.out.println("Batched Stream Start");
            timeBatchedParallelStream(BatchedCsvStream.batch(Files.lines(inputPath), 20, true, 3));
        }

        System.out.println("JDK Total Computations: " + jdkTotal + ", Total Time: " + jdkTotalTime/SECONDS.toNanos(1) + " s");
        System.out.println("Batch Total Computations: " + batchTotal + ", Total Time: " + batchTotalTime/SECONDS.toNanos(1) + " s");
    }

    private static void timeJDKParallelStream(Stream<String> input) throws IOException {
        final long start = System.nanoTime();
        try (Stream<String> lines = input) {
            final long totalTime = lines.parallel().mapToLong(BatchedCsvStreamBenchmark::processLine).sum();
            final double cpuTime = totalTime, realTime = System.nanoTime() - start;
            final int cores = Runtime.getRuntime().availableProcessors();
            System.out.println("          Cores: " + cores);
            System.out.format("       CPU time: %.2f s\n", cpuTime/SECONDS.toNanos(1));
            System.out.format("      Real time: %.2f s\n", realTime/SECONDS.toNanos(1));
            System.out.format("CPU utilization: %.2f%%\n\n", 100.0*cpuTime/realTime/cores);
            jdkTotalTime += realTime;
        }
    }

    private static void timeBatchedParallelStream(Stream<String[]> input) throws IOException {
        final long start = System.nanoTime();
        try (Stream<String[]> lines = input) {
            final long totalTime = lines.mapToLong(BatchedCsvStreamBenchmark::processLine2).sum();
            final double cpuTime = totalTime, realTime = System.nanoTime() - start;
            final int cores = Runtime.getRuntime().availableProcessors();
            System.out.println("          Cores: " + cores);
            System.out.format("       CPU time: %.2f s\n", cpuTime/SECONDS.toNanos(1));
            System.out.format("      Real time: %.2f s\n", realTime/SECONDS.toNanos(1));
            System.out.format("CPU utilization: %.2f%%\n\n", 100.0*cpuTime/realTime/cores);
            batchTotalTime += realTime;
        }
    }

    private static long processLine(String line) {
        final long localStart = System.nanoTime();
        double d = 0;
        for (int i = 0; i < line.length(); i++) {
            for (int j = 0; j < line.length(); j++) {
                d += new BigDecimal(Math.pow(line.charAt(j), line.charAt(j)/32.0)).doubleValue();
                jdkTotal++;
            }
        }

        sink += d;
        return System.nanoTime() - localStart;
    } 

    private static long processLine2(String[] l) {
        StringBuilder sb = new StringBuilder();
        Stream.of(l).forEach(x -> sb.append(x).append(" "));
        String line = sb.toString();

        //Everything here is the same as processLine(String line) above.
        final long localStart = System.nanoTime();
        double d = 0;
        for (int i = 0; i < line.length(); i++) {
            for (int j = 0; j < line.length(); j++) {
                d += new BigDecimal(Math.pow(line.charAt(j), line.charAt(j)/32.0)).doubleValue();
                batchTotal++;
            }
        }

        sink += d;
        return System.nanoTime() - localStart;
    }

}
