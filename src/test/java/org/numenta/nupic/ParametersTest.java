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

package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Connections;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

public class ParametersTest {
    private Parameters parameters;

    @Test
    public void testEquals() {
        Parameters p1 = Parameters.empty();
        Parameters p2 = Parameters.empty();
        assertEquals(p1, p2);

        // Positive Number
        p1.set(KEY.POTENTIAL_PCT, 32.0);
        p2.set(KEY.POTENTIAL_PCT, 32.0);
        assertEquals(p1, p2);

        // Negative Number
        p1.set(KEY.POTENTIAL_PCT, 32.0);
        p2.set(KEY.POTENTIAL_PCT, 32.2);
        assertNotEquals(p1, p2);
        p2.set(KEY.POTENTIAL_PCT, 32.0); // reset

        // Positive int[]
        p1.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p2.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        assertEquals(p1, p2);

        // Negative int[]
        p1.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        p2.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2049 });
        assertNotEquals(p1, p2);
        p2.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 }); // reset

        // Positive Field Encodings Map
        Map<String, Map<String, Object>> map = getHotGymFieldEncodingMap();
        p1.set(KEY.FIELD_ENCODING_MAP, map);
        p2.set(KEY.FIELD_ENCODING_MAP, map);
        assertEquals(p1, p2);
        
        // Negative Field Encodings Map - vary N
        Map<String, Map<String, Object>> map2 = getHotGymFieldEncodingMap_varyN();
        p1.set(KEY.FIELD_ENCODING_MAP, map);
        p2.set(KEY.FIELD_ENCODING_MAP, map2);
        assertNotEquals(p1, p2);
        
        // Negative Field Encodings Map - vary inner Tuple value
        map2 = getHotGymFieldEncodingMap_varyDateFieldTupleValue();
        p1.set(KEY.FIELD_ENCODING_MAP, map);
        p2.set(KEY.FIELD_ENCODING_MAP, map2);
        assertNotEquals(p1, p2);
        
        // Negative Field Encodings Map - vary Date Field Key
        map2 = getHotGymFieldEncodingMap_varyDateFieldKey();
        p1.set(KEY.FIELD_ENCODING_MAP, map);
        p2.set(KEY.FIELD_ENCODING_MAP, map2);
        assertNotEquals(p1, p2);
        
        // Re-assert if changed back that it passes
        p1.set(KEY.FIELD_ENCODING_MAP, map2);
        assertEquals(p1, p2);
    }

    @Test
    public void testApply() {
        DummyContainer dc = new DummyContainer();
        Parameters params = Parameters.getAllDefaultParameters();
        params.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        params.set(Parameters.KEY.POTENTIAL_PCT, 20.0);
        params.set(Parameters.KEY.CELLS_PER_COLUMN, null);
        params.apply(dc);
        assertTrue(Arrays.equals(new int[] { 2048 }, dc.getColumnDimensions()));
        assertEquals(20.0, dc.getPotentialPct(), 0);
    }

    @Test
    public void testDefaultsAndUpdates() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertEquals(params.get(Parameters.KEY.CELLS_PER_COLUMN), 32);
        assertEquals(params.get(Parameters.KEY.SEED), 42);
        assertEquals(true, ((Random)params.get(Parameters.KEY.RANDOM)).getClass().equals(MersenneTwister.class));
        System.out.println("All Defaults:\n" + Parameters.getAllDefaultParameters());
        System.out.println("Spatial Defaults:\n" + Parameters.getSpatialDefaultParameters());
        System.out.println("Temporal Defaults:\n" + Parameters.getTemporalDefaultParameters());
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.set(Parameters.KEY.INPUT_DIMENSIONS, new int[]{64, 64});
        parameters.set(Parameters.KEY.COLUMN_DIMENSIONS, new int[]{32, 32});
        parameters.set(Parameters.KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 0.02*64*64);
        System.out.println("Updated/Combined:\n" + parameters);

    }

    public static class DummyContainerBase {
        private int[] columnDimensions;

        public int[] getColumnDimensions() {
            return columnDimensions;
        }

        public void setColumnDimensions(int[] columnDimensions) {
            this.columnDimensions = columnDimensions;
        }
    }

    public static class DummyContainer extends DummyContainerBase {
        private double potentialPct = 0;

        public double getPotentialPct() {
            return potentialPct;
        }

        public void setPotentialPct(double potentialPct) {
            this.potentialPct = potentialPct;
        }
    }

    @Test
    public void testUnion() {
        Parameters params = Parameters.getAllDefaultParameters();
        Parameters arg = Parameters.getAllDefaultParameters();
        arg.set(KEY.CELLS_PER_COLUMN, 5);

        assertTrue((int)params.get(KEY.CELLS_PER_COLUMN) != 5);
        params.union(arg);
        assertTrue((int)params.get(KEY.CELLS_PER_COLUMN) == 5);
    }

    @Test
    public void testGetKeyByFieldName() {
        KEY expected = Parameters.KEY.POTENTIAL_PCT;
        assertEquals(expected, KEY.getKeyByFieldName("potentialPct"));

        assertFalse(expected.equals(KEY.getKeyByFieldName("random")));
    }

    @Test
    public void testGetMinMax() {
        KEY synPermActInc = KEY.SYN_PERM_ACTIVE_INC;
        assertEquals(0.0, synPermActInc.getMin());
        assertEquals(1.0, synPermActInc.getMax());
    }

    @Test
    public void testCheckRange() {
        Parameters params = Parameters.getAllDefaultParameters();

        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, 2.0);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Can not set Parameters Property 'synPermActiveInc' because of value '2.0' not in range. Range[0.0-1.0]", e.getMessage());
        }
        
        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, -0.6);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Can not set Parameters Property 'synPermActiveInc' because of value '-0.6' not in range. Range[0.0-1.0]", e.getMessage());
        }

        try {
            KEY.SYN_PERM_ACTIVE_INC.checkRange(null);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("checkRange argument can not be null", e.getMessage());
        }

        // Test catch type mismatch
        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, Boolean.TRUE);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Can not set Parameters Property 'synPermActiveInc' because of type mismatch. The required type is class java.lang.Double", e.getMessage());
        }
        
        // Check values _AT_ the min / max (should pass)
        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, 0.0);
            assertEquals(0.0, (double)params.get(KEY.SYN_PERM_ACTIVE_INC), 0.0);
        }catch(Exception e) {
            fail();
        }
        
        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, 1.0);
            assertEquals(1.0, (double)params.get(KEY.SYN_PERM_ACTIVE_INC), 0.0);
        }catch(Exception e) {
            fail();
        }

        // Positive test
        try {
            params.set(KEY.SYN_PERM_ACTIVE_INC, 0.8);
            assertEquals(0.8, (double)params.get(KEY.SYN_PERM_ACTIVE_INC), 0.0);
        }catch(Exception e) {
            fail();
        }

    }

    @Test
    public void testSize() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertEquals(48, params.size());
    }

    @Test
    public void testKeys() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertTrue(params.keys() != null && params.keys().size() == 48); 
    }

    @Test
    public void testClearParameter() {
        Parameters params = Parameters.getAllDefaultParameters();

        assertNotNull(params.get(KEY.SYN_PERM_ACTIVE_INC));

        params.clearParameter(KEY.SYN_PERM_ACTIVE_INC);

        assertNull(params.get(KEY.SYN_PERM_ACTIVE_INC));
    }

    @Test
    public void testLogDiff() {
        Parameters params = Parameters.getAllDefaultParameters();

        assertNotNull(params.get(KEY.SYN_PERM_ACTIVE_INC));

        Connections connections = new Connections();
        params.apply(connections);

        Parameters all = Parameters.getAllDefaultParameters();
        all.set(KEY.SYN_PERM_ACTIVE_INC, 0.9);

        boolean b = all.logDiff(connections);
        assertTrue(b);
        
        try {
            all.logDiff(null);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("cn Object is required and can not be null", e.getMessage());
        }
    }

    @Test
    public void testSetterMethods() {
        Parameters params = Parameters.getAllDefaultParameters();

        params.setCellsPerColumn(42);
        assertEquals(42, params.get(KEY.CELLS_PER_COLUMN));

        params.setActivationThreshold(42);
        assertEquals(42, params.get(KEY.ACTIVATION_THRESHOLD));

        params.setLearningRadius(42);
        assertEquals(42, params.get(KEY.LEARNING_RADIUS));

        params.setMinThreshold(42);
        assertEquals(42, params.get(KEY.MIN_THRESHOLD));

        params.setSeed(42);
        assertEquals(42, params.get(KEY.SEED));

        params.setInitialPermanence(0.82);
        assertEquals(0.82, params.get(KEY.INITIAL_PERMANENCE));

        params.setConnectedPermanence(0.82);
        assertEquals(0.82, params.get(KEY.CONNECTED_PERMANENCE));

        params.setPermanenceIncrement(0.11);
        assertEquals(0.11, params.get(KEY.PERMANENCE_INCREMENT));

        params.setPermanenceDecrement(0.11);
        assertEquals(0.11, params.get(KEY.PERMANENCE_DECREMENT));
        
        params.setMaxSegmentsPerCell(11);
        assertEquals(11, params.get(KEY.MAX_SEGMENTS_PER_CELL));
        
        params.setMaxSynapsesPerSegment(22);
        assertEquals(22, params.get(KEY.MAX_SYNAPSES_PER_SEGMENT));
        
        params.setMaxNewSynapseCount(32);
        assertEquals(32, params.get(KEY.MAX_NEW_SYNAPSE_COUNT));

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
    public static Map<String, Map<String, Object>> getHotGymFieldEncodingMap_varyN() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                        null,
                        20, // n
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
    public static Map<String, Map<String, Object>> getHotGymFieldEncodingMap_varyDateFieldTupleValue() {
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

        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 2.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");

        return fieldEncodings;
    }
    
    /**
     * Returns the Hot Gym encoder setup.
     * @return
     */
    public static Map<String, Map<String, Object>> getHotGymFieldEncodingMap_varyDateFieldKey() {
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

//        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");

        return fieldEncodings;
    }

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
}
