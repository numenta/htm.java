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

package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.util.UniversalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A scalar encoder encodes a numeric (floating point) value into an array of
 * bits.
 *
 * This class maps a scalar value into a random distributed representation that
 * is suitable as scalar input into the spatial pooler. The encoding scheme is
 * designed to replace a simple ScalarEncoder. It preserves the important
 * properties around overlapping representations. Unlike ScalarEncoder the min
 * and max range can be dynamically increased without any negative effects. The
 * only required parameter is resolution, which determines the resolution of
 * input values.
 *
 * Scalar values are mapped to a bucket. The class maintains a random
 * distributed encoding for each bucket. The following properties are maintained
 * by RandomDistributedEncoder:
 * </p>
 * <ol>
 * <li>Similar scalars should have high overlap. Overlap should decrease
 * smoothly as scalars become less similar. Specifically, neighboring bucket
 * indices must overlap by a linearly decreasing number of bits.
 *
 * <li>Dissimilar scalars should have very low overlap so that the SP does not
 * confuse representations. Specifically, buckets that are more than w indices
 * apart should have at most maxOverlap bits of overlap. We arbitrarily (and
 * safely) define "very low" to be 2 bits of overlap or lower.
 *
 * Properties 1 and 2 lead to the following overlap rules for buckets i and j:<br>
 *
 * <pre>
 * {@code
 * If abs(i-j) < w then:
 * 		overlap(i,j) = w - abs(i-j);
 * else:
 * 		overlap(i,j) <= maxOverlap;
 * }
 * </pre>
 *
 * <li>The representation for a scalar must not change during the lifetime of
 * the object. Specifically, as new buckets are created and the min/max range is
 * extended, the representation for previously in-range scalars and previously
 * created buckets must not change.
 * </ol>
 *
 *
 * @author Numenta
 * @author Anubhav Chaturvedi
 */

public class RandomDistributedScalarEncoder extends Encoder<Double> {

	private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(RandomDistributedScalarEncoder.class);

	public static final long DEFAULT_SEED = 42;

	// UniversalRandom is faster than MersenneTwister, therefore is recommended
	UniversalRandom rng;

	int maxOverlap;
	int maxBuckets;
	Double offset;
	long seed;
	int minIndex;
	int maxIndex;
	int numRetry;

	ConcurrentHashMap<Integer, List<Integer>> bucketMap;

	RandomDistributedScalarEncoder() {
	}

	public static Encoder.Builder<RandomDistributedScalarEncoder.Builder, RandomDistributedScalarEncoder> builder() {
		return new RandomDistributedScalarEncoder.Builder();
	}

	/**
	 * Perform validation on state parameters and proceed with initialization of
	 * the encoder.
	 *
	 * @throws IllegalStateException
	 *             Throws {@code IllegalStateException} containing appropriate
	 *             message if some validation fails.
	 */
	public void init() throws IllegalStateException {
		if (getW() <= 0 || getW() % 2 == 0)
			throw new IllegalStateException(
			    "W must be an odd positive integer (to eliminate centering difficulty)");

		setHalfWidth((getW() - 1) / 2);

		if (getResolution() <= 0)
			throw new IllegalStateException(
                "Resolution must be a positive number");

		if (n <= 6 * getW())
			throw new IllegalStateException(
			    "n must be strictly greater than 6*w. For good results we "
                    + "recommend n be strictly greater than 11*w.");

		initEncoder(getResolution(), getW(), getN(), getOffset(), getSeed());
	}

	/**
	 * Perform the initialization of the encoder.
	 *
	 * @param resolution
	 * @param w
	 * @param n
	 * @param offset
	 * @param seed
	 */
	// TODO why are none of these parameters used..?
	public void initEncoder(double resolution, int w, int n, Double offset, long seed) {
		rng = (seed == -1) ? new UniversalRandom(DEFAULT_SEED) : new UniversalRandom(seed);

		initializeBucketMap(getMaxBuckets(), getOffset());

		if (getName() == null || getName().isEmpty()) {
			setName("[" + getResolution() + "]");
		}

		// TODO reduce logging level?
		LOG.debug(this.toString());
	}

	/**
	 * Initialize the bucket map assuming the given number of maxBuckets.
	 *
	 * @param maxBuckets
	 * @param offset
	 */
	public void initializeBucketMap(int maxBuckets, Double offset) {
		/*
		 * The first bucket index will be _maxBuckets / 2 and bucket indices
		 * will be allowed to grow lower or higher as long as they don't become
		 * negative. _maxBuckets is required because the current CLA Classifier
		 * assumes bucket indices must be non-negative. This normally does not
		 * need to be changed but if altered, should be set to an even number.
		 */

		setMaxBuckets(maxBuckets);

		setMinIndex(maxBuckets / 2);
		setMaxIndex(maxBuckets / 2);

		/*
		 * The scalar offset used to map scalar values to bucket indices. The
		 * middle bucket will correspond to numbers in the range
		 * [offset-resolution/2, offset+resolution/2). The bucket index for a
		 * number x will be: maxBuckets/2 + int( round( (x-offset)/resolution )
		 * )
		 */
		setOffset(offset);

		/*
		 * This HashMap maps a bucket index into its bit representation We
		 * initialize the HashMap with a single bucket with index 0
		 */
		bucketMap = new ConcurrentHashMap<Integer, List<Integer>>();
		// generate the random permutation
		int[] t = ArrayUtils.range(0, getN());
		rng.shuffle(t);
		bucketMap.put(getMinIndex(), 
		    Arrays.stream(t).boxed().limit(getW()).collect(Collectors.toList()));

		// How often we need to retry when generating valid encodings
		setNumRetry(0);
	}

	/**
	 * Create the given bucket index. Recursively create as many in-between
	 * bucket indices as necessary.
	 *
	 * @param index the index at which bucket needs to be created
	 * @throws IllegalStateException
	 */
	public void createBucket(int index) throws IllegalStateException {
		if (index < getMinIndex()) {
			if (index == getMinIndex() - 1) {
				/*
				 * Create a new representation that has exactly w-1 overlapping
				 * bits as the min representation
				 */
				bucketMap.put(index, newRepresentation(getMinIndex(), index));
				setMinIndex(index);
			} else {
				// Recursively create all the indices above and then this index
				createBucket(index + 1);
				createBucket(index);
			}
		} else {
			if (index == getMaxIndex() + 1) {
				/*
				 * Create a new representation that has exactly w-1 overlapping
				 * bits as the max representation
				 */
				bucketMap.put(index, newRepresentation(getMaxIndex(), index));
				setMaxIndex(index);
			} else {
				// Recursively create all the indices below and then this index
				createBucket(index - 1);
				createBucket(index);
			}
		}
	}

	/**
	 * Get a new representation for newIndex that overlaps with the
	 * representation at index by exactly w-1 bits
	 *
	 * @param index
	 * @param newIndex
	 * @throws IllegalStateException
	 */
	public List<Integer> newRepresentation(int index, int newIndex)
			throws IllegalStateException {
		List<Integer> newRepresentation = new ArrayList<Integer>(
				bucketMap.get(index));

		/*
		 * Choose the bit we will replace in this representation. We need to
		 * shift this bit deterministically. If this is always chosen randomly
		 * then there is a 1 in w chance of the same bit being replaced in
		 * neighboring representations, which is fairly high
		 */

		int ri = newIndex % getW();

		// Now we choose a bit such that the overlap rules are satisfied.
		int newBit = rng.nextInt(getN());
		newRepresentation.set(ri, newBit);
		while (bucketMap.get(index).contains(newBit)
				|| !newRepresentationOK(newRepresentation, newIndex)) {
			setNumRetry(getNumRetry() + 1);
			newBit = rng.nextInt(getN());
			newRepresentation.set(ri, newBit);
		}

		return newRepresentation;
	}

	/**
	 * Check if this new candidate representation satisfies all our
	 * overlap rules. Since we know that neighboring representations differ by
	 * at most one bit, we compute running overlaps.
	 *
	 * @param newRep Encoded SDR to be considered
	 * @param newIndex The index being considered
	 * @return {@code true} if newRep satisfies all our overlap rules
	 * @throws IllegalStateException
	 */
	public boolean newRepresentationOK(List<Integer> newRep, int newIndex) {
		if (newRep.size() != getW())
			return false;
		if (newIndex < getMinIndex() - 1 || newIndex > getMaxIndex() + 1) {
			throw new IllegalStateException(
					"newIndex must be within one of existing indices");
		}

		// A binary representation of newRep. We will use this to test
		// containment
		boolean[] newRepBinary = new boolean[getN()];
		Arrays.fill(newRepBinary, false);
		for (int index : newRep)
			newRepBinary[index] = true;

		// Midpoint
		int midIdx = getMaxBuckets() / 2;

		// Start by checking the overlap at minIndex
		int runningOverlap = countOverlap(bucketMap.get(getMinIndex()), newRep);
		if (!overlapOK(getMinIndex(), newIndex, runningOverlap))
			return false;

		// Compute running overlaps all the way to the midpoint
		for (int i = getMinIndex() + 1; i < midIdx + 1; i++) {
			// This is the bit that is going to change
			int newBit = (i - 1) % getW();

			// Update our running overlap
			if (newRepBinary[bucketMap.get(i - 1).get(newBit)])
				runningOverlap--;
			if (newRepBinary[bucketMap.get(i).get(newBit)])
				runningOverlap++;

			// Verify our rules
			if (!overlapOK(i, newIndex, runningOverlap))
				return false;
		}

		// At this point, runningOverlap contains the overlap for midIdx
		// Compute running overlaps all the way to maxIndex
		for (int i = midIdx + 1; i <= getMaxIndex(); i++) {
			int newBit = i % getW();

			// Update our running overlap
			if (newRepBinary[bucketMap.get(i - 1).get(newBit)])
				runningOverlap--;
			if (newRepBinary[bucketMap.get(i).get(newBit)])
				runningOverlap++;

			// Verify our rules
			if (!overlapOK(i, newIndex, runningOverlap))
				return false;
		}
		return true;
	}

	/**
	 * Get the overlap between two representations. rep1 and rep2 are
	 * {@link List} of non-zero indices.
	 *
	 * @param rep1 The first representation for overlap calculation
	 * @param rep2 The second representation for overlap calculation
	 * @return The number of 'on' bits that overlap
	 */
	public int countOverlap(List<Integer> rep1, List<Integer> rep2) {
		int overlap = 0;
		for (int index : rep1) {
			for (int index2 : rep2)
				if (index == index2)
					overlap++;
		}
		return overlap;
	}

	/**
	 * Get the overlap between two representations. rep1 and rep2 are arrays
	 * of non-zero indices.
	 *
	 * @param rep1 The first representation for overlap calculation
	 * @param rep2 The second representation for overlap calculation
	 * @return The number of 'on' bits that overlap
	 */
	public int countOverlap(int[] rep1, int[] rep2) {
		int overlap = 0;
		for (int index : rep1) {
			for (int index2 : rep2)
				if (index == index2)
					overlap++;
		}
		return overlap;
	}

	/**
	 * Check if the given overlap between bucket indices i and j are acceptable.
	 *
	 * @param i The index of the bucket to be compared
	 * @param j The index of the bucket to be compared
	 * @param overlap The overlap between buckets at index i and j
	 * @return {@code true} if overlap is acceptable, else {@code false}
	 */
	public boolean overlapOK(int i, int j, int overlap) {
		if (Math.abs(i - j) < getW() && overlap == (getW() - Math.abs(i - j)))
			return true;
		if (Math.abs(i - j) >= getW() && overlap <= getMaxOverlap())
			return true;

		return false;
	}

	/**
	 * Check if the overlap between the buckets at indices i and j are
	 * acceptable. The overlap is calculate from the bucketMap.
	 *
	 * @param i The index of the bucket to be compared
	 * @param j The index of the bucket to be compared
	 * @return {@code true} if the given overlap is acceptable, else {@code false}
	 * @throws IllegalStateException
	 */
	public boolean overlapOK(int i, int j) throws IllegalStateException {
		return overlapOK(i, j, countOverlapIndices(i, j));
	}

	/**
	 * Get the overlap between bucket at indices i and j
	 *
	 * @param i The index of the bucket
	 * @param j The index of the bucket
	 * @return the overlap between bucket at indices i and j
	 * @throws IllegalStateException
	 */
	public int countOverlapIndices(int i, int j) throws IllegalStateException {
		boolean containsI = bucketMap.containsKey(i);
		boolean containsJ = bucketMap.containsKey(j);
		if (containsI && containsJ) {
			List<Integer> rep1 = bucketMap.get(i);
			List<Integer> rep2 = bucketMap.get(j);
			return countOverlap(rep1, rep2);
		} else if (!containsI && !containsJ)
			throw new IllegalStateException("index " + i + " and " + j + " don't exist");
		else if(!containsI)
			throw new IllegalStateException("index " + i + " doesn't exist");
		else
			throw new IllegalStateException("index " + j + " doesn't exist");
	}

	/**
	 * Given a bucket index, return the list of indices of the 'on' bits. If the
	 * bucket index does not exist, it is created. If the index falls outside
	 * our range we clip it.
	 *
	 * @param index The bucket index
	 * @return The list of active bits in the representation
	 * @throws IllegalStateException
	 */
	public List<Integer> mapBucketIndexToNonZeroBits(int index)
			throws IllegalStateException {
		if (index < 0)
			index = 0;

		if (index >= getMaxBuckets())
			index = getMaxBuckets() - 1;

		if (!bucketMap.containsKey(index)) {
			LOG.trace("Adding additional buckets to handle index={} ", index);
			createBucket(index);
		}
		return bucketMap.get(index);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int[] getBucketIndices(double x) {
		if (Double.isNaN(x))
			x = Encoder.SENTINEL_VALUE_FOR_MISSING_DATA;

		int test = Double.compare(x, Encoder.SENTINEL_VALUE_FOR_MISSING_DATA);
		if (test == 0)
			return new int[0];

		if (getOffset() == null)
			setOffset(x);

		/*
		 * Difference in the round function behavior for Python and Java In
		 * Python, the absolute value is rounded up and sign is applied in Java,
		 * value is rounded to next biggest integer
		 *
		 * so for Python, round(-0.5) => -1.0 whereas in Java, Math.round(-0.5)
		 * => 0.0
		 */
		double deltaIndex = (x - getOffset()) / getResolution();
		int sign = (int) (deltaIndex / Math.abs(deltaIndex));
		int bucketIdx = (getMaxBuckets() / 2)
				+ (sign * (int) Math.round(Math.abs(deltaIndex)));

		if (bucketIdx < 0)
			bucketIdx = 0;
		else if (bucketIdx >= getMaxBuckets())
			bucketIdx = getMaxBuckets() - 1;

		int[] bucketIdxArray = new int[1];
		bucketIdxArray[0] = bucketIdx;
		return bucketIdxArray;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getWidth() {
		return getN();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isDelta() {
		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLearning(boolean learningEnabled) {
		setLearningEnabled(learningEnabled);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Tuple> getDescription() {

		String name = getName();
		if (name == null || name.isEmpty())
			setName("[" + getResolution() + "]");
		name = getName();

		return new ArrayList<Tuple>(Arrays.asList(new Tuple[] { new Tuple(name,
				0) }));
	}

	/**
	 * @return maxOverlap for this RDSE
	 */
	public int getMaxOverlap() {
		return maxOverlap;
	}

	/**
	 * @return the maxBuckets for this RDSE
	 */
	public int getMaxBuckets() {
		return maxBuckets;
	}

	/**
	 * @return the seed for the random number generator
	 */
	public long getSeed() {
		return seed;
	}

	/**
	 * @return the offset
	 */
	public Double getOffset() {
		return offset;
	}

	private int getMinIndex() {
		return minIndex;
	}

	private int getMaxIndex() {
		return maxIndex;
	}

	/**
	 * @return the number of retry to create new bucket
	 */
	public int getNumRetry() {
		return numRetry;
	}

	/**
	 * @param maxOverlap The maximum permissible overlap between representations
	 */
	public void setMaxOverlap(int maxOverlap) {
		this.maxOverlap = maxOverlap;
	}

	/**
	 * @param maxBuckets the new maximum number of buckets allowed
	 */
	public void setMaxBuckets(int maxBuckets) {
		this.maxBuckets = maxBuckets;
	}

	/**
	 * @param seed
	 */
	public void setSeed(long seed) {
		this.seed = seed;
	}

	/**
	 * @param offset
	 */
	public void setOffset(Double offset) {
		this.offset = offset;
	}

	private void setMinIndex(int minIndex) {
		this.minIndex = minIndex;
	}

	private void setMaxIndex(int maxIndex) {
		this.maxIndex = maxIndex;
	}

	/**
	 * @param numRetry New number of retries for new representation
	 */
	public void setNumRetry(int numRetry) {
		this.numRetry = numRetry;
	}

	@Override
	public String toString() {
		// TODO don't mix StringBuilder appending with String concatenation
		StringBuilder dumpString = new StringBuilder(50);
		dumpString.append("RandomDistributedScalarEncoder:\n");
		dumpString.append("  minIndex: " + getMinIndex() + "\n");
		dumpString.append("  maxIndex: " + getMaxIndex() + "\n");
		dumpString.append("  w: " + getW() + "\n");
		dumpString.append("  n: " + getWidth() + "\n");
		dumpString.append("  resolution: " + getResolution() + "\n");
		dumpString.append("  offset: " + getOffset() + "\n");
		dumpString.append("  numTries: " + getNumRetry() + "\n");
		dumpString.append("  name: " + getName() + "\n");
		dumpString.append("  buckets : \n");
		for (int index : bucketMap.keySet()) {
			dumpString.append("  [ " + index + " ]: "
					+ Arrays.deepToString(bucketMap.get(index).toArray())
					+ "\n");
		}
		return dumpString.toString();
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
	 *
	 * @author Anubhav Chaturvedi
	 */
	public static class Builder
			extends
			Encoder.Builder<RandomDistributedScalarEncoder.Builder, RandomDistributedScalarEncoder> {

		private int maxOverlap;
		private int maxBuckets;
		private Double offset;
		private long seed;
		int minIndex;
		int maxIndex;

		private Builder() {
			this.n(400);
			this.w(21);
			seed = 42;
			maxBuckets = 1000;
			maxOverlap = 2;
			offset = null;
		}

		private Builder(int n, int w) {
			this();
			this.n(n);
			this.w(w);
		}

		@Override
		public RandomDistributedScalarEncoder build() {
			// Must be instantiated so that super class can initialize
			// boilerplate variables.
			encoder = new RandomDistributedScalarEncoder();

			// Call super class here
			RandomDistributedScalarEncoder partialBuild = super.build();

			////////////////////////////////////////////////////////
			//  Implementing classes would do setting of specific //
			//  vars here together with any sanity checking       //
			////////////////////////////////////////////////////////
			partialBuild.setSeed(seed);
			partialBuild.setMaxOverlap(maxOverlap);
			partialBuild.setMaxBuckets(maxBuckets);
			partialBuild.setOffset(offset);
			partialBuild.setNumRetry(0);

			partialBuild.init();

			return partialBuild;
		}

		public RandomDistributedScalarEncoder.Builder setOffset(double offset) {
			this.offset = Double.valueOf(offset);
			return this;
		}

		public RandomDistributedScalarEncoder.Builder setMaxBuckets(
				int maxBuckets) {
			this.maxBuckets = maxBuckets;
			return this;
		}

		public RandomDistributedScalarEncoder.Builder setMaxOverlap(
				int maxOverlap) {
			this.maxOverlap = maxOverlap;
			return this;
		}

		public RandomDistributedScalarEncoder.Builder setSeed(long seed) {
			this.seed = seed;
			return this;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void encodeIntoArray(Double inputData, int[] output) {
		int[] bucketIdx = getBucketIndices(inputData);
		Arrays.fill(output, 0);

		if (bucketIdx.length == 0)
			return;

		if (bucketIdx[0] != Integer.MIN_VALUE) {
			List<Integer> indices;
			try {
				indices = mapBucketIndexToNonZeroBits(bucketIdx[0]);
				for (int index : indices)
					output[index] = 1;
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <S> List<S> getBucketValues(Class<S> returnType) {
		return new ArrayList<>((Collection<S>)this.bucketMap.values());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<FieldMetaType> getDecoderOutputFieldTypes() {
		return new LinkedHashSet<FieldMetaType>(Arrays.asList(FieldMetaType.FLOAT, FieldMetaType.INTEGER));
	}
}
