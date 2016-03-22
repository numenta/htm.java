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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Sparse Pass Through Encoder
 * Convert a bitmap encoded as array indices to an SDR
 * Each encoding is an SDR in which w out of n bits are turned on.
 * The input should be an array or string of indices to turn on
 * Note: the value for n must equal input length * w
 * i.e. for n=8 w=1 [0,2,5] => 101001000
 * or for n=8 w=1 "0,2,5" => 101001000
 * i.e. for n=24 w=3 [0,2,5] => 111000111000000111000000000
 * or for n=24 w=3 "0,2,5" => 111000111000000111000000000
 *
 * @author wilsondy (from Python original)
 */
public class SparsePassThroughEncoder extends PassThroughEncoder<int[]> {
    
    private static final long serialVersionUID = 1L;

    private SparsePassThroughEncoder() { super(); }

    private static final Logger LOGGER = LoggerFactory.getLogger(SparsePassThroughEncoder.class);

    public SparsePassThroughEncoder(int outputWidth, Integer outputBitsOnCount) {
        super(outputWidth, outputBitsOnCount);
        LOGGER.info("Building new SparsePassThroughEncoder instance, outputWidth: {} outputBitsOnCount: {}", outputWidth);
    }

    /**
     * Returns a builder for building SparsePassThroughEncoders.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code SparsePassThroughEncoder.Builder}
     */
    public static Encoder.Builder<SparsePassThroughEncoder.Builder, SparsePassThroughEncoder> sparseBuilder() {
        return new SparsePassThroughEncoder.Builder();
    }

    @Override
    /**
     * Convert the array of indices to a bit array and then pass to parent.
     */
    public void encodeIntoArray(int[] input, int[] output) {

        int[] denseInput = new int[output.length];
        for (int i : input) {
            if (i > denseInput.length)
                throw new IllegalArgumentException(String.format("Output bit count set too low, need at least %d bits", i));
            denseInput[i] = 1;
        }
        super.encodeIntoArray(denseInput, output);
        LOGGER.trace("Input: {} \nOutput: {} \n", Arrays.toString(input), Arrays.toString(output));
    }

    /**
     * Returns a {@link Encoder.Builder} for constructing {@link SparsePassThroughEncoder}s
     * <p/>
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses, while avoiding
     * the mistake-proneness of extremely long argument lists.
     */
    public static class Builder extends Encoder.Builder<SparsePassThroughEncoder.Builder, SparsePassThroughEncoder> {
        private Builder() {}

        @Override
        public SparsePassThroughEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new SparsePassThroughEncoder();

            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////

            ((SparsePassThroughEncoder) encoder).init();

            return (SparsePassThroughEncoder) encoder;
        }
    }
}
