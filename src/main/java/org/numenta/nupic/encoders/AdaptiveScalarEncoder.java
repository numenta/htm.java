package org.numenta.nupic.encoders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is an implementation of the scalar encoder that adapts the min and
 * max of the scalar encoder dynamically. This is essential to the streaming
 * model of the online prediction framework.
 * 
 * Initialization of an adaptive encoder using resolution or radius is not
 * supported; it must be initialized with n. This n is kept constant while
 * the min and max of the encoder changes.
 * 
 * The adaptive encoder must be have periodic set to false.
 * 
 * The adaptive encoder may be initialized with a minval and maxval or with
 * `None` for each of these. In the latter case, the min and max are set as
 * the 1st and 99th percentile over a window of the past 100 records.
 * 
 * *Note:** the sliding window may record duplicates of the values in the
 * data set, and therefore does not reflect the statistical distribution of
 * the input data and may not be used to calculate the median, mean etc.
 */
public class AdaptiveScalarEncoder extends ScalarEncoder {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveScalarEncoder.class);

    public int recordNum = 0;
    public boolean learningEnabled = true;
    public Double[] slidingWindow = new Double[0];
    public int windowSize = 300;
    public Double bucketValues;

    /**
     * {@inheritDoc}
     *
     * @see org.numenta.nupic.encoders.ScalarEncoder#init()
     */
    @Override
    public void init() {
        this.setPeriodic(false);
        super.init();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.numenta.nupic.encoders.ScalarEncoder#initEncoder(int, double,
     * double, int, double, double)
     */
    @Override
    public void initEncoder(int w, double minVal, double maxVal, int n,
        double radius, double resolution) {
        this.encLearningEnabled = true;
        if(this.periodic) {
            throw new IllegalStateException(
                "Adaptive scalar encoder does not encode periodic inputs");
        }
        assert n != 0;
        super.initEncoder(w, minVal, maxVal, n, radius, resolution);
    }

    /**
     * Constructs a new {@code AdaptiveScalarEncoder}
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

    /**
     * Constructs a new {@link Builder} suitable for constructing
     * {@code AdaptiveScalarEncoder}s.
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> topDownCompute(int[] encoded) {
        if (this.getMinVal() == 0 || this.getMaxVal() == 0) {
            List<Encoding> res = new ArrayList<Encoding>();
            int[] enArray = new int[this.getN()];
            Arrays.fill(enArray, 0);
            Encoding ecResult = new Encoding(0, 0, enArray);
            res.add(ecResult);
            return res;
        }
        return super.topDownCompute(encoded);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeIntoArray(Double input, int[] output) {
        this.recordNum += 1;
        boolean learn = false;
        if (!this.encLearningEnabled) {
            learn = true;
        }
        if (input == AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA) {
            Arrays.fill(output, 0);
        } else if (!Double.isNaN(input)) {
            this.setMinAndMax(input, learn);
        }
        super.encodeIntoArray(input, output);
    }

    private void setMinAndMax(Double input, boolean learn) {
        if (slidingWindow.length >= windowSize) {
            slidingWindow = deleteItem(slidingWindow, 0);
        }
        slidingWindow = appendItem(slidingWindow, input);

        if (this.minVal == this.maxVal) {
            this.minVal = input;
            this.maxVal = input + 1;
            setEncoderParams();
        } else {
            Double[] sorted = Arrays.copyOf(slidingWindow, slidingWindow.length);
            Arrays.sort(sorted);
            double minOverWindow = sorted[0];
            double maxOverWindow = sorted[sorted.length - 1];
            if (minOverWindow < this.minVal) {
                LOGGER.trace("Input {}={} smaller than minVal {}. Adjusting minVal to {}",
                                this.name, input, this.minVal, minOverWindow);
                this.minVal = minOverWindow;
                setEncoderParams();
            }
            if (maxOverWindow > this.maxVal) {
                LOGGER.trace("Input {}={} greater than maxVal {}. Adjusting maxVal to {}",
                                this.name, input, this.minVal, minOverWindow);
                this.maxVal = maxOverWindow;
                setEncoderParams();
            }
        }
    }

    private void setEncoderParams() {
        this.rangeInternal = this.maxVal - this.minVal;
        this.resolution = this.rangeInternal / (this.n - this.w);
        this.radius = this.w * this.resolution;
        this.range = this.rangeInternal + this.resolution;
        this.nInternal = this.n - 2 * this.padding;
        this.bucketValues = null;
    }

    private Double[] appendItem(Double[] a, Double input) {
        a = Arrays.copyOf(a, a.length + 1);
        a[a.length - 1] = input;
        return a;
    }

    private Double[] deleteItem(Double[] a, int i) {
        a = Arrays.copyOfRange(a, 1, a.length - 1);
        return a;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getBucketIndices(String inputString) {
        double input = Double.parseDouble(inputString);
        return calculateBucketIndices(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] getBucketIndices(double input) {
        return calculateBucketIndices(input);
    }

    private int[] calculateBucketIndices(double input) {
        this.recordNum += 1;
        boolean learn = false;
        if (!this.encLearningEnabled) {
            learn = true;
        }
        if ((Double.isNaN(input)) && (Double.valueOf(input) instanceof Double)) {
            input = AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA;
        }
        if (input == AdaptiveScalarEncoder.SENTINEL_VALUE_FOR_MISSING_DATA) {
            return new int[this.n];
        } else {
            this.setMinAndMax(input, learn);
        }
        return super.getBucketIndices(input);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Encoding> getBucketInfo(int[] buckets) {
        if (this.minVal == 0 || this.maxVal == 0) {
            int[] initialBuckets = new int[this.n];
            Arrays.fill(initialBuckets, 0);
            List<Encoding> encoderResultList = new ArrayList<Encoding>();
            Encoding encoderResult = new Encoding(0, 0, initialBuckets);
            encoderResultList.add(encoderResult);
            return encoderResultList;
        }
        return super.getBucketInfo(buckets);
    }
}
