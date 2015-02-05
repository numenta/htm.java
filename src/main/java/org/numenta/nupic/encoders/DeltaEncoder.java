/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, In  Unless you have an agreement
 * with Numenta, In, for a separate license for this software code, the
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeltaEncoder extends AdaptiveScalarEncoder {
	
	public double prevAbsolute = 0;
	public double prevDelta = 0;
	public boolean stateLock = false;

	/**
	 * 
	 */
	public DeltaEncoder() {
	}

	/**
	 * {@inheritDoc}
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#init()
	 */
	@Override
	public void init() {
		super.init();
	}

	/**
	 * {@inheritDoc}
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#initEncoder(int, double, double, int, double, double)
	 */
	@Override
	public void initEncoder(int w, double minVal, double maxVal, int n,
			double radius, double resolution) {
		super.initEncoder(w, minVal, maxVal, n, radius, resolution);
	}

	/**
	 * Returns a builder for building DeltaEncoder. This builder may be
	 * reused to produce multiple builders
	 * 
	 * @return a {@code DeltaEncoder.Builder}
	 */
	public static DeltaEncoder.Builder deltaBuilder() {
		return new DeltaEncoder.Builder();
	}

	public static class Builder extends Encoder.Builder<DeltaEncoder.Builder, DeltaEncoder> {
		private Builder() {}

		@Override
		public DeltaEncoder build() {
			encoder = new DeltaEncoder();
			super.build();
			((DeltaEncoder) encoder).init();
			return (DeltaEncoder) encoder;
		}
	}

	/**
	 * {@inheritDoc}
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#encodeIntoArray(java.lang.Double, int[])
	 */
	@Override
	public void encodeIntoArray(Double input, int[] output) {
		if (!(input instanceof Double)) {
			throw new IllegalArgumentException(
					String.format("Expected a Double input but got input of type %s", input.toString()));
		}
		double delta = 0;
		if (input == DeltaEncoder.SENTINEL_VALUE_FOR_MISSING_DATA) {
			output = new int[this.n];
			Arrays.fill(output, 0);
		} else {
			if (this.prevAbsolute == 0) {
				this.prevAbsolute = input;
			}
			delta = input - this.prevAbsolute;
			super.encodeIntoArray(input, output);
		}
		if (!this.stateLock) {
			this.prevAbsolute = input;
			this.prevDelta = delta;
		}
	}

	/**
	 * @return the stateLock
	 */
	public boolean isStateLock() {
		return stateLock;
	}

	/**
	 * @param stateLock the stateLock to set
	 */
	public void setStateLock(boolean stateLock) {
		this.stateLock = stateLock;
	}

	public void setFieldStats(String fieldName, String[] fieldParameters) {
		// TODO Auto-generated method stub
	}
	
	@Override
	public boolean isDelta() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#getBucketInfo(int[])
	 */
	@Override
	public List<EncoderResult> getBucketInfo(int[] buckets) {
		return super.getBucketInfo(buckets);
	}

	/**
	 * {@inheritDoc}
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#topDownCompute(int[])
	 */
	@Override
	public List<EncoderResult> topDownCompute(int[] encoded) {
		if (this.prevAbsolute == 0 || this.prevDelta == 0) {
			int[] initialBuckets = new int[this.n];
			Arrays.fill(initialBuckets, 0);
			List<EncoderResult> encoderResultList = new ArrayList<EncoderResult>();
			EncoderResult encoderResult = new EncoderResult(0, 0, initialBuckets);
			encoderResultList.add(encoderResult);
			return encoderResultList;
		}
		List<EncoderResult> erList = super.topDownCompute(encoded);
		if (this.prevAbsolute != 0) {
			double objVal = (double)(erList.get(0).getValue()) + this.prevAbsolute;
			double objScalar = erList.get(0).getScalar().doubleValue() + this.prevAbsolute;
			List<EncoderResult> encoderResultList = new ArrayList<EncoderResult>();
			EncoderResult encoderResult = new EncoderResult(objVal, objScalar, erList.get(0).getEncoding());
			encoderResultList.add(encoderResult);
			return encoderResultList;
		}
		return erList;
	}
}
