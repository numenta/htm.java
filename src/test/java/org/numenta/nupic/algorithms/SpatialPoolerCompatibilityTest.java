/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.util.UniversalRandom;

/**
 * Tests the {@link SpatialPooler} output of the Java SP against the 
 * generated activations, permanences and connected state of the Python
 * SP.
 * 
 * This is accomplished by use of the {@link UniversalRandom} RNG in both the 
 * Python and Java versions, to produce Python output which is then stored as
 * a Java class file which can return the Python SP's state at any given 
 * processing cycle.
 * 
 * @author cogmission
 * @see SpatialPoolerCompatibilityActives
 * @see SpatialPoolerCompatibilityPermanences
 * @see UniversalRandom
 */
public class SpatialPoolerCompatibilityTest {

    /**
     * Test the Java output against stored Python output allowing "learning"
     * to be randomly turned on and off. Uses the {@link UniversalRandom} RNG
     * in both the Python and Java versions to enable identical output generation.
     */
    @Test
    public void testCompatability1() {
        int numRecords = 100;
        
        Tuple tuple = createSP();
        Connections c = (Connections)tuple.get(0);
        SpatialPooler sp = (SpatialPooler)tuple.get(1);
        
        int[][] potentials = c.getPotentials();
        int[][] connecteds = c. getConnecteds();
        double[][] perms = c.getPermanences();
        
        int[][] expectedInitialPotentials = getPythonInitialPotentialMapping1();
        int[][] expectedInitialColumnConnects = getPythonInitialColumnConnections();
        double[][] expectedInitialPerms = getPythonInitialPermanences1();
        
        assertTrue(
            IntStream.range(0, potentials.length)
                .allMatch(i -> Arrays.equals(potentials[i], expectedInitialPotentials[i])));
        assertTrue(
            IntStream.range(0, potentials.length)
                .allMatch(i -> Arrays.equals(connecteds[i], expectedInitialColumnConnects[i])));
        assertTrue(
            IntStream.range(0, perms.length)
                .allMatch(d -> Arrays.equals(perms[d], expectedInitialPerms[d])));
        
        
        // Set the percentage of 1's in the test inputs
        double sparsity = 0.4d;
        int[][] inputMatrix = new UniversalRandom(42).binDistrib(numRecords, c.getNumInputs(), sparsity);
        int[][] pythonInputMatrix = getPythonInputs1();
        assertTrue(
            IntStream.range(0, numRecords)
                .allMatch(i -> Arrays.equals(inputMatrix[i], pythonInputMatrix[i])));
        
        runSideBySide(sp, c, pythonInputMatrix, numRecords, new SpatialPoolerCompatibilityActives(), new SpatialPoolerCompatibilityPermanences());
    }
    
    /**
     * Main loop of the test which calls compute and then compares the {@link SpatialPooler}'s
     * state to the stored test from the Python version of the same test.
     * @param sp                the SpatialPooler
     * @param c                 The {@link Connections} object
     * @param inputs            the test input data
     * @param numRecords        the number of iterations to run
     * @param actives           the Class storing the Python activations
     * @param perms             the Class storing the Python permanences
     */
    private void runSideBySide(SpatialPooler sp, Connections c, int[][] inputs, int numRecords,
        SpatialPoolerCompatibilityActives actives, SpatialPoolerCompatibilityPermanences perms) {
        
        UniversalRandom univ = new UniversalRandom(42);
                        
        int[] activeArray = new int[c.getNumColumns()];
        for(int i = 0;i < numRecords;i++) {
            boolean learn = univ.nextDouble() > 0.5;
            
            sp.compute(c, inputs[i], activeArray, learn);
            //System.out.println("i: " + Arrays.toString(inputs[i]));
            //System.out.println("v: " + Arrays.toString(activeArray));
            //printConnecteds(c);
            //printPermanences(c);
            assertTrue(Arrays.equals(actives.getActivations()[i], activeArray));
            
            try {
                comparePermanences(c, perms, i);
                compareActivations(actives, activeArray, i);
            }catch(Exception e) {
                System.out.println("failed for record #: " + i);
                fail();
            }
            
            if(i % 10 == 0) {
                //System.out.println("\n---------------------------- converting ------------------------------");
                convertPermanences(c, sp, perms, i);
                //System.out.println("---------------------------- converted  ------------------------------");
                //return;
            }
           
        }
    }
    
    /**
     * Prints the connected column mapping to standard out
     * @param c
     */
    @SuppressWarnings("unused")
    private void printConnecteds(Connections c) {
        int[][] cons = c.getConnecteds();
        for(int i = 0;i < cons.length;i++) {
            System.out.println("c: " + Arrays.toString(cons[i]));
        }
    }
    
    /**
     * Prints the each columns synapse permanences
     * @param c
     */
    @SuppressWarnings("unused")
    private void printPermanences(Connections c) {
        double[][] perms = c.getPermanences();
        for(int i = 0;i < perms.length;i++) {
            System.out.println(Arrays.toString(perms[i]));
        }
    }
    
    /**
     * Compares the stored python spatial pooler permanences against those
     * generated by the java {@link SpatialPooler} 
     * 
     * @param c             the {@link Connections} object
     * @param compats       the class containing stored python output
     * @param iteration     the current iteration num
     */
    private void comparePermanences(Connections c, SpatialPoolerCompatibilityPermanences compats, int iteration) {
        double[][] comp = getPermsForIteration(compats, iteration);
        double[][] actual = c.getPermanences();
        //System.out.println("\nComparing permanences for iteration: " + iteration);
        for(int i = 0;i < comp.length;i++) {
            //System.out.println("tested for comp: " + Arrays.toString(comp[i]) + "  actual: " + Arrays.toString(actual[i]) + " -- col = " + i);
            for(int j = 0;j < comp[i].length;j++) {
                try {
                    assertEquals(comp[i][j], actual[i][j], 0.0001);
                }catch(AssertionError e) {
                    System.out.println("failed for comp: " + Arrays.toString(comp[i]) + "  actual: " + Arrays.toString(actual[i]));
                    throw e;
                }
            }
        }
    }
    
    /**
     * Compares the stored python column activations against the 
     * column activations of the java {@link SpatialPooler}
     * 
     * @param actives           the stored activations generated from the python compatibility test
     * @param activations       the column activations for the current cycle.       
     * @param iteration         the current iteration num
     */
    private void compareActivations(SpatialPoolerCompatibilityActives actives, int[] activations, int iteration) {
        int[] pythonActivations = actives.getActivations()[iteration];
        try {
            assertTrue(Arrays.equals(pythonActivations, activations));
        }catch(AssertionError e) {
            System.out.println("compareActivations failed for iteration: " + iteration);
        }
    }
    
    /**
     * Transfer the permanences from source to dest SP's. This is used in test
     * routines to counteract some drift between implementations.
     * We assume the two SP's have identical configurations/parameters.
     * 
     * @param c
     * @param sp
     * @param compats
     * @param iteration
     */
    private void convertPermanences(Connections c, SpatialPooler sp, SpatialPoolerCompatibilityPermanences compats, int iteration) {
        double[][] pythonSpPerms = getPermsForIteration(compats, iteration);
        for(int i = 0;i < c.getNumColumns();i++) {
            double[] perms = pythonSpPerms[i];
            Column col = c.getColumn(i);
            col.setProximalPermanences(c, perms);
        }
        
        comparePermanences(c, compats, iteration);
    }
    
    /**
     * Returns the array of persisted Python SP column synapse permanences 
     * @param compats       the generated class which returns the permanences
     * @param iteration     the current iteration
     * @return  a 2 dim array containing each column's synapse permanences
     */
    private double[][] getPermsForIteration(SpatialPoolerCompatibilityPermanences compats, int iteration) {
        double[][] comp = null;
        try {
            Method getPermanences = SpatialPoolerCompatibilityPermanences.class.getMethod(
                "getPermanences" + iteration, (Class[])null);
            comp = (double[][])getPermanences.invoke(compats, (Object[])null);           
        } catch(Exception e) {
            e.printStackTrace();
        } 
        
        return comp;
    }
    
    /**
     * Creates a {@link SpatialPooler} with predetermined parameters.
     * @return
     */
    private Tuple createSP() {
        Parameters parameters = Parameters.getAllDefaultParameters();
        parameters.set(KEY.INPUT_DIMENSIONS, new int[] { 4, 4 });
        parameters.set(KEY.COLUMN_DIMENSIONS, new int[] { 5, 3 });
        parameters.set(KEY.CELLS_PER_COLUMN, 1);
        parameters.set(KEY.RANDOM, new UniversalRandom(42));

        //SpatialPooler specific
        parameters.set(KEY.POTENTIAL_RADIUS, 20);//3
        parameters.set(KEY.POTENTIAL_PCT, 0.5);//0.5
        parameters.set(KEY.GLOBAL_INHIBITION, true);
        parameters.set(KEY.LOCAL_AREA_DENSITY, 0.0);
        parameters.set(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 5.0);
        parameters.set(KEY.STIMULUS_THRESHOLD, 0.0);
        parameters.set(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
        parameters.set(KEY.SYN_PERM_ACTIVE_INC, 0.1);
        parameters.set(KEY.SYN_PERM_TRIM_THRESHOLD, 0.05);
        parameters.set(KEY.SYN_PERM_CONNECTED, 0.1);
        parameters.set(KEY.MIN_PCT_OVERLAP_DUTY_CYCLES, 0.001);
        parameters.set(KEY.MIN_PCT_ACTIVE_DUTY_CYCLES, 0.001);
        parameters.set(KEY.DUTY_CYCLE_PERIOD, 30);
        parameters.set(KEY.MAX_BOOST, 10.0);
        
        Connections conn = new Connections();
        parameters.apply(conn);
        
        SpatialPooler sp = new SpatialPooler();
        sp.init(conn);
        
        return new Tuple(conn, sp);
    }
    
    
    //////////////////////////////////////////////////////
    //           Stored Initial State of Python SP      //
    //////////////////////////////////////////////////////
    /**
     * Returns the initial column connections considered to be "connected"
     * to the input vector bits in that columns pool.
     * @return
     */
    private int[][] getPythonInitialColumnConnections() {
        return new int[][] {
            { 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0 },
            { 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0 },
            { 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1 },
            { 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0 },
            { 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0 },
            { 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0 },
            { 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0 },
            { 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 },
            { 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0 },
            { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 },
            { 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1 }
        };
    }
    
    /**
     * Returns the mapping of which inputs belong to each {@link Column}'s
     * {@link Pool}.
     * @return  a 2 dim array where a "1" indicates membership
     */
    private int[][] getPythonInitialPotentialMapping1() {
        return new int[][] {
            { 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0 },
            { 0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0 },
            { 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 0 },
            { 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1 },
            { 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0 },
            { 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0 },
            { 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0, 1, 0 },
            { 1, 0, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1, 1 },
            { 0, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 0, 0 },
            { 0, 0, 1, 0, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1, 0, 0 },
            { 1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1 },
            { 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 1, 1 },
            { 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 0 },
            { 1, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1 },
            { 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1 }
        };
   }
    
   /**
    * The initial permanences before any processing has occurred.
    * @return
    */
   private double[][] getPythonInitialPermanences1() {
       return new double[][] {
           { 0.0, 0.0, 0.74377, 0.0, 0.0, 0.06271, 0.0623, 0.81379, 0.0, 0.0, 0.51139, 0.99208, 0.0, 0.08018, 0.0, 0.0 },
           { 0.0, 0.68185, 0.05791, 0.11953, 0.0, 0.5941, 0.0, 0.0, 0.0, 0.33391, 0.0, 0.0, 0.05948, 0.0, 0.45874, 0.0 },
           { 0.0, 0.0, 0.0, 0.56431, 0.0, 0.66772, 0.83539, 0.95185, 0.81082, 0.07339, 0.0638, 0.0, 0.0, 0.0, 0.0, 0.0 },
           { 0.0, 0.0, 0.0, 0.0, 0.0, 0.05192, 0.46153, 0.0, 0.0, 0.0, 0.55891, 0.7111, 0.0, 0.0, 0.54064, 0.94915 },
           { 0.09988, 0.0, 0.08008, 0.0, 0.828, 0.66268, 0.0, 0.35614, 0.96175, 0.24013, 0.0, 0.0, 0.75304, 0.0, 0.0, 0.0 },
           { 0.0, 0.0, 0.06743, 0.0, 0.0, 0.0, 0.49294, 0.06845, 0.0, 0.51796, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 },
           { 0.0, 0.79075, 0.0, 0.0, 0.61381, 0.0, 0.08392, 0.0, 0.61768, 0.0, 0.0, 0.59041, 0.43822, 0.0, 0.0, 0.0 },
           { 0.34363, 0.0, 0.0, 0.0, 0.0, 0.05401, 0.6598, 0.13402, 0.07946, 0.0, 0.0, 0.07423, 0.0, 0.0, 0.35227, 0.08265 },
           { 0.0, 0.64513, 0.06734, 0.0, 0.0, 0.0, 0.0, 0.06107, 0.16858, 0.06727, 0.0, 0.0, 0.35164, 0.07435, 0.0, 0.0 },
           { 0.0, 0.0, 0.27271, 0.0, 0.90487, 0.0, 0.67375, 0.0, 0.07149, 0.19324, 0.0, 0.09281, 0.0, 0.37126, 0.0, 0.0 },
           { 0.0, 0.30159, 0.84898, 0.0, 0.18082, 0.0, 0.0, 0.0, 0.05324, 0.0, 0.80101, 0.0, 0.07576, 0.0, 0.0, 0.8974 },
           { 0.62569, 0.0, 0.0, 0.0, 0.0, 0.46846, 0.0, 0.80119, 0.0, 0.0, 0.0, 0.07477, 0.0, 0.0, 0.09469, 0.0 },
           { 0.0, 0.0, 0.06282, 0.09819, 0.0, 0.0, 0.79606, 0.0, 0.0, 0.21277, 0.0, 0.0, 0.0, 0.52515, 0.0, 0.0 },
           { 0.48619, 0.45531, 0.0, 0.0, 0.0, 0.0, 0.0, 0.08895, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.09234 },
           { 0.2368, 0.0, 0.0, 0.0, 0.99505, 0.33265, 0.0, 0.0, 0.0, 0.0, 0.0, 0.33742, 0.07621, 0.0, 0.0, 0.20548 }
       };
   }
   
   /**
    * The randomly generated inputs for the test copied over from the Python 
    * test.
    * @return
    */
   private int[][] getPythonInputs1() {
       return new int[][] {
           { 1, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1 },
           { 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1 },
           { 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0 },
           { 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1 },
           { 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0 },
           { 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 1, 0 },
           { 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1 },
           { 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1 },
           { 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0 },
           { 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1, 0 },
           { 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1 },
           { 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0 },
           { 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1 },
           { 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0 },
           { 0, 0, 1, 0, 0, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0 },
           { 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0 },
           { 1, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0 },
           { 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0 },
           { 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0, 0, 0 },
           { 0, 0, 1, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0 },
           { 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0 },
           { 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0 },
           { 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 0 },
           { 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1 },
           { 0, 0, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1 },
           { 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0, 1 },
           { 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 1 },
           { 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1 },
           { 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1 },
           { 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1 },
           { 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1 },
           { 1, 1, 1, 0, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0 },
           { 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0 },
           { 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1 },
           { 1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1 },
           { 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1 },
           { 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1 },
           { 1, 0, 0, 1, 0, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 1 },
           { 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1 },
           { 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0 },
           { 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1 },
           { 0, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0 },
           { 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0 },
           { 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0 },
           { 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0 },
           { 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 1 },
           { 0, 0, 1, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0 },
           { 0, 1, 0, 0, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0 },
           { 1, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1 },
           { 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 0, 0 },
           { 1, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 0 },
           { 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0 },
           { 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0 },
           { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0 },
           { 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0 },
           { 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0 },
           { 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 },
           { 0, 1, 0, 0, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1 },
           { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1 },
           { 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0 },
           { 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0 },
           { 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1 },
           { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0 },
           { 1, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0 },
           { 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1 },
           { 1, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 1, 1, 1 },
           { 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1 },
           { 1, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0 },
           { 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1, 0, 0 },
           { 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0 },
           { 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1 },
           { 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1 },
           { 1, 1, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0 },
           { 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 0, 1 },
           { 0, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1 },
           { 0, 0, 1, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 1 },
           { 0, 0, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0 },
           { 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0 },
           { 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1 },
           { 1, 0, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1 },
           { 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 0, 0 },
           { 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 1 },
           { 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 1 },
           { 0, 0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 1, 0, 0, 0, 0 },
           { 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 1, 0 },
           { 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0 },
           { 0, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0 },
           { 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1 },
           { 0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0 },
           { 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 1 },
           { 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0 },
           { 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 0, 1 },
           { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1, 0, 0, 0, 1 },
           { 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 1, 1 },
           { 1, 0, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0 },
           { 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0 },
           { 0, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 0, 0 },
           { 0, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 1, 0, 0, 0, 1 },
           { 0, 0, 0, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1 },
           { 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1 }
       };
   }
   
}
