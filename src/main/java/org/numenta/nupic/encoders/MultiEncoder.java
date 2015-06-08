/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
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

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.util.Tuple;

/**
 * A MultiEncoder encodes a dictionary or object with
 * multiple components. A MultiEncode contains a number
 * of sub-encoders, each of which encodes a separate component.
 *
 * @see Encoder
 * @see EncoderResult
 * @see Parameters
 *
 * @author wlmiller
 */
public class MultiEncoder extends Encoder<Object> {
    protected TIntObjectMap<String> indexToCategory = new TIntObjectHashMap<String>();

    protected List<Tuple> categoryList;

    protected int width;
    
    protected static final String CATEGORY_DELIMITER = ";";

    /**
     * Constructs a new {@code MultiEncoder}
     */
    private MultiEncoder() {}

    /**
     * Returns a builder for building MultiEncoders.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code MultiEncoder.Builder}
     */
    public static Encoder.Builder<MultiEncoder.Builder, MultiEncoder> builder() {
        return new MultiEncoder.Builder();
    }

    public void init() {
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void setFieldStats(String fieldName, Map<String, Double> fieldStatistics) {
        for (EncoderTuple t : getEncoders(this)) {
            String name = t.getName();
            Encoder encoder = t.getEncoder();
            encoder.setFieldStats(name, fieldStatistics);
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void encodeIntoArray(Object input, int[] output) {
        for (EncoderTuple t : getEncoders(this)) {
            String name = t.getName();
            Encoder encoder = t.getEncoder();
            int offset = t.getOffset();

            int[] tempArray = new int[encoder.getWidth()];

            try {
                encoder.encodeIntoArray(getInputValue(input, name), tempArray);
            }catch(Exception e) {
                System.out.println("input = " + input + ", name = " + name + ",  inputValue = " + getInputValue(input, name));
                e.printStackTrace();
                System.exit(1);
            }

            System.arraycopy(tempArray, 0, output, offset, tempArray.length);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public int[] encodeField(String fieldName, Object value) {
        for (EncoderTuple t : getEncoders(this)) {
            String name = t.getName();
            Encoder encoder = t.getEncoder();

            if (name.equals(fieldName)) {
                return encoder.encode(value);
            }
        }
        return new int[]{};
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public List<int[]> encodeEachField(Object input) {
        List<int[]> encodings = new ArrayList<int[]>();

        for (EncoderTuple t : getEncoders(this)) {
            String name = t.getName();
            Encoder encoder = t.getEncoder();

            encodings.add(encoder.encode(getInputValue(input, name)));
        }

        return encodings;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addEncoder(String name, Encoder child) {
        super.addEncoder(this, name, child, width);

        for (Object d : child.getDescription()) {
            Tuple dT = (Tuple) d;
            description.add(new Tuple(dT.get(0), (int)dT.get(1) + getWidth()));
        }
        width += child.getWidth();
    }

    @SuppressWarnings("rawtypes")
    public void addMultipleEncoders(Map<String, Map<String, Object>> fieldEncodings) {
        // Sort the encoders so that they end up in a controlled order
        List<String> sortedFields = new ArrayList<String>(fieldEncodings.keySet());
        Collections.sort(sortedFields);

        for (String field : sortedFields) {
            Map<String, Object> params = fieldEncodings.get(field);

            if (!params.containsKey("fieldName")) {
                throw new IllegalArgumentException("Missing fieldname for encoder " + field);
            }
            String fieldName = (String) params.get("fieldName");

            if (!params.containsKey("encoderType")) {
                throw new IllegalArgumentException("Missing type for encoder " + field);
            }

            String encoderType = (String) params.get("encoderType");
            Encoder.Builder builder = getBuilder(encoderType);
            for (String param : params.keySet()) {
                if (!param.equals("fieldName") && !param.equals("encoderType") &&
                    !param.equals("fieldType") && !param.equals("fieldEncodings")) {

                    setValue(builder, param, params.get(param));
                }
            }

            Encoder encoder = (Encoder)builder.build();
            this.addEncoder(fieldName, encoder);
        }
    }

    /**
     * Open up for internal Network API use.
     * Returns an {@link Encoder.Builder} which corresponds to the specified name.
     * @param encoderName
     * @return
     */
    public Encoder.Builder<?,?> getBuilder(String encoderName) {
        switch(encoderName) {
            case "CategoryEncoder":
                return CategoryEncoder.builder();
            case "CoordinateEncoder":
                return CoordinateEncoder.builder();
            case "GeospatialCoordinateEncoder":
                return GeospatialCoordinateEncoder.geobuilder();
            case "LogEncoder":
                return LogEncoder.builder();
            case "PassThroughEncoder":
                return PassThroughEncoder.builder();
            case "ScalarEncoder":
                return ScalarEncoder.builder();
            case "SparsePassThroughEncoder":
                return SparsePassThroughEncoder.sparseBuilder();
            case "SDRCategoryEncoder":
                return SDRCategoryEncoder.builder();
            case "RandomDistributedScalarEncoder":
                return RandomDistributedScalarEncoder.builder();
            case "DateEncoder":
                return DateEncoder.builder();
            case "DeltaEncoder":
                return DeltaEncoder.deltaBuilder();
            default:
                throw new IllegalArgumentException("Invalid encoder: " + encoderName);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void setValue(Encoder.Builder builder, String param, Object value)  {
        switch(param) {
            case "n":
                builder.n((int) value);
                break;
            case "w":
                builder.w((int) value);
                break;
            case "minVal":
                builder.minVal((double) value);
                break;
            case "maxVal":
                builder.maxVal((double) value);
                break;
            case "radius":
                builder.radius((double) value);
                break;
            case "resolution":
                builder.resolution((double) value);
                break;
            case "periodic":
                builder.periodic((boolean) value);
                break;
            case "clipInput":
                builder.clipInput((boolean) value);
                break;
            case "forced":
                builder.forced((boolean) value);
                break;
            case "name":
                builder.name((String) value);
                break;
            case "categoryList":
                if(value instanceof String) {
                    String strVal = (String)value;
                    if(strVal.indexOf(CATEGORY_DELIMITER) == -1) {
                        throw new IllegalArgumentException("Category field not delimited with '" + CATEGORY_DELIMITER + "' character.");
                    }
                    value = Arrays.<String>asList(strVal.split("[\\s]*\\" + CATEGORY_DELIMITER + "[\\s]*"));
                }
                if(builder instanceof CategoryEncoder.Builder) {
                    ((CategoryEncoder.Builder) builder).categoryList((List<String>) value);
                }else{
                    ((SDRCategoryEncoder.Builder) builder).categoryList((List<String>) value);
                }
                
                break;
            default:
                throw new IllegalArgumentException("Invalid parameter: " + param);
        }
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getN() {
        return width;
    }

    @Override
    public int getW() {
        return width;
    }

    @Override
    public String getName() {
        if (name == null) return "";
        else return name;
    }

    @Override
    public boolean isDelta() {
        return false;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setLearning(boolean learningEnabled) {
        for (EncoderTuple t : getEncoders(this)) {
            Encoder encoder = t.getEncoder();
            encoder.setLearningEnabled(learningEnabled);
        }
    }

    @Override
    public <S> List<S> getBucketValues(Class<S> returnType) {
        return null;
    }

    /**
     * Returns a {@link EncoderBuilder} for constructing {@link MultiEncoder}s
     *
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses, while avoiding
     * the mistake-proneness of extremely long argument lists.
     *
     */
    public static class Builder extends Encoder.Builder<MultiEncoder.Builder, MultiEncoder> {
        private Builder() {}

        @Override
        public MultiEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new MultiEncoder();

            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////

            //Call init
            ((MultiEncoder)encoder).init();

            return (MultiEncoder)encoder;
        }
    }
}
