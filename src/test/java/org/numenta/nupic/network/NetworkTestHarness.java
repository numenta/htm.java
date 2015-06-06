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
package org.numenta.nupic.network;

import java.util.HashMap;
import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
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
        p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        p.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.setParameterByKey(KEY.POTENTIAL_PCT, 0.8);
        p.setParameterByKey(KEY.SYN_PERM_CONNECTED,0.1);
        p.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        p.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.setParameterByKey(KEY.MAX_BOOST, 1.0);
        
        p.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.21);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.1);
        p.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.1);
        p.setParameterByKey(KEY.MIN_THRESHOLD, 9);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 12);
        
        p.setParameterByKey(KEY.CLIP_INPUT, true);
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }

    /**
     * Returns Encoder parameters and meta information for the "Hot Gym" encoder
     * @return
     */
    public static Parameters getHotGymTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = getHotGymFieldEncodingMap();

        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);

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
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);

        return p;
    }
    
    /**
     * Returns the default parameters used for the "dayOfWeek" encoder and algorithms.
     * @return
     */
    public static Parameters getParameters() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[] { 8 });
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 20 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 6);
        
        //SpatialPooler specific
        parameters.setParameterByKey(KEY.POTENTIAL_RADIUS, 12);//3
        parameters.setParameterByKey(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.setParameterByKey(KEY.GLOBAL_INHIBITIONS, false);
        parameters.setParameterByKey(KEY.LOCAL_AREA_DENSITY, -1.0);
        parameters.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.setParameterByKey(KEY.STIMULUS_THRESHOLD, 1.0);
        parameters.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.setParameterByKey(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.setParameterByKey(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
        parameters.setParameterByKey(KEY.DUTY_CYCLE_PERIOD, 10);
        parameters.setParameterByKey(KEY.MAX_BOOST, 10.0);
        parameters.setParameterByKey(KEY.SEED, 42);
        parameters.setParameterByKey(KEY.SP_VERBOSITY, 0);
        
        //Temporal Memory specific
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 5);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 6);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.05);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.05);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 4);
        
        return parameters;
    }
    
    
}
