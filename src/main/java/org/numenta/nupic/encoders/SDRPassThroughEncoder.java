/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2015, Numenta, Inc.  Unless you have an agreement
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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SDRPassThroughEncoder extends PassThroughEncoder<int[]> {
    private static final long serialVersionUID = 1L;
    
    protected final Logger LOGGER = LoggerFactory.getLogger(SDRPassThroughEncoder.class);
    
    protected SDRPassThroughEncoder() { }

    public SDRPassThroughEncoder(int outputWidth, Integer outputBitsOnCount) {
        super(outputWidth, outputBitsOnCount);
        
        LOGGER.info("Building new SDRPassThroughEncoder overriding instance, outputWidth: {} outputBitsOnCount: {}", outputWidth, outputBitsOnCount);
    }
    
    /**
     * Returns a builder for building SDRPassThroughEncoders.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code SDRPassThroughEncoder.Builder}
     */
    public static Encoder.Builder<SDRPassThroughEncoder.Builder, SDRPassThroughEncoder> sptBuilder() {
        return new SDRPassThroughEncoder.Builder();
    }
    
    /**
     * Check for length the same and copy input into output
     * If outputBitsOnCount (w) set, throw error if not true
     * @param <T>
     *
     * @param input
     * @param output
     */
    @Override
    public void encodeIntoArray(int[] input, int[] output) {
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("encodeIntoArray: input: {} \nOutput: {} ", Arrays.toString(input), Arrays.toString(output));
        }
        
        System.arraycopy(input, 0, output, 0, output.length);
    }
    
    /**
     * Returns a {@link Encoder.Builder} for constructing {@link SDRPassThroughEncoder}s
     * <p/>
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses, while avoiding
     * the mistake-proneness of extremely long argument lists.
     */
    public static class Builder extends Encoder.Builder<SDRPassThroughEncoder.Builder, SDRPassThroughEncoder> {
        private Builder() { }

        @Override
        public SDRPassThroughEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new SDRPassThroughEncoder();
            this.w = this.n;
            
            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////

            ((SDRPassThroughEncoder) encoder).init();

            return (SDRPassThroughEncoder) encoder;
        }
    }
}
