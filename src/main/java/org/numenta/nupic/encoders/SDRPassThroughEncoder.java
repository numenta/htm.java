package org.numenta.nupic.encoders;

import java.util.Arrays;

import org.numenta.nupic.FieldMetaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SDRPassThroughEncoder extends PassThroughEncoder<String> {
    protected final Logger LOGGER = LoggerFactory.getLogger(SDRPassThroughEncoder.class);
    
    private FieldMetaType metaType;
    
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
     * Returns either {@link FieldMetaType#SARR} or {@link FieldMetaType#DARR}
     * @return
     */
    public FieldMetaType getMetaType() {
        return metaType;
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
    public void encodeIntoArray(String t, int[] output) {
        if(metaType == null) {
            metaType = FieldMetaType.fromString(t);
            
            if(metaType != FieldMetaType.SARR && metaType != FieldMetaType.DARR) {
                LOGGER.error("SDRPassThroughEncoder only encodes types: SARR, or DARR");
                throw new IllegalArgumentException("SDRPassThroughEncoder only encodes types: SARR, or DARR");
            }
        }
        
        if(LOGGER.isTraceEnabled()) {
            LOGGER.trace("encodeIntoArray: metaType: {} \nOutput: {} ", metaType, Arrays.toString(output));
        }
        
        System.arraycopy(metaType.decodeType(t, this), 0, output, 0, output.length);
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
