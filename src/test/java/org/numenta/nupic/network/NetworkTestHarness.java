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
package org.numenta.nupic.network;

import java.util.HashMap;
import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Classifier;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.util.Tuple;

/**
 * Encapsulates setup methods which are shared among various tests
 * in the {@link org.numenta.nupic.network} package.
 * 
 * @author cogmission
 */
public class NetworkTestHarness {
    /**
     * Sets up an Encoder Mapping of configurable values.
     *  
     * @param map               if called more than once to set up encoders for more
     *                          than one field, this should be the map itself returned
     *                          from the first call to {@code #setupMap(Map, int, int, double, 
     *                          double, double, double, Boolean, Boolean, Boolean, String, String, String)}
     * @param n                 the total number of bits in the output encoding
     * @param w                 the number of bits to use in the representation
     * @param min               the minimum value (if known i.e. for the ScalarEncoder)
     * @param max               the maximum value (if known i.e. for the ScalarEncoder)
     * @param radius            see {@link Encoder}
     * @param resolution        see {@link Encoder}
     * @param periodic          such as hours of the day or days of the week, which repeat in cycles
     * @param clip              whether the outliers should be clipped to the min and max values
     * @param forced            use the implied or explicitly stated ratio of w to n bits rather than the "suggested" number
     * @param fieldName         the name of the encoded field
     * @param fieldType         the data type of the field
     * @param encoderType       the Camel case class name minus the .class suffix
     * @return
     */
    public static Map<String, Map<String, Object>> setupMap(
            Map<String, Map<String, Object>> map,
            int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
            Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {

        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }

        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);

        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clipInput", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);

        return map;
    }

    /**
     * Returns the Hot Gym encoder setup.
     * @return
     */
    public static Map<String, Map<String, Object>> getHotGymFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                0, // n
                0, // w
                0, 0, 0, 0, null, null, null,
                "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
                fieldEncodings, 
                25, 
                3, 
                0, 0, 0, 0.1, null, null, null, 
                "consumption", "float", "RandomDistributedScalarEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        return fieldEncodings;
    }
    
    /**
     * Returns the Hot Gym encoder setup.
     * @return
     */
    public static Map<String, Map<String, Object>> getNetworkDemoFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                0, // n
                0, // w
                0, 0, 0, 0, null, null, null,
                "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
                fieldEncodings, 
                50, 
                21, 
                0, 100, 0, 0.1, null, Boolean.TRUE, null, 
                "consumption", "float", "ScalarEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(21,9.5)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        return fieldEncodings;
    }
    
    /**
     * Returns Encoder parameters and meta information for the "Hot Gym" encoder
     * @return
     */
    public static Parameters getNetworkDemoTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getNetworkDemoFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.GLOBAL_INHIBITION, true);
        p.set(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.set(KEY.CELLS_PER_COLUMN, 32);
        p.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.set(KEY.POTENTIAL_PCT, 0.8);
        p.set(KEY.SYN_PERM_CONNECTED,0.1);
        p.set(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        p.set(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.set(KEY.MAX_BOOST, 1.0);
        
        p.set(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        p.set(KEY.INITIAL_PERMANENCE, 0.21);
        p.set(KEY.PERMANENCE_INCREMENT, 0.1);
        p.set(KEY.PERMANENCE_DECREMENT, 0.1);
        p.set(KEY.MIN_THRESHOLD, 9);
        p.set(KEY.ACTIVATION_THRESHOLD, 12);
        
        p.set(KEY.CLIP_INPUT, true);
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }

    /**
     * Returns Encoder parameters and meta information for the "Hot Gym" encoder
     * @return
     */
    public static Parameters getHotGymTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getHotGymFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }
    
    /**
     * Parameters and meta information for the "dayOfWeek" encoder
     * @return
     */
    public static Map<String, Map<String, Object>> getDayDemoFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                null,
                8, // n
                3, // w
                0.0, 8.0, 0, 1, Boolean.TRUE, null, Boolean.TRUE,
                "dayOfWeek", "number", "ScalarEncoder");
        return fieldEncodings;
    }
    
    /**
     * Returns Encoder parameters for the "dayOfWeek" test encoder.
     * @return
     */
    public static Parameters getDayDemoTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getDayDemoFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }

    /**
     * @return a Map that can be used as the value for a Parameter
     * object's KEY.INFERRED_FIELDS key, to classify the specified
     * field with the specified Classifier type.
     */
    public static Map<String, Class<? extends Classifier>> getInferredFieldsMap(
            String field, Class<? extends Classifier> classifier) {
        Map<String, Class<? extends Classifier>> inferredFieldsMap = new HashMap<>();
        inferredFieldsMap.put(field, classifier);
        return inferredFieldsMap;
    }
    
    /**
     * Returns the default parameters used for the "dayOfWeek" encoder and algorithms.
     * @return
     */
    public static Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.set(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, -1);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, false);
        parameters.set(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.1);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.1);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.set(KEY.MAX_BOOST, 10.0);
        parameters.set(KEY.SEED, 42);
        
        //Temporal Memory specific
        parameters.set(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.set(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.set(KEY.MIN_THRESHOLD, 5);
        parameters.set(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.set(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.set(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.set(KEY.ACTIVATION_THRESHOLD, 4);
        
        return parameters;
    }
    
    /**
     * Parameters and meta information for the "Geospatial Test" encoder
     * @return
     */
    public static Map<String, Map<String, Object>> getGeospatialFieldEncodingMap() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(null, 0, 0, 0.0D, 0.0D, 0.0D, 0.0D, (Boolean)null, (Boolean)null, (Boolean)null, "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(fieldEncodings, 50, 21, 0.0D, 100.0D, 0.0D, 0.1D, (Boolean)null, Boolean.TRUE, (Boolean)null, "consumption", "float", "ScalarEncoder");
        fieldEncodings = setupMap(fieldEncodings, 999, 25, 0.0D, 100.0D, 0.0D, 0.1D, (Boolean)null, Boolean.TRUE, (Boolean)null, "location", "geo", "GeospatialCoordinateEncoder");
        
        fieldEncodings.get("timestamp").put(Parameters.KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(new Object[]{Integer.valueOf(21), Double.valueOf(9.5D)}));
        fieldEncodings.get("timestamp").put(Parameters.KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        fieldEncodings.get("location").put("timestep", "60");
        fieldEncodings.get("location").put("scale", "30");
        
        return fieldEncodings;
    }
    
    /**
     * Parameters and meta information for the "Geospatial Test" encoder
     * @return
     */
    public static Parameters getGeospatialTestEncoderParams() {
    	Map<String, Map<String, Object>> fieldEncodings = getGeospatialFieldEncodingMap();
    	
    	Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }
    
}
