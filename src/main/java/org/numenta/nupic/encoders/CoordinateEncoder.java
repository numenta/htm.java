package org.numenta.nupic.encoders;

import gnu.trove.set.TDoubleSet;
import gnu.trove.set.hash.TDoubleHashSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

public class CoordinateEncoder extends Encoder implements CoordinateOrder {
	private static Random random = new MersenneTwister();
	
	/**
	 * Package private to encourage construction using the Builder Pattern
	 * but still allow inheritance.
	 */
	CoordinateEncoder() {}
	
	@Override
	public int getWidth() {
		return n;
	}

	@Override
	public boolean isDelta() {
		return false;
	}
	
	@Override
	public List<Tuple> getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns a builder for building ScalarEncoders. 
	 * This builder may be reused to produce multiple builders
	 * 
	 * @return a {@code CoordinateEncoder.Builder}
	 */
	public static Encoder.Builder<CoordinateEncoder.Builder, CoordinateEncoder> builder() {
		return new CoordinateEncoder.Builder();
	}
	
	/**
	 * Returns coordinates around given coordinate, within given radius.
     * Includes given coordinate.
     * 
	 * @param coordinate	Coordinate whose neighbors to find
	 * @param radius		Radius around `coordinate`
	 * @return
	 */
	public List<int[]> neighbors(int[] coordinate, double radius) {
		int[][] ranges = new int[coordinate.length][];
		for(int i = 0;i < coordinate.length;i++) {
			ranges[i] = ArrayUtils.range(coordinate[i] - (int)radius, coordinate[i] + (int)radius + 1);
		}
		
		List<int[]> retVal = new ArrayList<int[]>();
		int len = ranges.length == 1 ? 1 : ranges[0].length;
		for(int k = 0;k < ranges[0].length;k++) {
			for(int j = 0;j < len;j++) {
				int[] entry = new int[ranges.length];
				entry[0] = ranges[0][k];
				for(int i = 1;i < ranges.length;i++) {
					entry[i] = ranges[i][j];
				}
				retVal.add(entry);
			}
		}
		return retVal;
	}
	
	/**
	 * Returns the top W coordinates by order.
	 * 
	 * @param co			Implementation of {@link CoordinateOrder}
	 * @param coordinates	A 2D array, where each element
                            is a coordinate
	 * @param w				(int) Number of top coordinates to return
	 * @return
	 */
	public int[][] topWCoordinates(CoordinateOrder co, int[][] coordinates, int w) {
		double[] orders = new double[coordinates.length];
		for(int i = 0;i < coordinates.length;i++) {
			orders[i] = co.orderForCoordinate(coordinates[i]);
		}
		
		System.out.println(Arrays.toString(orders));
		final TDoubleSet end = new TDoubleHashSet(
			ArrayUtils.sub(orders, ArrayUtils.range(orders.length - w, orders.length)));
		
		Arrays.sort(orders);
		
		int[] indices = ArrayUtils.where(orders, new Condition.Adapter<Double>() {
			public boolean eval(double d) { return end.contains(d); }
		});
		
		return ArrayUtils.sub(coordinates, indices);
	}
	
	/**
	 * Returns the order for a coordinate.
	 * 
	 * @param coordinate	coordinate array
	 * 
	 * @return	A value in the interval [0, 1), representing the
     *          order of the coordinate
	 */
	
	public double orderForCoordinate(int[] coordinate) {
		int seed = ArrayUtils.fromCoordinate(coordinate);
		random.setSeed(seed);
		return random.nextDouble();
	}
	
	/**
	 * Returns the order for a coordinate.
	 * 
	 * @param coordinate	coordinate array
	 * @param n				the number of available bits in the SDR
	 * 
	 * @return	The index to a bit in the SDR
	 */
	public static int bitForCoordinate(int[] coordinate, int n) {
		int seed = ArrayUtils.fromCoordinate(coordinate);
		return new MersenneTwister(seed).nextInt(n);
	}
	
	@Override
	public int[] encodeIntoArray(int[] inputData, int[] output) {
		List<int[]> neighs = neighbors(inputData, radius);
		int[][] neighbors = new int[neighs.size()][];
		for(int i = 0;i < neighs.size();i++) neighbors[i] = neighs.get(i);
		
		int[][] winners = topWCoordinates(this, neighbors, w);
		
		for(int i = 0;i < winners.length;i++) {
			int bit = bitForCoordinate(winners[i], n);
			output[bit] = 1;
		}
		return output;
	}

	@Override
	public int[] encodeIntoArray(double inputData, int[] output) {
		throw new UnsupportedOperationException("Not suported.");
	}

	@Override
	public int[] encodeIntoArray(String inputData, int[] output) {
		throw new UnsupportedOperationException("Not suported.");
	}

	@Override
	public void setLearning(boolean learningEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> List<T> getBucketValues(Class<T> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Returns a {@link EncoderBuilder} for constructing {@link CoordinateEncoder}s
	 * 
	 * The base class architecture is put together in such a way where boilerplate
	 * initialization can be kept to a minimum for implementing subclasses. 
	 * Hopefully! :-)
	 * 
	 * @see ScalarEncoder.Builder#setStuff(int)
	 */
	public static class Builder extends Encoder.Builder<CoordinateEncoder.Builder, CoordinateEncoder> {
		private Builder() {}

		@Override
		public CoordinateEncoder build() {
			//Must be instantiated so that super class can initialize 
			//boilerplate variables.
			encoder = new CoordinateEncoder();
			
			//Call super class here
			super.build();
			
			////////////////////////////////////////////////////////
			//  Implementing classes would do setting of specific //
			//  vars here together with any sanity checking       //
			////////////////////////////////////////////////////////
			
			if(w <= 0 || w % 2 == 0) {
				throw new IllegalArgumentException("w must be odd, and must be a positive integer");
			}
			
			if(n <= 6 * w) {
				throw new IllegalArgumentException(
					"n must be an int strictly greater than 6*w. For " +
                       "good results we recommend n be strictly greater than 11*w");
			}
			
			if(name == null || name.equals("None")) {
				name = new StringBuilder("[").append(n).append(":").append(w).append("]").toString();
			}
			
			return (CoordinateEncoder)encoder;
		}
	}
}
