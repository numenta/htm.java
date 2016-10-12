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
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.Connections;
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

        /////////////////////////////////////////////////////////////////////////////
        // Used to write the 3 needed outputs to be inserted in the initialization //
        // part of the SP compatibility test                                       //
        /////////////////////////////////////////////////////////////////////////////
//        for(int[] pots : potentials) {
//            System.out.println(Arrays.toString(pots));
//        }
//        System.out.println("\n\n");
//        for(int[] conns : connecteds) {
//            System.out.println(Arrays.toString(conns));
//        }
//        System.out.println("\n\n");
//        for(double[] perm : perms) {
//            System.out.println(Arrays.toString(perm));
//        }
        
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
                .allMatch(d -> {
                    return IntStream.range(0, perms[d].length).allMatch(i -> {
                        return areEqualDouble(perms[d][i], expectedInitialPerms[d][i], 4); 
                    });
                }));
            
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
     * Used to specify the number of places after the decimal for which
     * the comparison should be made.
     * 
     * @param a             the first double to compare
     * @param b             the second double to compare
     * @param precision     the number of places after the decimal point
     * @return  a flag indicating whether the two specified doubles are equal
     *          at the specified precision.
     */
    public static boolean areEqualDouble(double a, double b, int precision) {
        return Math.abs(a - b) <= Math.pow(10, -precision);
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
        for(int i = 0;i < comp.length;i++) {
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
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 0},
            {1, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 1, 0, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1},
            {0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1},
            {0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0},
            {0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0},
            {0, 1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0},
            {1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {1, 1, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0},
            {0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0},
            {0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1},
            {0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
            {1, 0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1}
        };
    }
    
    /**
     * Returns the mapping of which inputs belong to each {@link Column}'s
     * {@link Pool}.
     * @return  a 2 dim array where a "1" indicates membership
     */
    private int[][] getPythonInitialPotentialMapping1() {
        return new int[][] {
            {1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 0, 1, 0, 0},
            {1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1, 0},
            {0, 0, 1, 0, 1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0},
            {0, 0, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1},
            {0, 0, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1},
            {0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 0},
            {1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0},
            {0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1},
            {1, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1, 1, 0},
            {1, 1, 0, 1, 1, 1, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0},
            {1, 0, 1, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0, 0, 1, 0},
            {0, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 0},
            {1, 0, 1, 0, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 1, 1},
            {0, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1},
            {1, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1, 1, 0, 0, 0, 1}
        };
   }
    
   /**
    * The initial permanences before any processing has occurred.
    * @return
    */
   private double[][] getPythonInitialPermanences1() {
       return new double[][] {
           {0.0, 0.0, 0.74377, 0.0, 0.0, 0.062710002, 0.0623, 0.81379002, 0.0, 0.0, 0.51138997, 0.99207997, 0.0, 0.080179997, 0.0, 0.0},
           {0.68185002, 0.057909999, 0.0, 0.11953, 0.0, 0.0, 0.0, 0.5941, 0.33390999, 0.0, 0.0, 0.0, 0.05948, 0.0, 0.45874, 0.0},
           {0.0, 0.0, 0.56431001, 0.0, 0.66772002, 0.83538997, 0.95185, 0.0, 0.81081998, 0.07339, 0.0, 0.0638, 0.0, 0.0, 0.0, 0.0},
           {0.0, 0.0, 0.0, 0.05192, 0.0, 0.46153, 0.0, 0.0, 0.0, 0.0, 0.55891001, 0.0, 0.0, 0.71109998, 0.54064, 0.94915003},
           {0.0, 0.0, 0.099880002, 0.0, 0.080080003, 0.0, 0.82801002, 0.0, 0.0, 0.66267997, 0.35613999, 0.96174997, 0.0, 0.0, 0.24013001, 0.75304002},
           {0.0, 0.0, 0.0, 0.0, 0.0, 0.067429997, 0.0, 0.0, 0.0, 0.49294001, 0.068449996, 0.0, 0.51796001, 0.0, 0.0, 0.0},
           {0.0, 0.0, 0.0, 0.79075003, 0.61381, 0.0, 0.083920002, 0.0, 0.61768001, 0.59040999, 0.0, 0.0, 0.43821999, 0.0, 0.0, 0.0},
           {0.0, 0.34362999, 0.05401, 0.0, 0.65979999, 0.13402, 0.0, 0.0, 0.0, 0.0, 0.079460002, 0.0, 0.07423, 0.35227001, 0.0, 0.082649998},
           {0.64512998, 0.0, 0.0, 0.067340001, 0.0, 0.0, 0.0, 0.061069999, 0.16858, 0.067280002, 0.0, 0.0, 0.0, 0.35163999, 0.074349999, 0.0},
           {0.27271, 0.90486997, 0.0, 0.67374998, 0.071489997, 0.19324, 0.0, 0.0, 0.0, 0.0, 0.092809997, 0.0, 0.0, 0.0, 0.37125999, 0.0},
           {0.0, 0.0, 0.30159, 0.0, 0.0, 0.84898001, 0.18082, 0.0, 0.053240001, 0.0, 0.80101001, 0.075759999, 0.0, 0.0, 0.89740002, 0.0},
           {0.0, 0.0, 0.62568998, 0.0, 0.0, 0.0, 0.0, 0.0, 0.46845999, 0.80119002, 0.0, 0.074770004, 0.094690003, 0.0, 0.0, 0.0},
           {0.062820002, 0.0, 0.098190002, 0.0, 0.0, 0.0, 0.79606003, 0.0, 0.21277, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.52516001},
           {0.0, 0.48618999, 0.45532, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.088950001, 0.0, 0.09234},
           {0.2368, 0.0, 0.0, 0.99505001, 0.33265001, 0.0, 0.0, 0.0, 0.0, 0.33741999, 0.07621, 0.0, 0.0, 0.0, 0.0, 0.20547999}
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
