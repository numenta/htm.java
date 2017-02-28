/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, In  Unless you have an agreement
 * with Numenta, In, for a separate license for this software code, the
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

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DOCUMENTATION TAKEN DIRECTLY FROM THE PYTHON VERSION:
 *
 * A scalar encoder encodes a numeric (floating point) value into an array
 * of bits. The output is 0's except for a contiguous block of 1's. The
 * location of this contiguous block varies continuously with the input value.
 *
 * The encoding is linear. If you want a nonlinear encoding, just transform
 * the scalar (e.g. by applying a logarithm function) before encoding.
 * It is not recommended to bin the data as a pre-processing step, e.g.
 * "1" = $0 - $.20, "2" = $.21-$0.80, "3" = $.81-$1.20, etc as this
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
 * of the output. Only one of these should be specified to the constructor:
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
 * monday midnight-> 01110000000000
 * tuesday noon -> 00111000000000
 * etc
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
public class ScalarEncoder extends Encoder<Double> {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ScalarEncoder.class);

    /**
     * Constructs a new {@code ScalarEncoder}
     */
    ScalarEncoder() {}

    /**
     * Returns a builder for building ScalarEncoders.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code ScalarEncoder.Builder}
     */
    public static Encoder.Builder<ScalarEncoder.Builder, ScalarEncoder> builder() {
        return new ScalarEncoder.Builder();
    }

    /**
     * Returns true if the underlying encoder works on deltas
     */
    @Override
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
     */
    public void init() {
        if(getW() % 2 == 0) {
            throw new IllegalStateException(
                "W must be an odd number (to eliminate centering difficulty)");
        }

        setHalfWidth((getW() - 1) / 2);

        // For non-periodic inputs, padding is the number of bits "outside" the range,
        // on each side. I.e. the representation of minval is centered on some bit, and
        // there are "padding" bits to the left of that centered bit; similarly with
        // bits to the right of the center bit of maxval
        setPadding(isPeriodic() ? 0 : getHalfWidth());

        if(!Double.isNaN(getMinVal()) && !Double.isNaN(getMaxVal())) {
            if(getMinVal() >= getMaxVal()) {
                throw new IllegalStateException("maxVal must be > minVal");
            }
            setRangeInternal(getMaxVal() - getMinVal());
        }

        // There are three different ways of thinking about the representation. Handle
        // each case here.
        initEncoder(getW(), getMinVal(), getMaxVal(), getN(), getRadius(), getResolution());

        //nInternal represents the output area excluding the possible padding on each side
        setNInternal(getN() - 2 * getPadding());

        if(getName() == null) {
            if((getMinVal() % ((int)getMinVal())) > 0 ||
                (getMaxVal() % ((int)getMaxVal())) > 0) {
                setName("[" + getMinVal() + ":" + getMaxVal() + "]");
            }else{
                setName("[" + (int)getMinVal() + ":" + (int)getMaxVal() + "]");
            }
        }

        //Checks for likely mistakes in encoder settings
        if(!isForced()) {
            checkReasonableSettings();
        }
        description.add(new Tuple((name = getName()).equals("None") ? "[" + (int)getMinVal() + ":" + (int)getMaxVal() + "]" : name, 0));
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
    public void initEncoder(int w, double minVal, double maxVal, int n, double radius, double resolution) {
        if(n != 0) {
            if(!Double.isNaN(minVal) && !Double.isNaN(maxVal)) {
                if(!isPeriodic()) {
                    setResolution(getRangeInternal() / (getN() - getW()));
                }else{
                    setResolution(getRangeInternal() / getN());
                }

                setRadius(getW() * getResolution());

                if(isPeriodic()) {
                    setRange(getRangeInternal());
                }else{
                    setRange(getRangeInternal() + getResolution());
                }
            }
        }else{
            if(radius != 0) {
                setResolution(getRadius() / w);
            }else if(resolution != 0) {
                setRadius(getResolution() * w);
            }else{
                throw new IllegalStateException(
                    "One of n, radius, resolution must be specified for a ScalarEncoder");
            }

            if(isPeriodic()) {
                setRange(getRangeInternal());
            }else{
                setRange(getRangeInternal() + getResolution());
            }

            double nFloat = w * (getRange() / getRadius()) + 2 * getPadding();
            setN((int)Math.ceil(nFloat));
        }
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
    public Integer getFirstOnBit(double input) {
        if(Double.isNaN(input)) {
            return null;
        }else{
            if(input < getMinVal()) {
                if(clipInput() && !isPeriodic()) {
                    if(LOGGER.isTraceEnabled()) {
                        LOGGER.info("Clipped input " + getName() + "=" + input + " to minval " + getMinVal());
                    }
                    input = getMinVal();
                }else{
                    throw new IllegalStateException("input (" + input +") less than range (" +
                        getMinVal() + " - " + getMaxVal() + ")");
                }
            }
        }

        if(isPeriodic()) {
            if(input >= getMaxVal()) {
                throw new IllegalStateException("input (" + input +") greater than periodic range (" +
                    getMinVal() + " - " + getMaxVal() + ")");
            }
        }else{
            if(input > getMaxVal()) {
                if(clipInput()) {
                    if(LOGGER.isTraceEnabled()) {
                        LOGGER.info("Clipped input " + getName() + "=" + input + " to maxval " + getMaxVal());
                    }
                    input = getMaxVal();
                }else{
                    throw new IllegalStateException("input (" + input +") greater than periodic range (" +
                        getMinVal() + " - " + getMaxVal() + ")");
                }
            }
        }

        int centerbin;
        if(isPeriodic()) {
            centerbin = ((int)((input - getMinVal()) *  getNInternal() / getRange())) + getPadding();
        }else{
            centerbin = ((int)(((input - getMinVal()) + getResolution()/2) / getResolution())) + getPadding();
        }

        return centerbin - getHalfWidth();
    }

    /**
     * Check if the settings are reasonable for the SpatialPooler to work
     * @param c
     */
    public void checkReasonableSettings() {
        if(getW() < 21) {
            throw new IllegalStateException(
                "Number of bits in the SDR (%d) must be greater than 2, and recommended >= 21 (use forced=True to override)");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<FieldMetaType> getDecoderOutputFieldTypes() {
        return new LinkedHashSet<>(Arrays.asList(FieldMetaType.FLOAT, FieldMetaType.INTEGER));
    }

    /**
     * Should return the output width, in bits.
     */
    @Override
    public int getWidth() {
        return getN();
    }

    /**
     * {@inheritDoc}
     * NO-OP
     */
    @Override
    public int[] getBucketIndices(String input) { return null; }

    /**
     * Returns the bucket indices.
     *
     * @param	input
     */
    @Override
    public int[] getBucketIndices(double input) {
        int minbin = getFirstOnBit(input);

        //For periodic encoders, the bucket index is the index of the center bit
        int bucketIdx;
        if(isPeriodic()) {
            bucketIdx = minbin + getHalfWidth();
            if(bucketIdx < 0) {
                bucketIdx += getN();
            }
        }else{//for non-periodic encoders, the bucket index is the index of the left bit
            bucketIdx = minbin;
        }

        return new int[] { bucketIdx };
    }

    /**
     * Encodes inputData and puts the encoded value into the output array,
     * which is a 1-D array of length returned by {@link Connections#getW()}.
     *
     * Note: The output array is reused, so clear it before updating it.
     * @param inputData Data to encode. This should be validated by the encoder.
     * @param output 1-D array of same length returned by {@link Connections#getW()}
     */
    @Override
    public void encodeIntoArray(Double input, int[] output) {
        if(Double.isNaN(input)) {
            Arrays.fill(output, 0);
            return;
        }

        Integer bucketVal = getFirstOnBit(input);
        if(bucketVal != null) {
            int bucketIdx = bucketVal;
            Arrays.fill(output, 0);
            int minbin = bucketIdx;
            int maxbin = minbin + 2*getHalfWidth();
            if(isPeriodic()) {
                if(maxbin >= getN()) {
                    int bottombins = maxbin - getN() + 1;
                    int[] range = ArrayUtils.range(0, bottombins);
                    ArrayUtils.setIndexesTo(output, range, 1);
                    maxbin = getN() - 1;
                }
                if(minbin < 0) {
                    int topbins = -minbin;
                    ArrayUtils.setIndexesTo(output, ArrayUtils.range(getN() - topbins, getN()), 1);
                    minbin = 0;
                }
            }

            ArrayUtils.setIndexesTo(output, ArrayUtils.range(minbin, maxbin + 1), 1);
        }

        // Added guard against immense string concatenation
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("");
            LOGGER.trace("input: " + input);
            LOGGER.trace("range: " + getMinVal() + " - " + getMaxVal());
            LOGGER.trace("n:" + getN() + "w:" + getW() + "resolution:" + getResolution() +
                            "radius:" + getRadius() + "periodic:" + isPeriodic());
            LOGGER.trace("output: " + Arrays.toString(output));
            LOGGER.trace("input desc: " + decode(output, ""));
        }
    }

    /**
     * Returns a {@link DecodeResult} which is a tuple of range names
     * and lists of {@link RangeLists} in the first entry, and a list
     * of descriptions for each range in the second entry.
     *
     * @param encoded			the encoded bit vector
     * @param parentFieldName	the field the vector corresponds with
     * @return
     */
    @Override
    public DecodeResult decode(int[] encoded, String parentFieldName) {
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
        int maxZerosInARow = getHalfWidth();
        for(int i = 0;i < maxZerosInARow;i++) {
            int[] searchStr = new int[i + 3];
            Arrays.fill(searchStr, 1);
            ArrayUtils.setRangeTo(searchStr, 1, -1, 0);
            int subLen = searchStr.length;

            // Does this search string appear in the output?
            if(isPeriodic()) {
                for(int j = 0;j < getN();j++) {
                    int[] outputIndices = ArrayUtils.range(j, j + subLen);
                    outputIndices = ArrayUtils.modulo(outputIndices, getN());
                    if(Arrays.equals(searchStr, ArrayUtils.sub(tmpOutput, outputIndices))) {
                        ArrayUtils.setIndexesTo(tmpOutput, outputIndices, 1);
                    }
                }
            }else{
                for(int j = 0;j < getN() - subLen + 1;j++) {
                    if(Arrays.equals(searchStr, ArrayUtils.sub(tmpOutput, ArrayUtils.range(j, j + subLen)))) {
                        ArrayUtils.setRangeTo(tmpOutput, j, j + subLen, 1);
                    }
                }
            }
        }

        LOGGER.trace("raw output:" + Arrays.toString(
                        ArrayUtils.sub(encoded, ArrayUtils.range(0, getN()))));
        LOGGER.trace("filtered output:" + Arrays.toString(tmpOutput));

        // ------------------------------------------------------------------------
        // Find each run of 1's.
        int[] nz = ArrayUtils.where(tmpOutput, new Condition.Adapter<Integer>() {
            @Override
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
                runs.add(new Tuple(run[0], run[1]));
                run = new int[] { nz[i], 1 };
            }
            i += 1;
        }
        runs.add(new Tuple(run[0], run[1]));

        // If we have a periodic encoder, merge the first and last run if they
        // both go all the way to the edges
        if(isPeriodic() && runs.size() > 1) {
            int l = runs.size() - 1;
            if(((Integer)runs.get(0).get(0)) == 0 && ((Integer)runs.get(l).get(0)) + ((Integer)runs.get(l).get(1)) == getN()) {
                runs.set(l, new Tuple((Integer)runs.get(l).get(0),
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
            if(runLen <= getW()) {
                left = right = start + runLen / 2;
            }else{
                left = start + getHalfWidth();
                right = start + runLen - 1 - getHalfWidth();
            }

            double inMin, inMax;
            // Convert to input space.
            if(!isPeriodic()) {
                inMin = (left - getPadding()) * getResolution() + getMinVal();
                inMax = (right - getPadding()) * getResolution() + getMinVal();
            }else{
                inMin = (left - getPadding()) * getRange() / getNInternal() + getMinVal();
                inMax = (right - getPadding()) * getRange() / getNInternal() + getMinVal();
            }
            // Handle wrap-around if periodic
            if(isPeriodic()) {
                if(inMin >= getMaxVal()) {
                    inMin -= getRange();
                    inMax -= getRange();
                }
            }

            // Clip low end
            if(inMin < getMinVal()) {
                inMin = getMinVal();
            }
            if(inMax < getMinVal()) {
                inMax = getMinVal();
            }

            // If we have a periodic encoder, and the max is past the edge, break into
            // 	2 separate ranges
            if(isPeriodic() && inMax >= getMaxVal()) {
                ranges.add(new MinMax(inMin, getMaxVal()));
                ranges.add(new MinMax(getMinVal(), inMax - getRange()));
            }else{
                if(inMax > getMaxVal()) {
                    inMax = getMaxVal();
                }
                if(inMin > getMaxVal()) {
                    inMin = getMaxVal();
                }
                ranges.add(new MinMax(inMin, inMax));
            }
        }

        String desc = generateRangeDescription(ranges);
        String fieldName;
        // Return result
        if(parentFieldName != null && !parentFieldName.isEmpty()) {
            fieldName = String.format("%s.%s", parentFieldName, getName());
        }else{
            fieldName = getName();
        }

        RangeList inner = new RangeList(ranges, desc);
        Map<String, RangeList> fieldsDict = new HashMap<String, RangeList>();
        fieldsDict.put(fieldName, inner);

        return new DecodeResult(fieldsDict, Arrays.asList(fieldName));
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
    public SparseObjectMatrix<int[]> getTopDownMapping() {

        if(topDownMapping == null) {
            //The input scalar value corresponding to each possible output encoding
            if(isPeriodic()) {
                setTopDownValues(
                    ArrayUtils.arange(getMinVal() + getResolution() / 2.0,
                        getMaxVal(), getResolution()));
            }else{
                //Number of values is (max-min)/resolutions
                setTopDownValues(
                    ArrayUtils.arange(getMinVal(), getMaxVal() + getResolution() / 2.0,
                        getResolution()));
            }
        }

        //Each row represents an encoded output pattern
        int numCategories = getTopDownValues().length;
        SparseObjectMatrix<int[]> topDownMapping;
        setTopDownMapping(
            topDownMapping = new SparseObjectMatrix<int[]>(
                new int[] { numCategories }));

        double[] topDownValues = getTopDownValues();
        int[] outputSpace = new int[getN()];
        double minVal = getMinVal();
        double maxVal = getMaxVal();
        for(int i = 0;i < numCategories;i++) {
            double value = topDownValues[i];
            value = Math.max(value, minVal);
            value = Math.min(value, maxVal);
            encodeIntoArray(value, outputSpace);
            topDownMapping.set(i, Arrays.copyOf(outputSpace, outputSpace.length));
        }

        return topDownMapping;
    }

    /**
     * {@inheritDoc}
     *
     * @param <S>	the input value, in this case a double
     * @return	a list of one input double
     */
    @Override
    public <S> TDoubleList getScalars(S d) {
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
     * @param	returnType 		class type parameter so that this method can return encoder
     * 							specific value types
     *
     * @return list of items, each item representing the bucket value for that
     *        bucket.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <S> List<S> getBucketValues(Class<S> t) {
        if(bucketValues == null) {
            SparseObjectMatrix<int[]> topDownMapping = getTopDownMapping();
            int numBuckets = topDownMapping.getMaxIndex() + 1;
            bucketValues = new ArrayList<Double>();
            for(int i = 0;i < numBuckets;i++) {
                ((List<Double>)bucketValues).add((Double)getBucketInfo(new int[] { i }).get(0).get(1));
            }
        }
        return (List<S>)bucketValues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> getBucketInfo(int[] buckets) {
        SparseObjectMatrix<int[]> topDownMapping = getTopDownMapping();

        //The "category" is simply the bucket index
        int category = buckets[0];
        int[] encoding = topDownMapping.getObject(category);

        //Which input value does this correspond to?
        double inputVal;
        if(isPeriodic()) {
            inputVal = getMinVal() + getResolution() / 2 + category * getResolution();
        }else{
            inputVal = getMinVal() + category * getResolution();
        }

        return Arrays.asList(new Encoding(inputVal, inputVal, encoding));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> topDownCompute(int[] encoded) {
        //Get/generate the topDown mapping table
        SparseObjectMatrix<int[]> topDownMapping = getTopDownMapping();

        // See which "category" we match the closest.
        int category = ArrayUtils.argmax(rightVecProd(topDownMapping, encoded));

        return getBucketInfo(new int[]{category});
    }

    /**
     * Returns a list of {@link Tuple}s which in this case is a list of
     * key value parameter values for this {@code ScalarEncoder}
     *
     * @return	a list of {@link Tuple}s
     */
    public List<Tuple> dict() {
        List<Tuple> l = new ArrayList<Tuple>();
        l.add(new Tuple("maxval", getMaxVal()));
        l.add(new Tuple("bucketValues", getBucketValues(Double.class)));
        l.add(new Tuple("nInternal", getNInternal()));
        l.add(new Tuple("name", getName()));
        l.add(new Tuple("minval", getMinVal()));
        l.add(new Tuple("topDownValues", Arrays.toString(getTopDownValues())));
        l.add(new Tuple("clipInput", clipInput()));
        l.add(new Tuple("n", getN()));
        l.add(new Tuple("padding", getPadding()));
        l.add(new Tuple("range", getRange()));
        l.add(new Tuple("periodic", isPeriodic()));
        l.add(new Tuple("radius", getRadius()));
        l.add(new Tuple("w", getW()));
        l.add(new Tuple("topDownMappingM", getTopDownMapping()));
        l.add(new Tuple("halfwidth", getHalfWidth()));
        l.add(new Tuple("resolution", getResolution()));
        l.add(new Tuple("rangeInternal", getRangeInternal()));

        return l;
    }

    /**
     * Returns a {@link EncoderBuilder} for constructing {@link ScalarEncoder}s
     *
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses, while avoiding
     * the mistake-proneness of extremely long argument lists.
     *
     * @see ScalarEncoder.Builder#setStuff(int)
     */
    public static class Builder extends Encoder.Builder<ScalarEncoder.Builder, ScalarEncoder> {
        private Builder() {}

        @Override
        public ScalarEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new ScalarEncoder();

            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////

            ((ScalarEncoder)encoder).init();

            return (ScalarEncoder)encoder;
        }
    }
}
