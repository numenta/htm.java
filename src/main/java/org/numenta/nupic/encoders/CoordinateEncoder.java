package org.numenta.nupic.encoders;

import java.util.List;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.SparseMatrix;
import org.numenta.nupic.util.Tuple;

public class CoordinateEncoder extends Encoder {

	/**
	 * Package private to encourage construction using the Builder Pattern
	 * but still allow inheritance.
	 */
	CoordinateEncoder() {}
	
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
	 * Returns the order for a coordinate.
	 * 
	 * @param coordinate	coordinate array
	 * 
	 * @return	A value in the interval [0, 1), representing the
     *          order of the coordinate
	 */
	public static double orderForCoordinate(int[] coordinate) {
		int seed = ArrayUtils.fromCoordinate(coordinate);
		return new MersenneTwister(seed).nextDouble();
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
	public int getWidth() {
		return n;
	}

	@Override
	public boolean isDelta() {
		return false;
	}
	
	@Override
	public int[] encodeIntoArray(int[] inputData, int[] output) {
		// TODO Auto-generated method stub
		return null;
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
	public List<Tuple> getDescription() {
		// TODO Auto-generated method stub
		return null;
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
