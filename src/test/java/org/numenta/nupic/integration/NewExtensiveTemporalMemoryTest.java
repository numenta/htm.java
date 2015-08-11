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
 * ==============================================================================
 *                        Basic First Order Sequences
 * ==============================================================================
 * 
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
 *   activationThreshold = 11

 * Note: this is not a high order sequence, so one cell per column is fine.
 *
 * Input Sequence: We train with M input sequences, each consisting of N random
 * patterns. Each pattern consists of a random number of bits on. The number of
 * 1's in each pattern should be between 21 and 25 columns.
 *
 * Each input pattern can optionally have an amount of spatial noise represented
 * by X, where X is the probability of switching an on bit with a random bit.
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
 * B3) N=300, M=1, P=1. (See how high we can go with N)
 *
 * B4) N=100, M=3, P=1. (See how high we can go with N*M)
 *
 * B5) Like B1 but with cellsPerColumn = 4. First order sequences should still
 * work just fine.
 * 
 * B6) Like B4 but with cellsPerColumn = 4. First order sequences should still
 * work just fine.
 *
 * B7) Like B1 but with slower learning. Set the following parameters differently:
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
 * working correctly.
 *
 * B8) Like B7 but with 4 cells per column. Should still work.
 *
 * B9) Like B7 but present the sequence less than 4 times: the inference should be
 * incorrect.
 *
 * B10) Like B2, except that cells per column = 4. Should still add zero additional
 * synapses. [TODO]
 * 
 * B11) Like B5, but with activationThreshold = 8 and with each pattern
 * corrupted by a small amount of spatial noise (X = 0.05).
 * 
 * 
 * ==============================================================================
 *                        High Order Sequences
 * ==============================================================================
 * 
 * These tests ensure that high order sequences can be learned in a multiple cells
 * per column instantiation.
 *
 * Parameters: Same as Basic First Order Tests above, but with varying cells per
 * column.
 *
 * Input Sequence: We train with M input sequences, each consisting of N random
 * patterns. Each pattern consists of a random number of bits on. The number of
 * 1's in each pattern should be between 21 and 25 columns. The sequences are
 * constructed to contain shared subsequences, such as:
 *
 * A B C D E F G H I J
 * K L M D E F N O P Q
 *
 * The position and length of shared subsequences are parameters in the tests.
 *
 * Each input pattern can optionally have an amount of spatial noise represented
 * by X, where X is the probability of switching an on bit with a random bit.
 *
 * Training: Identical to basic first order tests above.
 *
 * Testing: Identical to basic first order tests above unless noted.
 *
 * We can also calculate the number of segments and synapses that should be
 * learned. We raise an error if too many or too few were learned.
 *
 * H1) Learn two sequences with a shared subsequence in the middle. Parameters
 * should be the same as B1. Since cellsPerColumn == 1, it should make more
 * predictions than necessary.
 *
 * H2) Same as H1, but with cellsPerColumn == 4, and train multiple times.
 * It should make just the right number of predictions.
 *
 * H3) Like H2, except the shared subsequence is in the beginning (e.g.
 * "ABCDEF" and "ABCGHIJ"). At the point where the shared subsequence ends, all
 * possible next patterns should be predicted. As soon as you see the first unique
 * pattern, the predictions should collapse to be a perfect prediction.
 *
 * H4) Shared patterns. Similar to H2 except that patterns are shared between
 * sequences.  All sequences are different shufflings of the same set of N
 * patterns (there is no shared subsequence).
 *
 * H5) Combination of H4) and H2). Shared patterns in different sequences, with a
 * shared subsequence.
 *
 * H6) Stress test: every other pattern is shared. [TODO]
 *
 * H7) Start predicting in the middle of a sequence. [TODO]
 *
 * H8) Hub capacity. How many patterns can use that hub? [TODO]
 *
 * H9) Sensitivity to small amounts of spatial noise during inference (X = 0.05).
 * Parameters the same as B11, and sequences like H2.
 *
 * H10) Higher order patterns with alternating elements.
 *
 * Create the following 4 sequences:
 *
 *      A B A B A C
 *      A B A B D E
 *      A B F G H I
 *      A J K L M N
 *
 * After training we should verify that the expected transitions are in the
 * model. Prediction accuracy should be perfect. In addition, during inference,
 * after the first element is presented, the columns should not burst any more.
 * Need to verify, for the first sequence, that the high order representation
 * when presented with the second A and B is different from the representation
 * in the first presentation. [TODO]
 * 
 * @author Chetan Surpur
 * @author David Ray
 * 
 * @see AbstractTemporalMemoryTest
 * @see BasicTemporalMemoryTest
 */
public class NewExtensiveTemporalMemoryTest extends NewAbstractTemporalMemoryTest {
    private static final PatternMachine PATTERN_MACHINE = new PatternMachine(100, 23, 300, true);
    
    /**
     * Basic sequence learner.  M=1, N=100, P=1.
     */
    @Test
    public void testB1() {
        
        init(null, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            83, 53, 70, 45, 44, 39, 22, 80, 10, 0, 18, 30, 73, 33, 90, 4, 76, 77, 12, 31, 55, 88, 
            26, 42, 69, 15, 40, 96, 9, 72, 11, 47, 85, 28, 93, 5, 66, 65, 35, 16, 49, 34, 7, 95, 
            27, 19, 81, 25, 62, 13, 24, 3, 17, 38, 8, 78, 6, 64, 36, 89, 56, 99, 54, 43, 50, 67, 
            46, 68, 61, 97, 79, 41, 58, 48, 98, 57, 75, 32, 94, 59, 63, 84, 37, 29, 1, 52, 21, 2, 
            23, 87, 91, 74, 86, 82, 20, 60, 71, 14, 92, 51, -1
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, true, 1);
    }
}
