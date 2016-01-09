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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.KNNClassifier;
import org.numenta.nupic.util.MersenneTwister;

public class ParametersTest {
    private Parameters parameters;

    @Test
    public void testApply() {
        DummyContainer dc = new DummyContainer();
        Parameters params = Parameters.getAllDefaultParameters();
        params.setParameterByKey(Parameters.KEY.COLUMN_DIMENSIONS, new int[] { 2048 });
        params.setParameterByKey(Parameters.KEY.POTENTIAL_PCT, 20.0);
        params.setParameterByKey(Parameters.KEY.CELLS_PER_COLUMN, null);
        params.apply(dc);
        assertTrue(Arrays.equals(new int[] { 2048 }, dc.getColumnDimensions()));
        assertEquals(20.0, dc.getPotentialPct(), 0);
    }

    @Test
    public void testDefaultsAndUpdates() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertEquals(params.getParameterByKey(Parameters.KEY.CELLS_PER_COLUMN), 32);
        assertEquals(params.getParameterByKey(Parameters.KEY.SEED), 42);
        assertEquals(true, ((Random)params.getParameterByKey(Parameters.KEY.RANDOM)).getClass().equals(MersenneTwister.class));
        System.out.println("All Defaults:\n" + Parameters.getAllDefaultParameters());
        System.out.println("Spatial Defaults:\n" + Parameters.getSpatialDefaultParameters());
        System.out.println("Temporal Defaults:\n" + Parameters.getTemporalDefaultParameters());
        parameters = Parameters.getSpatialDefaultParameters();
        parameters.setParameterByKey(Parameters.KEY.INPUT_DIMENSIONS, new int[]{64, 64});
        parameters.setParameterByKey(Parameters.KEY.COLUMN_DIMENSIONS, new int[]{32, 32});
        parameters.setParameterByKey(Parameters.KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 0.02*64*64);
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
    public void testKNNEnumAndConstantFields() {
        Parameters params = Parameters.getKNNDefaultParameters();
        KNNClassifier knn = KNNClassifier.builder().apply(params);
        try {
            params.apply(knn);
            assertTrue(knn.getNumSVDDims() == null);
            assertTrue(knn.getDistanceMethod() == DistanceMethod.NORM); // the default
        }catch(Exception e) {
            fail();
        }
        
        params = Parameters.getKNNDefaultParameters();
        params.setParameterByKey(KEY.NUM_SVD_DIMS, Constants.KNN.ADAPTIVE);
        params.setParameterByKey(KEY.DISTANCE_METHOD, DistanceMethod.PCT_INPUT_OVERLAP);
        knn = KNNClassifier.builder().apply(params);
        try {
            params.apply(knn);
            assertTrue(knn.getNumSVDDims() == Constants.KNN.ADAPTIVE);
            assertTrue(knn.getDistanceMethod() == DistanceMethod.PCT_INPUT_OVERLAP);
        }catch(Exception e) {
            fail();
        }
    }
    
    @Test
    public void testUnion() {
        Parameters params = Parameters.getAllDefaultParameters();
        Parameters arg = Parameters.getAllDefaultParameters();
        arg.setParameterByKey(KEY.CELLS_PER_COLUMN, 5);
        
        assertTrue((int)params.getParameterByKey(KEY.CELLS_PER_COLUMN) != 5);
        params.union(arg);
        assertTrue((int)params.getParameterByKey(KEY.CELLS_PER_COLUMN) == 5);
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
            params.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 2.0);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Can not set Parameters Property 'synPermActiveInc' because of value '2.0' not in range. Range[0.0-1.0]", e.getMessage());
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
            params.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, Boolean.TRUE);
            fail();
        }catch(Exception e) {
            assertEquals(e.getClass(), IllegalArgumentException.class);
            assertEquals("Can not set Parameters Property 'synPermActiveInc' because of type mismatch. The required type is class java.lang.Double", e.getMessage());
        }
        
        // Positive test
        try {
            params.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.8);
            assertEquals(0.8, (double)params.getParameterByKey(KEY.SYN_PERM_ACTIVE_INC), 0.0);
        }catch(Exception e) {
            
        }
        
    }
    
    @Test
    public void testSize() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertEquals(64, params.size());
    }
    
    @Test
    public void testKeys() {
        Parameters params = Parameters.getAllDefaultParameters();
        assertTrue(params.keys() != null && params.keys().size() == 64); 
    }
    
    @Test
    public void testClearParameter() {
        Parameters params = Parameters.getAllDefaultParameters();
        
        assertNotNull(params.getParameterByKey(KEY.SYN_PERM_ACTIVE_INC));
        
        params.clearParameter(KEY.SYN_PERM_ACTIVE_INC);
        
        assertNull(params.getParameterByKey(KEY.SYN_PERM_ACTIVE_INC));
    }
    
    @Test
    public void testLogDiff() {
        Parameters params = Parameters.getAllDefaultParameters();
        
        assertNotNull(params.getParameterByKey(KEY.SYN_PERM_ACTIVE_INC));
        
        Connections connections = new Connections();
        params.apply(connections);
          
        Parameters all = Parameters.getAllDefaultParameters();
        all.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.9);
        
        boolean b = all.logDiff(connections);
        assertTrue(b);
    }

    @Test
    public void testSetterMethods() {
        Parameters params = Parameters.getAllDefaultParameters();
        
        params.setCellsPerColumn(42);
        assertEquals(42, params.getParameterByKey(KEY.CELLS_PER_COLUMN));
        
        params.setActivationThreshold(42);
        assertEquals(42, params.getParameterByKey(KEY.ACTIVATION_THRESHOLD));
        
        params.setLearningRadius(42);
        assertEquals(42, params.getParameterByKey(KEY.LEARNING_RADIUS));
        
        params.setMinThreshold(42);
        assertEquals(42, params.getParameterByKey(KEY.MIN_THRESHOLD));
        
        params.setMaxNewSynapseCount(42);
        assertEquals(42, params.getParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT));
        
        params.setSeed(42);
        assertEquals(42, params.getParameterByKey(KEY.SEED));
        
        params.setInitialPermanence(0.82);
        assertEquals(0.82, params.getParameterByKey(KEY.INITIAL_PERMANENCE));
        
        params.setConnectedPermanence(0.82);
        assertEquals(0.82, params.getParameterByKey(KEY.CONNECTED_PERMANENCE));
        
        params.setPermanenceIncrement(0.11);
        assertEquals(0.11, params.getParameterByKey(KEY.PERMANENCE_INCREMENT));
        
        params.setPermanenceDecrement(0.11);
        assertEquals(0.11, params.getParameterByKey(KEY.PERMANENCE_DECREMENT));
        
    }
}
