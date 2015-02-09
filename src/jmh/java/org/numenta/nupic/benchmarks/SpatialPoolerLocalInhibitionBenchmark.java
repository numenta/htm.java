package org.numenta.nupic.benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;

public class SpatialPoolerLocalInhibitionBenchmark extends AbstractAlgorithmBenchmark {
		
	private int[][] input;
	
	@Setup
	public void init() {
		super.init();
		
		input = new int[7][8];
    	for(int i = 0;i < 7;i++) {
    		input[i] = encoder.encode((double) i + 1);
    	}
	}
	
	@Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MILLISECONDS)
    public int[] measureAvgCompute_7_Times(Blackhole bh) throws InterruptedException {
		for(int i = 0;i < 7;i++) {
			pooler.compute(memory, input[i], SDR, true, false);
		}
        
        return SDR;
    }

}
