package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.data.FieldMetaType;
import org.numenta.nupic.research.Connections;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;


/**
 * DOCUMENTATION TAKE DIRECTLY FROM THE PYTHON VERSION:
 * 
 * A scalar encoder encodes a numeric (floating point) value into an array
 * of bits. The output is 0's except for a contiguous block of 1's. The
 * location of this contiguous block varies continuously with the input value.
 *
 * The encoding is linear. If you want a nonlinear encoding, just transform
 * the scalar (e.g. by applying a logarithm function) before encoding.
 * It is not recommended to bin the data as a pre-processing step, e.g.
 * "1" = $0 - $.20, "2" = $.21-$0.80, "3" = $.81-$1.20, etc. as this
 * removes a lot of information and prevents nearby values from overlapping
 * in the output. Instead, use a continuous transformation that scales
 * the data (a piecewise transformation is fine).
 *
 *
 * Parameters:
 * -----------------------------------------------------------------------------
 * w --        The number of bits that are set to encode a single value - the
 *             "width" of the output signal
 *             restriction: w must be odd to avoid centering problems.
 *
 * minval --   The minimum value of the input signal.
 *
 * maxval --   The upper bound of the input signal
 *
 * periodic -- If true, then the input value "wraps around" such that minval = maxval
 *             For a periodic value, the input must be strictly less than maxval,
 *             otherwise maxval is a true upper bound.
 *
 * There are three mutually exclusive parameters that determine the overall size of
 * of the output. Only one of these should be specifed to the constructor:
 *
 * n      --      The number of bits in the output. Must be greater than or equal to w
 * radius --      Two inputs separated by more than the radius have non-overlapping
 *                representations. Two inputs separated by less than the radius will
 *                in general overlap in at least some of their bits. You can think
 *                of this as the radius of the input.
 * resolution --  Two inputs separated by greater than, or equal to the resolution are guaranteed
 *                 to have different representations.
 *
 * Note: radius and resolution are specified w.r.t the input, not output. w is
 * specified w.r.t. the output.
 *
 * Example:
 * day of week.
 * w = 3
 * Minval = 1 (Monday)
 * Maxval = 8 (Monday)
 * periodic = true
 * n = 14
 * [equivalently: radius = 1.5 or resolution = 0.5]
 *
 * The following values would encode midnight -- the start of the day
 * monday (1)   -> 11000000000001
 * tuesday(2)   -> 01110000000000
 * wednesday(3) -> 00011100000000
 * ...
 * sunday (7)   -> 10000000000011
 *
 * Since the resolution is 12 hours, we can also encode noon, as
 * monday noon  -> 11100000000000
 * monday midnt-> 01110000000000
 * tuesday noon -> 00111000000000
 * etc.
 *
 *
 * It may not be natural to specify "n", especially with non-periodic
 * data. For example, consider encoding an input with a range of 1-10
 * (inclusive) using an output width of 5.  If you specify resolution =
 * 1, this means that inputs of 1 and 2 have different outputs, though
 * they overlap, but 1 and 1.5 might not have different outputs.
 * This leads to a 14-bit representation like this:
 *
 * 1 ->  11111000000000  (14 bits total)
 * 2 ->  01111100000000
 * ...
 * 10->  00000000011111
 * [resolution = 1; n=14; radius = 5]
 *
 * You could specify resolution = 0.5, which gives
 * 1   -> 11111000... (22 bits total)
 * 1.5 -> 011111.....
 * 2.0 -> 0011111....
 * [resolution = 0.5; n=22; radius=2.5]
 *
 * You could specify radius = 1, which gives
 * 1   -> 111110000000....  (50 bits total)
 * 2   -> 000001111100....
 * 3   -> 000000000011111...
 * ...
 * 10  ->                           .....000011111
 * [radius = 1; resolution = 0.2; n=50]
 *
 *
 * An N/M encoding can also be used to encode a binary value,
 * where we want more than one bit to represent each state.
 * For example, we could have: w = 5, minval = 0, maxval = 1,
 * radius = 1 (which is equivalent to n=10)
 * 0 -> 1111100000
 * 1 -> 0000011111
 *
 *
 * Implementation details:
 * --------------------------------------------------------------------------
 * range = maxval - minval
 * h = (w-1)/2  (half-width)
 * resolution = radius / w
 * n = w * range/radius (periodic)
 * n = w * range/radius + 2 * h (non-periodic)
 * 
 * @author metaware
 */
public class ScalarEncoder extends Encoder {
	/**
	 * Constructs a new {@code ScalarEncoder}
	 */
	public ScalarEncoder() {}
	
	/**
	 * Returns true if the underlying encoder works on deltas
	 */
	public boolean isDelta() {
		return false;
	}
	
	/**
	 * w -- number of bits to set in output
     * minval -- minimum input value
     * maxval -- maximum input value (input is strictly less if periodic == True)
	 *
     * Exactly one of n, radius, resolution must be set. "0" is a special
     * value that means "not set".
	 *
     * n -- number of bits in the representation (must be > w)
     * radius -- inputs separated by more than, or equal to this distance will have non-overlapping
     * representations
     * resolution -- inputs separated by more than, or equal to this distance will have different
     * representations
	 * 
     * name -- an optional string which will become part of the description
	 *
     * clipInput -- if true, non-periodic inputs smaller than minval or greater
     * than maxval will be clipped to minval/maxval
	 *
     * forced -- if true, skip some safety checks (for compatibility reasons), default false
     * 
	 * @param c		the memory
	 */
	public void init(Connections c) {
		if(c.getW() % 2 == 0) {
			throw new IllegalStateException(
				"W must be an odd number (to eliminate centering difficulty)");
		}
		
		c.setHalfWidth((c.getW() - 1) / 2);
		
		// For non-periodic inputs, padding is the number of bits "outside" the range,
	    // on each side. I.e. the representation of minval is centered on some bit, and
	    // there are "padding" bits to the left of that centered bit; similarly with
	    // bits to the right of the center bit of maxval
		c.setPadding(c.isPeriodic() ? 0 : c.getHalfWidth());
		
		if(c.getMinVal() != 0 && c.getMaxVal() != 0) {
			if(c.getMinVal() >= c.getMaxVal()) {
				throw new IllegalStateException("maxVal must be > minVal");
			}
			c.setRangeInternal(c.getMaxVal() - c.getMinVal());
		}
		
		// There are three different ways of thinking about the representation. Handle
	    // each case here.
		initEncoder(c, c.getW(), c.getMinVal(), c.getMaxVal(), c.getN(), c.getRadius(), c.getResolution());
		
		//nInternal represents the output area excluding the possible padding on each side
		c.setNInternal(c.getN() - 2 * c.getPadding());
		
		if(c.getName() == null) {
			if((c.getMinVal() % ((int)c.getMinVal())) > 0 ||
			    (c.getMaxVal() % ((int)c.getMaxVal())) > 0) {
				c.setName("[" + c.getMinVal() + ":" + c.getMaxVal() + "]");
			}else{
				c.setName("[" + (int)c.getMinVal() + ":" + (int)c.getMaxVal() + "]");
			}
		}
		
		//Checks for likely mistakes in encoder settings
		if(!c.isForced()) {
			checkReasonableSettings(c);
		}
	}
	
	/**
	 * There are three different ways of thinking about the representation. 
     * Handle each case here.
     * 
	 * @param c
	 * @param minVal
	 * @param maxVal
	 * @param n
	 * @param radius
	 * @param resolution
	 */
	public void initEncoder(Connections c, int w, double minVal, double maxVal, int n, double radius, double resolution) {
		if(n != 0) {
			if(minVal != 0 && maxVal != 0) {
			    if(!c.isPeriodic()) {
					c.setResolution(c.getRangeInternal() / (c.getN() - c.getW()));
				}else{
					c.setResolution(c.getRangeInternal() / c.getN());
				}
				
				c.setRadius(c.getW() * c.getResolution());
				
				if(c.isPeriodic()) {
					c.setRange(c.getRangeInternal());
				}else{
					c.setRange(c.getRangeInternal() + c.getResolution());
				}
			}
		}else{
			if(radius != 0) {
				c.setResolution(c.getRadius() / w);
			}else if(resolution != 0) {
				c.setRadius(c.getResolution() * w);
			}else{
				throw new IllegalStateException(
					"One of n, radius, resolution must be specified for a ScalarEncoder");
			}
			
			if(c.isPeriodic()) {
				c.setRange(c.getRangeInternal());
			}else{
				c.setRange(c.getRangeInternal() + c.getResolution());
			}
			
			double nFloat = w * (c.getRange() / c.getRadius()) + 2 * c.getPadding();
			c.setN((int)Math.ceil(nFloat));
		}
	}
	
	/**
	 * Set whether learning is enabled.
	 * 
	 * @param 	c					the connections memory
	 * @param 	learningEnabled		flag indicating whether learning is enabled
	 */
	public void setLearning(Connections c, boolean learningEnabled) {
		c.setLearningEnabled(learningEnabled);
	}
	
	/**
	 * {@inheritDoc}
	 * @param c		the connections memory
	 * @return		Tuple containing 
	 */
	@Override
	public List<Tuple> getDescription(Connections c) {
		//Throws UnsupportedOperationException if you try to add to the list
		//returned by Arrays.asList() ??? So we wrap it in yet another List?
		return new ArrayList<Tuple>(Arrays.asList(new Tuple[] { new Tuple(2, c.getName(), 0) }));
	}
	
	/**
	 * Return the bit offset of the first bit to be set in the encoder output.
     * For periodic encoders, this can be a negative number when the encoded output
     * wraps around.
     * 
	 * @param c			the memory
	 * @param input		the input data
	 * @return			an encoded array
	 */
	public Integer getFirstOnBit(Connections c, double input) {
		if(input == SENTINEL_VALUE_FOR_MISSING_DATA) {
			return null;
		}else{
			if(input < c.getMinVal()) {
				if(c.clipInput() && !c.isPeriodic()) {
					if(c.getEncVerbosity() > 0) {
						System.out.println("Clipped input " + c.getName() +
							"=" + input + " to minval " + c.getMinVal());
					}
					input = c.getMinVal();
				}else{
					throw new IllegalStateException("input (" + input +") less than range (" +
						c.getMinVal() + " - " + c.getMaxVal());
				}
			}
		}
		
		if(c.isPeriodic()) {
			if(input >= c.getMaxVal()) {
				throw new IllegalStateException("input (" + input +") greater than periodic range (" +
					c.getMinVal() + " - " + c.getMaxVal());
			}
		}else{
			if(input > c.getMaxVal()) {
				if(c.clipInput()) {
					if(c.getEncVerbosity() > 0) {
						System.out.println("Clipped input " + c.getName() + "=" + input + " to maxval " + c.getMaxVal());
					}
					
					input = c.getMaxVal();
				}else{
					throw new IllegalStateException("input (" + input +") greater than periodic range (" +
						c.getMinVal() + " - " + c.getMaxVal());
				}
			}
		}
		
		int centerbin;
		if(c.isPeriodic()) {
			centerbin = (int)((int)((input - c.getMinVal()) *  c.getNInternal() / c.getRange())) + c.getPadding();
		}else{
			centerbin = (int)((int)(((input - c.getMinVal()) + c.getResolution()/2) / c.getResolution())) + c.getPadding();
		}
		
		int minbin = centerbin - c.getHalfWidth();
		return minbin;
	}
	
	/**
	 * Check if the settings are reasonable for the SpatialPooler to work
	 * @param c
	 */
	public void checkReasonableSettings(Connections c) {
		if(c.getW() < 21) {
			throw new IllegalStateException(
				"Number of bits in the SDR (%d) must be greater than 2, and recommended >= 21 (use forced=True to override)");
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<FieldMetaType> getDecoderOutputFieldTypes(Connections c) {
		return Arrays.asList(new FieldMetaType[] { FieldMetaType.FLOAT });
	}
	
	/**
	 * Should return the output width, in bits.
	 */
	public int getWidth(Connections c) {
		return c.getN();
	}
	
	public int[] getBucketIndices(Connections c, double input) {
		int minbin = getFirstOnBit(c, input);
		
		//For periodic encoders, the bucket index is the index of the center bit
		int bucketIdx;
		if(c.isPeriodic()) {
			bucketIdx = minbin + c.getHalfWidth();
			if(bucketIdx < 0) {
				bucketIdx += c.getN();
			}
		}else{//for non-periodic encoders, the bucket index is the index of the left bit
			bucketIdx = minbin;
		}
		
		return new int[] { bucketIdx };
	}

	/**
	 * Encodes inputData and puts the encoded value into the numpy output array,
     * which is a 1-D array of length returned by {@link Connections#getW()}.
	 *
     * Note: The numpy output array is reused, so clear it before updating it.
     * 
	 * @param c
	 * @param inputData Data to encode. This should be validated by the encoder.
     * @param output 1-D array of same length returned by {@link Connections#getW()}
     * 
	 * @return
	 */
	@Override
	public int[] encodeIntoArray(Connections c, double input, int[] output) {
		if(Double.isNaN(input)) {
			return new int[0];
		}
		
		Integer bucketVal = getFirstOnBit(c, input);
		if(bucketVal != null) {
			int bucketIdx = bucketVal;
			Arrays.fill(output, 0);
			int minbin = bucketIdx;
			int maxbin = minbin + 2*c.getHalfWidth();
			if(c.isPeriodic()) {
				if(maxbin >= c.getN()) {
					int bottombins = maxbin - c.getN() + 1;
					int[] range = ArrayUtils.range(0, bottombins);
					ArrayUtils.setIndexesTo(output, range, 1);
					maxbin = c.getN() - 1;
				}
				if(minbin < 0) {
					int topbins = -minbin;
					ArrayUtils.setIndexesTo(
						output, ArrayUtils.range(c.getN() - topbins, c.getN()), 1);
					minbin = 0;
				}
			}
			
			ArrayUtils.setIndexesTo(output, ArrayUtils.range(minbin, maxbin + 1), 1);
		}
		
		if(c.getEncVerbosity() >= 2) {
			System.out.println("");
			System.out.println("input: " + input);
			System.out.println("range: " + c.getMinVal() + " - " + c.getMaxVal());
			System.out.println("n:" + c.getN() + "w:" + c.getW() + "resolution:" + c.getResolution() +
				"radius:" + c.getRadius() + "periodic:" + c.isPeriodic());
			System.out.println("output: " + Arrays.toString(output));
			System.out.println("input desc: " + decode(c, output, ""));
		}
		
		return output;
	}
	
	public DecodeResult decode(Connections c, int[] encoded, String parentFieldName) {
		// For now, we simply assume any top-down output greater than 0
	    // is ON. Eventually, we will probably want to incorporate the strength
	    // of each top-down output.
		if(encoded == null || encoded.length < 1) { 
			return null;
		}
		int[] tmpOutput = Arrays.copyOf(encoded, encoded.length);
		
		// ------------------------------------------------------------------------
	    // First, assume the input pool is not sampled 100%, and fill in the
	    //  "holes" in the encoded representation (which are likely to be present
	    //  if this is a coincidence that was learned by the SP).

	    // Search for portions of the output that have "holes"
		int maxZerosInARow = c.getHalfWidth();
		for(int i = 0;i < maxZerosInARow;i++) {
			int[] searchStr = new int[i + 3];
			Arrays.fill(searchStr, 1);
			ArrayUtils.setRangeTo(searchStr, 1, -1, 0);
			int subLen = searchStr.length;
			
			// Does this search string appear in the output?
			if(c.isPeriodic()) {
				for(int j = 0;j < c.getN();j++) {
					int[] outputIndices = ArrayUtils.range(j, j + subLen);
					outputIndices = ArrayUtils.modulo(outputIndices, c.getN());
					if(Arrays.equals(searchStr, ArrayUtils.sub(tmpOutput, outputIndices))) {
						ArrayUtils.setIndexesTo(tmpOutput, outputIndices, 1);
					}
				}
			}else{
				for(int j = 0;j < c.getN() - subLen + 1;j++) {
					if(Arrays.equals(searchStr, ArrayUtils.sub(tmpOutput, ArrayUtils.range(j, j + subLen)))) {
						ArrayUtils.setRangeTo(tmpOutput, j, j + subLen, 1);
					}
				}
			}
		}
		
		if(c.getEncVerbosity() >= 2) {
			System.out.println("raw output:" + Arrays.toString(
				ArrayUtils.sub(encoded, ArrayUtils.range(0, c.getN()))));
			System.out.println("filtered output:" + Arrays.toString(tmpOutput));
		}
		
		// ------------------------------------------------------------------------
	    // Find each run of 1's.
		int[] nz = ArrayUtils.where(tmpOutput, new Condition.Adapter<Integer>() {
			public boolean eval(int n) {
				return n > 0;
			}
		});
		List<Tuple> runs = new ArrayList<Tuple>(); //will be tuples of (startIdx, runLength)
		Arrays.sort(nz);
		int[] run = new int[] { nz[0], 1 };
		int i = 1;
		while(i < nz.length) {
			if(nz[i] == run[0] + run[1]) {
				run[1] += 1;
			}else{
				runs.add(new Tuple(2, run[0], run[1]));
				run = new int[] { nz[i], 1 };
			}
			i += 1;
		}
		runs.add(new Tuple(2, run[0], run[1]));
		
		// If we have a periodic encoder, merge the first and last run if they
	    // both go all the way to the edges
		if(c.isPeriodic() && runs.size() > 1) {
			int l = runs.size() - 1;
			if(((Integer)runs.get(0).get(0)) == 0 && ((Integer)runs.get(l).get(0)) + ((Integer)runs.get(l).get(1)) == c.getN()) {
				runs.set(l, new Tuple(2, 
					(Integer)runs.get(l).get(0),  
						((Integer)runs.get(l).get(1)) + ((Integer)runs.get(0).get(1)) ));
				runs = runs.subList(1, runs.size());
			}
		}
		
		// ------------------------------------------------------------------------
	    // Now, for each group of 1's, determine the "left" and "right" edges, where
	    // the "left" edge is inset by halfwidth and the "right" edge is inset by
	    // halfwidth.
	    // For a group of width w or less, the "left" and "right" edge are both at
	    // the center position of the group.
		int left = 0;
		int right = 0;
		List<MinMax> ranges = new ArrayList<MinMax>();
		for(Tuple tupleRun : runs) {
			int start = (Integer)tupleRun.get(0);
			int runLen = (Integer)tupleRun.get(1);
			if(runLen <= c.getW()) {
				left = right = start + runLen / 2;
			}else{
				left = start + c.getHalfWidth();
				right = start + runLen - 1 - c.getHalfWidth();
			}
			
			double inMin, inMax;
			// Convert to input space.
			if(!c.isPeriodic()) {
				inMin = (left - c.getPadding()) * c.getResolution() + c.getMinVal();
				inMax = (right - c.getPadding()) * c.getResolution() + c.getMinVal();
			}else{
				inMin = (left - c.getPadding()) * c.getRange() / c.getNInternal() + c.getMinVal();
				inMax = (right - c.getPadding()) * c.getRange() / c.getNInternal() + c.getMinVal();
			}
			// Handle wrap-around if periodic
			if(c.isPeriodic()) {
				if(inMin >= c.getMaxVal()) {
					inMin -= c.getRange();
					inMax -= c.getRange();
				}
			}
			
			// Clip low end
			if(inMin < c.getMinVal()) {
				inMin = c.getMinVal();
			}
			if(inMax < c.getMinVal()) {
				inMax = c.getMinVal();
			}
			
			// If we have a periodic encoder, and the max is past the edge, break into
			// 	2 separate ranges
			if(c.isPeriodic() && inMax >= c.getMaxVal()) {
				ranges.add(new MinMax(inMin, c.getMaxVal()));
				ranges.add(new MinMax(c.getMinVal(), inMax - c.getRange()));
			}else{
				if(inMax > c.getMaxVal()) {
					inMax = c.getMaxVal();
				}
				if(inMin > c.getMaxVal()) {
					inMin = c.getMaxVal();
				}
				ranges.add(new MinMax(inMin, inMax));
			}
		}
		
		String desc = generateRangeDescription(ranges);
		String fieldName;
		// Return result
		if(!parentFieldName.isEmpty()) {
			fieldName = String.format("%s.%s", parentFieldName, c.getName());
		}else{
			fieldName = c.getName();
		}
		
		RangeList inner = new RangeList(ranges, desc);
		Map<String, RangeList> fieldsDict = new HashMap<String, RangeList>();
		fieldsDict.put(fieldName, inner);
		
		return new DecodeResult(fieldsDict, Arrays.asList(new String[] { fieldName }));
	}
	
	/**
	 * Generate description from a text description of the ranges
	 * 
	 * @param	ranges		A list of {@link MinMax}es.
	 */
	public String generateRangeDescription(List<MinMax> ranges) {
		StringBuilder desc = new StringBuilder();
		int numRanges = ranges.size();
		for(int i = 0;i < numRanges;i++) {
			if(ranges.get(i).min() != ranges.get(i).max()) {
				desc.append(String.format("%.2f-%.2f", ranges.get(i).min(), ranges.get(i).max()));
			}else{
				desc.append(String.format("%.2f", ranges.get(i).min()));
			}
			if(i < numRanges - 1) {
				desc.append(", ");
			}
		}
		return desc.toString();
	}
	
	/**
	 * Return the internal topDownMapping matrix used for handling the
     * bucketInfo() and topDownCompute() methods. This is a matrix, one row per
     * category (bucket) where each row contains the encoded output for that
     * category.
     * 
	 * @param c		the connections memory
	 * @return		the internal topDownMapping
	 */
	public SparseObjectMatrix<int[]> getTopDownMapping(Connections c) {
		
		if(c.getTopDownMapping() == null) {
			//The input scalar value corresponding to each possible output encoding
			if(c.isPeriodic()) {
				c.setTopDownValues(
					ArrayUtils.arange(c.getMinVal() + c.getResolution() / 2.0, 
						c.getMaxVal(), c.getResolution()));
			}else{
				//Number of values is (max-min)/resolutions
				c.setTopDownValues(
					ArrayUtils.arange(c.getMinVal(), c.getMaxVal() + c.getResolution() / 2.0, 
						c.getResolution()));
			}
		}
		
		//Each row represents an encoded output pattern
		int numCategories = c.getTopDownValues().length;
		SparseObjectMatrix<int[]> topDownMapping;
		c.setTopDownMapping(
			topDownMapping = new SparseObjectMatrix<int[]>(
				new int[] { numCategories }));
		
		double[] topDownValues = c.getTopDownValues();
		int[] outputSpace = new int[c.getN()];
		double minVal = c.getMinVal();
		double maxVal = c.getMaxVal();
		for(int i = 0;i < numCategories;i++) {
			double value = topDownValues[i];
			value = Math.max(value, minVal);
			value = Math.min(value, maxVal);
			encodeIntoArray(c, value, outputSpace);
			topDownMapping.set(i, Arrays.copyOf(outputSpace, outputSpace.length));
		}
		
		return topDownMapping;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @param c		the state memory
	 * @param <T>	the input value, in this case a double
	 * @return	a list of one input double
	 */
	@Override
	public <T> TDoubleList getScalars(Connections c, T d) {
		TDoubleList retVal = new TDoubleArrayList();
		retVal.add((Double)d);
		return retVal;
	}
	
	/**
	 * Returns a list of items, one for each bucket defined by this encoder.
     * Each item is the value assigned to that bucket, this is the same as the
     * EncoderResult.value that would be returned by getBucketInfo() for that
     * bucket and is in the same format as the input that would be passed to
     * encode().
	 * 
     * This call is faster than calling getBucketInfo() on each bucket individually
     * if all you need are the bucket values.
	 *
     * @return list of items, each item representing the bucket value for that
     *        bucket.
	 */
	public TDoubleList getBucketValues(Connections c) {
		TDoubleList bucketValues = null;
		if((bucketValues = c.getBucketValues()) == null) {
			SparseObjectMatrix<int[]> topDownMapping = c.getTopDownMapping();
			int numBuckets = topDownMapping.getMaxIndex() + 1;
			bucketValues = new TDoubleArrayList();
			for(int i = 0;i < numBuckets;i++) {
				bucketValues.add((Double)getBucketInfo(c, new int[] { i }).get(0).get(1));
			}
			c.setBucketValues(bucketValues);
		}
		return bucketValues;
	}
	
	public List<EncoderResult> getBucketInfo(Connections c, int[] buckets) {
		SparseObjectMatrix<int[]> topDownMapping = getTopDownMapping(c);
		
		//The "category" is simply the bucket index
		int category = buckets[0];
		int[] encoding = topDownMapping.getObject(category);
		
		//Which input value does this correspond to?
		double inputVal;
		if(c.isPeriodic()) {
			inputVal = c.getMinVal() + c.getResolution() / 2 + category * c.getResolution();
		}else{
			inputVal = c.getMinVal() + category * c.getResolution();
		}
		
		return Arrays.asList(
			new EncoderResult[] { 
				new EncoderResult(inputVal, inputVal, Arrays.toString(encoding)) });
			
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<EncoderResult> topDownCompute(Connections c, int[] encoded) {
		//Get/generate the topDown mapping table
		SparseObjectMatrix<int[]> topDownMapping = getTopDownMapping(c);
		
		// See which "category" we match the closest.
		int category = ArrayUtils.argmax(topDownMapping.rightVecProd(encoded));
		
		return getBucketInfo(c, new int[] { category });
	}
	
	/**
	 * Returns a list of {@link Tuple}s which in this case is a list of
	 * key value parameter values for this {@code ScalarEncoder}
	 * 
	 * @param c		the memory
	 * @return	a list of {@link Tuple}s
	 */
	public List<Tuple> dict(Connections c) {
		List<Tuple> l = new ArrayList<Tuple>();
		l.add(new Tuple(2, "maxval", c.getMaxVal()));
		l.add(new Tuple(2, "bucketValues", c.getBucketValues()));
		l.add(new Tuple(2, "nInternal", c.getNInternal()));
		l.add(new Tuple(2, "name", c.getName()));
		l.add(new Tuple(2, "minval", c.getMinVal()));
		l.add(new Tuple(2, "topDownValues", c.getTopDownValues()));
		l.add(new Tuple(2, "verbosity", c.getEncVerbosity()));
		l.add(new Tuple(2, "clipInput", c.clipInput()));
		l.add(new Tuple(2, "n", c.getN()));
		l.add(new Tuple(2, "padding", c.getPadding()));
		l.add(new Tuple(2, "range", c.getRange()));
		l.add(new Tuple(2, "periodic", c.isPeriodic()));
		l.add(new Tuple(2, "radius", c.getRadius()));
		l.add(new Tuple(2, "w", c.getW()));
		l.add(new Tuple(2, "topDownMappingM", c.getTopDownMapping()));
		l.add(new Tuple(2, "halfwidth", c.getHalfWidth()));
		l.add(new Tuple(2, "resolution", c.getResolution()));
		l.add(new Tuple(2, "rangeInternal", c.getRangeInternal()));
		
		return l;
	}
}
