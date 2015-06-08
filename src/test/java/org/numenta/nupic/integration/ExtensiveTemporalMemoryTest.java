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

package org.numenta.nupic.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.PatternMachine;
import org.numenta.nupic.datagen.SequenceMachine;
import org.numenta.nupic.integration.TemporalMemoryTestMachine.DetailedResults;


/**
 * These tests ensure the most basic (first order) sequence learning mechanism is
 * working.
 *
 * Parameters: Use a "fast learning mode": initPerm should be greater than
 * connectedPerm and permanenceDec should be zero. With these settings sequences
 * should be learned in one pass:
 *
 *   minThreshold = newSynapseCount
 *   initialPermanence = 0.8
 *   connectedPermanence = 0.7
 *   permanenceDecrement = 0
 *   permanenceIncrement = 0.4
 *
 * Other Parameters:
 *   columnDimensions = [100]
 *   cellsPerColumn = 1
 *   newSynapseCount = 11
 *   activationThreshold = 8

 * Note: this is not a high order sequence, so one cell per column is fine.
 *
 * Input Sequence: We train with M input sequences, each consisting of N random
 * patterns. Each pattern consists of a 2 bits on.
 *
 * Training: The TP is trained with P passes of the M sequences. There
 * should be a reset between sequences. The total number of iterations during
 * training is P*N*M.
 *
 * Testing: Run inference through the same set of sequences, with a reset before
 * each sequence. For each sequence the system should accurately predict the
 * pattern at the next time step up to and including the N-1'st pattern. The number
 * of predicted inactive cells at each time step should be reasonably low.
 *
 * We can also calculate the number of synapses that should be
 * learned. We raise an error if too many or too few were learned.
 *
 * B1) Basic sequence learner.  M=1, N=100, P=1.
 *
 * B2) Same as above, except P=2. Test that permanences go up and that no
 * additional synapses are learned. [TODO]
 *
 * B3) N=300, M=1, P=1. (See how high we can go with M) [TODO]
 *
 * B4) N=100, M=3, P=1. (See how high we can go with N*M) [TODO]
 *
 * B5) Like B1 but with cellsPerColumn = 4. First order sequences should still
 * work just fine. [TODO]
 *
 * B6) Like B1 but with slower learning. Set the following parameters differently:
 *
 *     activationThreshold = newSynapseCount
 *     minThreshold = activationThreshold
 *     initialPerm = 0.2
 *     connectedPerm = 0.7
 *     permanenceInc = 0.2
 *
 * Now we train the TP with the B1 sequence 4 times (P=4). This will increment
 * the permanences to be above 0.8 and at that point the inference will be correct.
 * This test will ensure the basic match function and segment activation rules are
 * working correctly. [TODO]
 *
 * B7) Like B6 but with 4 cells per column. Should still work. [TODO]
 *
 * B8) Like B6 but present the sequence less than 4 times: the inference should be
 * incorrect. [TODO]
 *
 * B9) Like B2, except that cells per column = 4. Should still add zero additional
 * synapses. [TODO]
 * 
 * @author Chetan Surpur
 * @author David Ray
 * 
 * @see AbstractTemporalMemoryTest
 * @see BasicTemporalMemoryTest
 */
public class ExtensiveTemporalMemoryTest extends AbstractTemporalMemoryTest {
    /**
     * Basic static input for all tests in this class
     */
    private void defaultSetup() {
        parameters = Parameters.getAllDefaultParameters();
        parameters.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 100 });
        parameters.setParameterByKey(KEY.CELLS_PER_COLUMN, 1);
        parameters.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.8);
        parameters.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.7);
        parameters.setParameterByKey(KEY.MIN_THRESHOLD, 11);
        parameters.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 11);
        parameters.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.4);
        parameters.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.0);
        parameters.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 8);
    }
    
    /**
     * Basic sequence learner.  M=1, N=100, P=1.
     */
    @Test
    public void testB1() {
        defaultSetup();
        
        initTM();
        
        int seed = 42;
        finishSetUp(new PatternMachine(
            ((int[])parameters.getParameterByKey(Parameters.KEY.COLUMN_DIMENSIONS))[0], 23, seed));
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            80, 59, 19, 9, 48, 97, 83, 90, 51, 16, 27, 18, 2, 42, 95, 
            57, 26, 21, 84, 88, 41, 13, 40, 43, 71, 99, 92, 32, 11, 91, 
            73, 86, 39, 56, 8, 60, 53, 65, 25, 33, 4, 66, 12, 14, 31, 
            58, 72, 55, 10, 22, 15, 94, 49, 81, 79, 98, 5, 1, 20, 70, 
            35, 87, 24, 78, 37, 38, 67, 34, 17, 0, 85, 96, 54, 29, 77, 
            74, 3, 50, 28, 89, 62, 6, 52, 45, 76, 30, 68, 47, 61, 82, 
            46, 36, 44, 93, 7, 75, 69, 64, 23, 63 };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        sequence.add(SequenceMachine.NONE);
        
        feedTM(sequence, true, 1);
        
        DetailedResults results = feedTM(sequence, false, 1);
        
        int count = 0;
        for(int i = 0;i < results.unpredictedActiveColumnsList.size();i++) {
            if(results.unpredictedActiveColumnsList.get(0).iterator().hasNext()) {
                count += 1;
            }
        }
        assertEquals(0, count);
        
        for(int i = 1;i < results.predictedActiveColumnsList.size() - 1;i++) {
            assertEquals(23, results.predictedActiveColumnsList.get(i).size());
        }
        
        for(int i = 1;i < results.predictedInactiveColumnsList.size() - 1;i++) {
            assertTrue(results.predictedInactiveColumnsList.get(i).size() < 5);
        }
    }
}
