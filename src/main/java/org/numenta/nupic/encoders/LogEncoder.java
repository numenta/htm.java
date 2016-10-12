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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * DOCUMENTATION TAKEN DIRECTLY FROM THE PYTHON VERSION:
 *
 * This class wraps the ScalarEncoder class.
 * A Log encoder represents a floating point value on a logarithmic scale.
 * valueToEncode = log10(input)
 *
 *   w -- number of bits to set in output
 *   minval -- minimum input value. must be greater than 0. Lower values are
 *             reset to this value
 *   maxval -- maximum input value (input is strictly less if periodic == True)
 *   periodic -- If true, then the input value "wraps around" such that minval =
 *             maxval For a periodic value, the input must be strictly less than
 *             maxval, otherwise maxval is a true upper bound.
 *
 *   Exactly one of n, radius, resolution must be set. "0" is a special
 *   value that means "not set".
 *   n -- number of bits in the representation (must be > w)
 *   radius -- inputs separated by more than this distance in log space will have
 *             non-overlapping representations
 *   resolution -- The minimum change in scaled value needed to produce a change
 *                 in encoding. This should be specified in log space. For
 *                 example, the scaled values 10 and 11 will be distinguishable
 *                 in the output. In terms of the original input values, this
 *                 means 10^1 (1) and 10^1.1 (1.25) will be distinguishable.
 *   name -- an optional string which will become part of the description
 *   clipInput -- if true, non-periodic inputs smaller than minval or greater
 *                 than maxval will be clipped to minval/maxval
 *   forced -- (default False), if True, skip some safety checks
 */
public class LogEncoder extends Encoder<Double> {

	private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LogEncoder.class);

	private ScalarEncoder encoder;
	private double minScaledValue, maxScaledValue;
	/**
	 * Constructs a new {@code LogEncoder}
	 */
	LogEncoder() {}

	/**
	 * Returns a builder for building LogEncoders.
	 * This builder may be reused to produce multiple builders
	 *
	 * @return a {@code LogEncoder.Builder}
	 */
	public static Encoder.Builder<LogEncoder.Builder, LogEncoder> builder() {
		return new LogEncoder.Builder();
	}

	/**
	 *   w -- number of bits to set in output
	 *   minval -- minimum input value. must be greater than 0. Lower values are
	 *             reset to this value
	 *   maxval -- maximum input value (input is strictly less if periodic == True)
	 *   periodic -- If true, then the input value "wraps around" such that minval =
	 *             maxval For a periodic value, the input must be strictly less than
	 *             maxval, otherwise maxval is a true upper bound.
	 *
	 *   Exactly one of n, radius, resolution must be set. "0" is a special
	 *   value that means "not set".
	 *   n -- number of bits in the representation (must be > w)
	 *   radius -- inputs separated by more than this distance in log space will have
	 *             non-overlapping representations
	 *   resolution -- The minimum change in scaled value needed to produce a change
	 *                 in encoding. This should be specified in log space. For
	 *                 example, the scaled values 10 and 11 will be distinguishable
	 *                 in the output. In terms of the original input values, this
	 *                 means 10^1 (1) and 10^1.1 (1.25) will be distinguishable.
	 *   name -- an optional string which will become part of the description
	 *   clipInput -- if true, non-periodic inputs smaller than minval or greater
	 *                 than maxval will be clipped to minval/maxval
	 *   forced -- (default False), if True, skip some safety checks
	 */
	public void init() {
		double lowLimit = 1e-07;

		// w defaults to 5
		if (getW() == 0) {
			setW(5);
		}

		// maxVal defaults to 10000.
		if (getMaxVal() == 0.0) {
			setMaxVal(10000.);
		}

		if (getMinVal() < lowLimit) {
			setMinVal(lowLimit);
		}

		if (getMinVal() >= getMaxVal()) {
			throw new IllegalStateException("Max val must be larger than min val or the lower limit " +
                       "for this encoder " + String.format("%.7f", lowLimit));
		}

		minScaledValue = Math.log10(getMinVal());
		maxScaledValue = Math.log10(getMaxVal());

		if(minScaledValue >= maxScaledValue) {
			throw new IllegalStateException("Max val must be larger, in log space, than min val.");
		}

		// There are three different ways of thinking about the representation. Handle
	    // each case here.
		encoder = ScalarEncoder.builder()
				.w(getW())
				.minVal(minScaledValue)
				.maxVal(maxScaledValue)
				.periodic(false)
				.n(getN())
				.radius(getRadius())
				.resolution(getResolution())
				.clipInput(clipInput())
				.forced(isForced())
				.name(getName())
				.build();

		setN(encoder.getN());
		setResolution(encoder.getResolution());
		setRadius(encoder.getRadius());
	}


	@Override
	public int getWidth() {
		return encoder.getWidth();
	}

	@Override
	public boolean isDelta() {
		return encoder.isDelta();
	}

	@Override
	public List<Tuple> getDescription() {
		return encoder.getDescription();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<FieldMetaType> getDecoderOutputFieldTypes() {
		return encoder.getDecoderOutputFieldTypes();
	}

	/**
	 * Convert the input, which is in normal space, into log space
	 * @param input Value in normal space.
	 * @return Value in log space.
	 */
	private Double getScaledValue(double input) {
		if(input == SENTINEL_VALUE_FOR_MISSING_DATA) {
			return null;
		} else {
			double val = input;
			if (val < getMinVal()) {
				val = getMinVal();
			} else if (val > getMaxVal()) {
				val = getMaxVal();
			}

			return Math.log10(val);
		}
	}

	/**
	 * Returns the bucket indices.
	 *
	 * @param	input
	 */
	@Override
	public int[] getBucketIndices(double input) {
		Double scaledVal = getScaledValue(input);

		if (scaledVal == null) {
			return new int[]{};
		} else {
			return encoder.getBucketIndices(scaledVal);
		}
	}

	/**
	 * Encodes inputData and puts the encoded value into the output array,
     * which is a 1-D array of length returned by {@link Connections#getW()}.
	 *
     * Note: The output array is reused, so clear it before updating it.
	 * @param inputData Data to encode. This should be validated by the encoder.
	 * @param output 1-D array of same length returned by {@link Connections#getW()}
     *
	 * @return
	 */
	@Override
	public void encodeIntoArray(Double input, int[] output) {
		Double scaledVal = getScaledValue(input);

		if (scaledVal == null) {
			Arrays.fill(output, 0);
		} else {
			encoder.encodeIntoArray(scaledVal, output);

			LOG.trace("input: " + input);
			LOG.trace(" scaledVal: " + scaledVal);
			LOG.trace(" output: " + Arrays.toString(output));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DecodeResult decode(int[] encoded, String parentFieldName) {
		// Get the scalar values from the underlying scalar encoder
		DecodeResult decodeResult = encoder.decode(encoded, parentFieldName);

		Map<String, RangeList> fields = decodeResult.getFields();

		if (fields.keySet().size() == 0) {
			return decodeResult;
		}

		// Convert each range into normal space
		RangeList inRanges = (RangeList) fields.values().toArray()[0];
		RangeList outRanges = new RangeList(new ArrayList<MinMax>(), "");
		for (MinMax minMax : inRanges.getRanges()) {
			MinMax scaledMinMax = new MinMax( Math.pow(10, minMax.min()),
											  Math.pow(10, minMax.max()));
			outRanges.add(scaledMinMax);
		}

		// Generate a text description of the ranges
		String desc = "";
		int numRanges = outRanges.size();
		for (int i = 0; i < numRanges; i++) {
			MinMax minMax = outRanges.getRange(i);
			if (minMax.min() != minMax.max()) {
				desc += String.format("%.2f-%.2f", minMax.min(), minMax.max());
			} else {
				desc += String.format("%.2f", minMax.min());
			}
			if (i < numRanges - 1) {
				desc += ", ";
			}
		}
		outRanges.setDescription(desc);

		String fieldName;
		if (!parentFieldName.equals("")) {
			fieldName = String.format("%s.%s", parentFieldName, getName());
		} else {
			fieldName = getName();
		}

		Map<String, RangeList> outFields = new HashMap<String, RangeList>();
		outFields.put(fieldName,  outRanges);

		List<String> fieldNames = new ArrayList<String>();
		fieldNames.add(fieldName);

		return new DecodeResult(outFields, fieldNames);
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <S> List<S> getBucketValues(Class<S> t) {
		// Need to re-create?
		if(bucketValues == null) {
			List<S> scaledValues = encoder.getBucketValues(t);
			bucketValues = new ArrayList<S>();

			for (S scaledValue : scaledValues) {
				double value = Math.pow(10, (Double)scaledValue);
				((List<Double>)bucketValues).add(value);
			}
		}
		return (List<S>)bucketValues;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Encoding> getBucketInfo(int[] buckets) {
		Encoding scaledResult = encoder.getBucketInfo(buckets).get(0);
		double scaledValue = (Double)scaledResult.getValue();
		double value = Math.pow(10, scaledValue);

		return Arrays.asList(new Encoding(value, value, scaledResult.getEncoding()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Encoding> topDownCompute(int[] encoded) {
		Encoding scaledResult = encoder.topDownCompute(encoded).get(0);
		double scaledValue = (Double)scaledResult.getValue();
		double value = Math.pow(10, scaledValue);

		return Arrays.asList(new Encoding(value, value, scaledResult.getEncoding()));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public TDoubleList closenessScores(TDoubleList expValues, TDoubleList actValues, boolean fractional) {
		TDoubleList retVal = new TDoubleArrayList();

		double expValue, actValue;
		if (expValues.get(0) > 0) {
			expValue = Math.log10(expValues.get(0));
		} else {
			expValue = minScaledValue;
		}
		if (actValues.get(0) > 0) {
			actValue = Math.log10(actValues.get(0));
		} else {
			actValue = minScaledValue;
		}

		double closeness;
		if (fractional) {
			double err = Math.abs(expValue - actValue);
			double pctErr = err / (maxScaledValue - minScaledValue);
			pctErr = Math.min(1.0,  pctErr);
			closeness = 1.0 - pctErr;
		} else {
			closeness = Math.abs(expValue - actValue);;
		}

		retVal.add(closeness);
		return retVal;
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
	public static class Builder extends Encoder.Builder<LogEncoder.Builder, LogEncoder> {
		private Builder() {}

		@Override
		public LogEncoder build() {
			//Must be instantiated so that super class can initialize
			//boilerplate variables.
			encoder = new LogEncoder();

			//Call super class here
			super.build();

			////////////////////////////////////////////////////////
			//  Implementing classes would do setting of specific //
			//  vars here together with any sanity checking       //
			////////////////////////////////////////////////////////
			
			try {
			    ((LogEncoder)encoder).init();
			}catch(Exception e) {
			    String msg = null;
			    int idx = -1;
			    if((idx = (msg = e.getMessage()).indexOf("ScalarEncoder")) != -1) {
			        msg = msg.substring(0, idx).concat("LogEncoder");
			    }
			    throw new IllegalStateException(msg);
			}

			return (LogEncoder)encoder;
		}
	}
}
