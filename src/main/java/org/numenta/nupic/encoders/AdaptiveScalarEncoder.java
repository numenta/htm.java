package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdaptiveScalarEncoder extends ScalarEncoder {

	/*
	 * This is an implementation of the scalar encoder that adapts the min and
	 * max of the scalar encoder dynamically. This is essential to the streaming
	 * model of the online prediction framework.
	 * 
	 * Initialization of an adapive encoder using resolution or radius is not
	 * supported; it must be intitialized with n. This n is kept constant while
	 * the min and max of the encoder changes.
	 * 
	 * The adaptive encoder must be have periodic set to false.
	 * 
	 * The adaptive encoder may be initialized with a minval and maxval or with
	 * `None` for each of these. In the latter case, the min and max are set as
	 * the 1st and 99th percentile over a window of the past 100 records.
	 * 
	 * *Note:** the sliding window may record duplicates of the values in the
	 * dataset, and therefore does not reflect the statistical distribution of
	 * the input data and may not be used to calculate the median, mean etc.
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.numenta.nupic.encoders.ScalarEncoder#init()
	 */
	@Override
	public void init() {
		this.setPeriodic(false);
		super.init();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.numenta.nupic.encoders.ScalarEncoder#initEncoder(int, double,
	 * double, int, double, double)
	 */
	@Override
	public void initEncoder(int w, double minVal, double maxVal, int n,
			double radius, double resolution) {
		this.setPeriodic(false);
		this.encLearningEnabled = true;
		if (this.periodic) {
			throw new IllegalStateException(
					"Adaptive scalar encoder does not encode periodic inputs");
		}
		assert n != 0;
		super.initEncoder(w, minVal, maxVal, n, radius, resolution);
	}

	/**
	 *
	 */
	public AdaptiveScalarEncoder() {
	}

	/**
	 * Returns a builder for building AdaptiveScalarEncoder. This builder may be
	 * reused to produce multiple builders
	 * 
	 * @return a {@code AdaptiveScalarEncoder.Builder}
	 */
	public static AdaptiveScalarEncoder.Builder adaptiveBuilder() {
		return new AdaptiveScalarEncoder.Builder();
	}

	public static class Builder extends Encoder.Builder<AdaptiveScalarEncoder.Builder, AdaptiveScalarEncoder> {
		private Builder() {}

		@Override
		public AdaptiveScalarEncoder build() {
			encoder = new AdaptiveScalarEncoder();
			super.build();
			((AdaptiveScalarEncoder) encoder).init();
			return (AdaptiveScalarEncoder) encoder;
		}
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.ScalarEncoder#topDownCompute(int[])
	 */
	@Override
	public List<EncoderResult> topDownCompute(int[] encoded) {
		if (this.getMinVal() == 0 || this.getMaxVal() == 0) {
			List<EncoderResult> res = new ArrayList<EncoderResult>();
			int[] enArray = new int[this.getN()];
			Arrays.fill(enArray, 0);
			EncoderResult ecResult = new EncoderResult(0, 0, enArray);
			res.add(ecResult);
			return res;
		}
		return super.topDownCompute(encoded);
	}

}
