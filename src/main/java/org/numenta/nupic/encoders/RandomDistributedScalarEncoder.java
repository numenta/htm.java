/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.encoders;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

/**
 * <p>
 * A scalar encoder encodes a numeric (floating point) value into an array of
 * bits.
 * </p>
 * <p>
 * This class maps a scalar value into a random distributed representation that
 * is suitable as scalar input into the spatial pooler. The encoding scheme is
 * designed to replace a simple {@link ScalarEncoder}. It preserves the
 * important properties around overlapping representations. Unlike
 * {@link ScalarEncoder} the min and max range can be dynamically increased
 * without any negative effects. The only required parameter is resolution,
 * which determines the resolution of input values.
 * </p>
 * Scalar values are mapped to a bucket. The class maintains a random
 * distributed encoding for each bucket. The following properties are maintained
 * by {@code RandomDistributedScalarEncoder}: <br/>
 * 1) Similar scalars should have high overlap. Overlap should decrease smoothly
 * as scalars become less similar. Specifically, neighboring bucket indices must
 * overlap by a linearly decreasing number of bits. <br/>
 * 2) Dissimilar scalars should have very low overlap so that the SP does not
 * confuse representations. Specifically, buckets that are more than {@code w}
 * indices apart should have at most MAX_OVERLAP bits of overlap. We arbitrarily
 * (and safely) define "very low" to be 2 bits of overlap or lower. <br/>
 * Properties 1 and 2 lead to the following overlap rules for buckets {@code i}
 * and {@code j}:
 *
 * <pre>
 *     <code>
 *   if abs(i-j) < w then:
 *              overlap(i,j) = w - abs(i-j)
 *          else:
 *              overlap(i,j) <= MAX_OVERLAP
 *     </code>
 * </pre>
 *
 * <br/>
 * 3) The representation for a scalar must not change during the lifetime of the
 * object. Specifically, as new buckets are created and the min/max range is
 * extended, the representation for previously in-range scalars and previously
 * created buckets must not change. </p>
 *
 * @author Sean Connolly
 */
public class RandomDistributedScalarEncoder extends Encoder<Double> {

	private static final int DEFAULT_W = 21;
	private static final int DEFAULT_N = 400;
	private static final int DEFAULT_SEED = 42;
	private static final int DEFAULT_VERBOSITY = 0;
	private static final int DEFAULT_MAX_BUCKETS = 1000;
	// The largest overlap we allow for non-adjacent encodings
	private static final int MAX_OVERLAP = 2;

	// TODO use org.numenta.nupic.util.MersenneTwister instead of
	// java.util.Random?
	private final Random random = new Random();
	private int seed = DEFAULT_SEED;

	// The first bucket index will be _maxBuckets / 2 and bucket indices will be
	// allowed to grow lower or higher as long as they don't become negative.
	// _maxBuckets is required because the current CLA Classifier assumes bucket
	// indices must be non-negative. This normally does not need to be changed
	// but if altered, should be set to an even number.
	// TODO maxBuckets can be static
	private final int maxBuckets = DEFAULT_MAX_BUCKETS;
	private int minIndex = maxBuckets / 2;
	private int maxIndex = maxBuckets / 2;

	// The scalar offset used to map scalar values to bucket indices.The middle
	// bucket will correspond to numbers in the range
	// [offset - resolution / 2, offset + resolution / 2).
	// The bucket index for a number x will be:
	// maxBuckets / 2 +int(round((x - offset) / resolution))
	private Double offset;

	// This dictionary maps a bucket index into its bit representation
	private final BucketMap bucketMap = new BucketMap();

	// How often we need to retry when generating valid encodings
	private final int numTries = 0;

	/**
	 * Returns a builder for building RandomDistributedScalarEncoder.<br/>
	 * This builder may be reused to produce multiple builders.
	 *
	 * @return a {@code RandomDistributedScalarEncoder.Builder}
	 */
	public static Encoder.Builder<RandomDistributedScalarEncoder.Builder, RandomDistributedScalarEncoder> builder() {
		return new RandomDistributedScalarEncoder.Builder();
	}

	public RandomDistributedScalarEncoder(double resolution) {
		this(resolution, DEFAULT_W, DEFAULT_N, DEFAULT_SEED, DEFAULT_VERBOSITY);
	}

	public RandomDistributedScalarEncoder(double resolution, int w, int n,
			int seed, int verbosity) {
		this(resolution, w, n, null, null, seed, verbosity);
	}

	public RandomDistributedScalarEncoder(double resolution, int w, int n,
			String name, Double offset, int seed, int verbosity) {
		this.resolution = resolution;
		this.w = w;
		this.n = n;
		this.name = name;
		this.offset = offset;
		this.seed = seed;
		this.verbosity = verbosity;
	}

	public void init() {
		validateState();
		if (seed != -1) {
			random.setSeed(seed);
		}
		bucketMap.init();
		// TODO set 'name' to toString() or getClass().getSimpleName()?
		if (verbosity > 0) {
			// TODO utilize a real logging mechanism
			System.out.println(toString());
		}
	}

	/**
	 * Validate the encoder parameters.
	 *
	 * @throws IllegalStateException
	 *             if the current state is invalid
	 */
	private void validateState() throws IllegalStateException {
		if (w <= 0 || w % 2 == 0) {
			throw new IllegalStateException(
					"w must be an odd positive integer: " + w);
		}
		if (resolution <= 0) {
			throw new IllegalStateException(
					"resolution must be a positive number: " + resolution);
		}
		if (n <= 6 * w) {
			throw new IllegalStateException(
					"n must be an int strictly greater than 6*w. For "
							+ "good results we recommend n be strictly greater "
							+ "than 11*w");
		}
	}

	/**
	 * @return the random number generator seed.
	 */
	public int getSeed() {
		return seed;
	}

	/**
	 * Set the seed used for the random number generator. If set to {@code -1}
	 * the generator will be initialized without a fixed seed. <br/>
	 * <b>Note:</b> call {@link #init()} after changing the seed to reinitialize
	 * the generator.
	 *
	 * @param seed
	 *            the random number generator seed
	 */
	public void setSeed(int seed) {
		// TODO drop class variable and pass directly to random.setSet(seed)?
		this.seed = seed;
	}

	/**
	 * @return the offset used to map scalar inputs to bucket indices
	 */
	public Double getOffset() {
		return offset;
	}

	/**
	 * Set the floating point offset used to map scalar inputs to bucket
	 * indices. The middle bucket will correspond to numbers in the range
	 * {@code [offset - resolution/2, offset + resolution/2)}. If set to
	 * {@code null}, the very first input that is encoded will be used to
	 * determine the offset.
	 *
	 * @param offset
	 *            the offset used to map scalar inputs to bucket indices
	 */
	public void setOffset(Double offset) {
		this.offset = offset;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encodeIntoArray(Double inputData, int[] output) {
		if (inputData == null) {
			throw new IllegalArgumentException("null data to encode.");
		}
		Integer bucketIndex = getBucketIndices(inputData)[0];
		// null is returned for missing value in which case we return all 0's.
		if (bucketIndex != null) {
			int[] onBits = mapBucketIndexToNonZeroBits(bucketIndex);
			for (int onBit : onBits) {
				output[onBit] = 1;
			}
		}
	}

	/**
	 * Returns an array containing the sub-field bucket indices for each
	 * sub-field of the inputData. To get the associated field names for each of
	 * the buckets, call getScalarNames().
	 *
	 * @param input
	 *            The data from the source; in this case, a scalar.
	 *
	 * @return array of bucket indices
	 */
	@Override
	public int[] getBucketIndices(double input) {
		if (offset == null) {
			offset = input;
		}
		int index = maxBuckets / 2
				+ (int) Math.round((input - offset) / resolution);
		if (index < 0) {
			index = 0;
		} else if (index >= maxBuckets) {
			index = maxBuckets - 1;
		}
		return new int[] { index };
	}

	/**
	 * Given a bucket index, return the list of non-zero bits. If the bucket
	 * index does not exist, it is created. If the index falls outside our range
	 * we clip it.
	 *
	 * @param index
	 *            the bucket index
	 *
	 * @return non-zero bits in the bucket
	 */
	public int[] mapBucketIndexToNonZeroBits(int index) {
		if (index < 0) {
			index = 0;
		}
		if (index >= maxBuckets) {
			index = maxBuckets - 1;
		}
		if (!bucketMap.containsKey(index)) {
			if (verbosity >= 2) {
				System.out.println("Adding additional buckets to handle index="
						+ index);
			}
			bucketMap.createBucket(index);
		}
		return bucketMap.get(index);
	}

	public BucketMap getBucketMap() {
		return bucketMap;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public <S> List<S> getBucketValues(Class<S> returnType) {
		return null;
	}

	/**
	 * <p>
	 * Returns a {@link Encoder.Builder} for constructing
	 * {@link RandomDistributedScalarEncoder}s.
	 * </p>
	 * <p>
	 * The base class architecture is put together in such a way where
	 * boilerplate initialization can be kept to a minimum for implementing
	 * subclasses, while avoiding the mistake-proneness of extremely long
	 * argument lists.
	 * </p>
	 */
	public static class Builder
			extends
			Encoder.Builder<RandomDistributedScalarEncoder.Builder, RandomDistributedScalarEncoder> {

		private int seed;
		private Double offset;

		private Builder() {
		}

		@Override
		public RandomDistributedScalarEncoder build() {
			encoder = new RandomDistributedScalarEncoder(resolution);
			w = DEFAULT_W;
			n = DEFAULT_N;
			seed = DEFAULT_SEED;
			encVerbosity = DEFAULT_VERBOSITY;
			super.build();
			((RandomDistributedScalarEncoder) encoder).setSeed(this.seed);
			((RandomDistributedScalarEncoder) encoder).setOffset(this.offset);
			((RandomDistributedScalarEncoder) encoder).init();
			return (RandomDistributedScalarEncoder) encoder;
		}

		public RandomDistributedScalarEncoder.Builder seed(int seed) {
			this.seed = seed;
			return this;
		}

		public RandomDistributedScalarEncoder.Builder offset(Double offset) {
			this.offset = offset;
			return this;
		}

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getWidth() {
		return getN();
	}

	/**
	 * {@inheritDoc} <br/>
	 * <b>Note:</b> Always returns {@code false} for
	 * {@link RandomDistributedScalarEncoder}.
	 */
	@Override
	public boolean isDelta() {
		return false;
	}

	/**
	 * Maps ...?
	 *
	 * @author Sean Connolly
	 */
	protected final class BucketMap extends HashMap<Integer, int[]> {

		/**
		 * Initialize the bucket map assuming the given number of maxBuckets.
		 */
		private void init() {
			clear();
			// We initialize the class with a single bucket with index 0
			int[] bucket = new int[w]; // a bucket consists of w ON bits
			for (int i = 0; i < w; i++) { // create w ON bits
				// ON bits are in the range 0 (inclusive) to n (exclusive)
				// TODO 0 (inclusive) to n (exclusive).. should be n+1?
				bucket[i] = random.nextInt(n);
			}
			put(minIndex, bucket);
		}

		void createBucket(int index) {

		}

		public int size() {
			return keySet().size();
		}

	}

}
