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

package org.numenta.nupic.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.PatternMachine;
import org.numenta.nupic.datagen.SequenceMachine;
import org.numenta.nupic.monitor.mixin.IndicesTrace;
import org.numenta.nupic.monitor.mixin.Metric;


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
public class ExtensiveTemporalMemoryTest extends AbstractTemporalMemoryTest {
    private static final PatternMachine PATTERN_MACHINE = new PatternMachine(100, 23, 300, true);
    
    private static final int VERBOSITY = 1;
    
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
        
        feedTM(sequence, "", true, 1);
        
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * N=300, M=1, P=1. (See how high we can go with N)
     */
    @Test
    public void testB3() {
        
        init(null, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            203, 266, 152, 9, 233, 226, 196, 109, 5, 175, 237, 57, 218, 45, 182, 221, 289, 211, 
            148, 165, 78, 113, 249, 250, 104, 42, 281, 295, 157, 238, 17, 164, 33, 24, 215, 119, 
            7, 90, 46, 73, 93, 76, 286, 60, 77, 63, 234, 229, 111, 231, 180, 144, 239, 75, 297, 278, 
            97, 92, 192, 25, 232, 59, 6, 185, 173, 30, 22, 256, 56, 186, 108, 126, 230, 193, 129, 
            282, 82, 84, 66, 288, 163, 154, 19, 124, 79, 114, 118, 72, 15, 10, 194, 101, 68, 224, 
            37, 16, 179, 147, 274, 67, 228, 69, 31, 183, 265, 225, 140, 18, 181, 96, 132, 262, 86, 
            248, 245, 116, 146, 292, 197, 206, 55, 172, 184, 167, 139, 253, 38, 125, 195, 283, 137, 
            112, 168, 117, 277, 271, 155, 176, 178, 2, 115, 143, 177, 120, 210, 260, 127, 74, 29, 83, 
            269, 107, 223, 158, 280, 246, 222, 65, 198, 85, 213, 159, 12, 35, 28, 142, 284, 254, 170, 
            51, 95, 208, 247, 41, 89, 244, 136, 26, 293, 141, 200, 0, 268, 272, 100, 259, 255, 171, 
            98, 36, 61, 150, 236, 202, 242, 11, 296, 267, 27, 219, 4, 122, 32, 204, 162, 209, 285, 
            138, 62, 135, 128, 290, 8, 70, 264, 64, 44, 279, 156, 40, 123, 275, 216, 153, 23, 261, 
            110, 81, 207, 212, 39, 240, 291, 258, 199, 14, 47, 94, 263, 227, 273, 201, 161, 43, 217, 
            145, 190, 220, 251, 3, 105, 53, 133, 1, 131, 103, 49, 80, 205, 34, 91, 52, 241, 13, 88, 
            166, 294, 134, 287, 243, 54, 50, 174, 189, 298, 187, 169, 58, 48, 235, 252, 21, 160, 276, 
            191, 257, 149, 130, 151, 99, 87, 214, 121, 299, 20, 188, 71, 106, 270, 102, -1
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, "", true, 1);
        
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * Basic sequence learner.  M=1, N=100, P=1.
     */
    @Test
    public void testB4() {
        
        init(null, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            83, 53, 70, 45, 44, 39, 22, 80, 10, 0, 18, 30, 73, 33, 90, 4, 76, 77, 12, 31, 55, 88, 
            26, 42, 69, 15, 40, 96, 9, 72, 11, 47, 85, 28, 93, 5, 66, 65, 35, 16, 49, 34, 7, 95, 
            27, 19, 81, 25, 62, 13, 24, 3, 17, 38, 8, 78, 6, 64, 36, 89, 56, 99, 54, 43, 50, 67, 
            46, 68, 61, 97, 79, 41, 58, 48, 98, 57, 75, 32, 94, 59, 63, 84, 37, 29, 1, 52, 21, 2, 
            23, 87, 91, 74, 86, 82, 20, 60, 71, 14, 92, 51, -1, 179, 137, 165, 154, 115, 120, 
            199, 125, 156, 117, 159, 193, 187, 166, 155, 173, 139, 130, 116, 149, 160, 153, 183, 
            123, 188, 109, 118, 182, 174, 189, 158, 198, 148, 176, 157, 190, 175, 186, 163, 124, 
            178, 110, 129, 119, 145, 181, 185, 152, 105, 167, 169, 101, 192, 121, 168, 191, 131, 
            112, 135, 128, 142, 170, 144, 138, 184, 103, 151, 162, 150, 141, 114, 108, 126, 113, 
            194, 100, 102, 177, 146, 164, 196, 143, 136, 161, 122, 147, 195, 133, 111, 171, 172, 
            106, 127, 140, 104, 132, 197, 134, 107, 180, -1, 248, 228, 285, 283, 214, 209, 242, 
            221, 275, 293, 244, 226, 260, 212, 220, 246, 245, 240, 213, 261, 253, 250, 271, 294, 
            257, 205, 203, 207, 284, 277, 262, 233, 265, 282, 288, 286, 247, 281, 287, 208, 292, 
            236, 204, 211, 259, 299, 225, 278, 206, 234, 241, 229, 230, 222, 249, 224, 256, 227, 
            279, 297, 264, 217, 238, 263, 239, 232, 251, 273, 270, 266, 298, 235, 276, 219, 202, 
            280, 258, 272, 215, 210, 290, 268, 223, 237, 216, 255, 274, 254, 267, 291, 269, 231, 
            289, 243, 252, 201, 218, 200, 296, 295, -1
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, "", true, 3);
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
    }
    
    /**
     * Like B1 but with cellsPerColumn = 4.
     * First order sequences should still work just fine.
     */
    @Test
    public void testB5() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getCellsPerColumn() == 4);
        
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
        
        feedTM(sequence, "", true, 1);
        
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * Like B4 but with cellsPerColumn = 4.
     * First order sequences should still work just fine.
     */
    @Test
    public void testB6() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getCellsPerColumn() == 4);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            83, 53, 70, 45, 44, 39, 22, 80, 10, 0, 18, 30, 73, 33, 90, 4, 76, 77, 12, 31, 55, 88, 
            26, 42, 69, 15, 40, 96, 9, 72, 11, 47, 85, 28, 93, 5, 66, 65, 35, 16, 49, 34, 7, 95, 
            27, 19, 81, 25, 62, 13, 24, 3, 17, 38, 8, 78, 6, 64, 36, 89, 56, 99, 54, 43, 50, 67, 
            46, 68, 61, 97, 79, 41, 58, 48, 98, 57, 75, 32, 94, 59, 63, 84, 37, 29, 1, 52, 21, 2, 
            23, 87, 91, 74, 86, 82, 20, 60, 71, 14, 92, 51, -1, 179, 137, 165, 154, 115, 120, 
            199, 125, 156, 117, 159, 193, 187, 166, 155, 173, 139, 130, 116, 149, 160, 153, 183, 
            123, 188, 109, 118, 182, 174, 189, 158, 198, 148, 176, 157, 190, 175, 186, 163, 124, 
            178, 110, 129, 119, 145, 181, 185, 152, 105, 167, 169, 101, 192, 121, 168, 191, 131, 
            112, 135, 128, 142, 170, 144, 138, 184, 103, 151, 162, 150, 141, 114, 108, 126, 113, 
            194, 100, 102, 177, 146, 164, 196, 143, 136, 161, 122, 147, 195, 133, 111, 171, 172, 
            106, 127, 140, 104, 132, 197, 134, 107, 180, -1, 248, 228, 285, 283, 214, 209, 242, 
            221, 275, 293, 244, 226, 260, 212, 220, 246, 245, 240, 213, 261, 253, 250, 271, 294, 
            257, 205, 203, 207, 284, 277, 262, 233, 265, 282, 288, 286, 247, 281, 287, 208, 292, 
            236, 204, 211, 259, 299, 225, 278, 206, 234, 241, 229, 230, 222, 249, 224, 256, 227, 
            279, 297, 264, 217, 238, 263, 239, 232, 251, 273, 270, 266, 298, 235, 276, 219, 202, 
            280, 258, 272, 215, 210, 290, 268, 223, 237, 216, 255, 274, 254, 267, 291, 269, 231, 
            289, 243, 252, 201, 218, 200, 296, 295, -1
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, "", true, 3);
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * Like B1 but with slower learning.
     *
     * Set the following parameters differently:
     *
     *   initialPermanence = 0.2
     *   connectedPermanence = 0.7
     *   permanenceIncrement = 0.2
     *
     * Now we train the TP with the B1 sequence 4 times (P=4). This will increment
     * the permanences to be above 0.8 and at that point the inference will be correct.
     * This test will ensure the basic match function and segment activation rules are
     * working correctly.
     */
    @Test
    public void testB7() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        p.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.7);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.2);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getInitialPermanence() == 0.2);
        assertTrue(tm.getConnections().getConnectedPermanence() == 0.7);
        assertTrue(tm.getConnections().getPermanenceIncrement() == 0.2);
        
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
        
        for(int i = 0;i < 4;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * Like B7 but with 4 cells per column.
     * Should still work.
     */
    @Test
    public void testB8() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        p.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.7);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.2);
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getInitialPermanence() == 0.2);
        assertTrue(tm.getConnections().getConnectedPermanence() == 0.7);
        assertTrue(tm.getConnections().getPermanenceIncrement() == 0.2);
        assertTrue(tm.getConnections().getCellsPerColumn() == 4);
        
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
        
        for(int i = 0;i < 4;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        
        assertAllActiveWerePredicted();
        assertAllInactiveWereUnpredicted();
    }
    
    /**
     * Like B7 but present the sequence less than 4 times.
     * The inference should be incorrect.
     */
    @Test
    public void testB9() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.2);
        p.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.7);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.2);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getInitialPermanence() == 0.2);
        assertTrue(tm.getConnections().getConnectedPermanence() == 0.7);
        assertTrue(tm.getConnections().getPermanenceIncrement() == 0.2);
        assertTrue(tm.getConnections().getCellsPerColumn() == 1);
        
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
        
        for(int i = 0;i < 3;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        
        assertAllActiveWereUnpredicted();
    }
    
    /**
     * Like B5, but with activationThreshold = 8 and with each pattern
     * corrupted by a small amount of spatial noise (X = 0.05).
     * <pre>
     * /////////////////////////////////////////////////////////////////
     * // Warning: This test diverges from Python due to heavy random // 
     * //          segment and synapse creation above the             //
     * //          "activationThreshold" of 8                         //
     * /////////////////////////////////////////////////////////////////
     * </pre>
     * <b>Observations:</b>
     * <ol>
     *      <li>The more activity beyond the point when Segments reach the "activationThreshold", the more the number of Synapses vary per Segment, and therefore make the statistics diverge.</li>
     *      <li>When setting the "activationThreshold" back from 8 to 11, the output was exactly the same.</li>
     *      <li>The total number of Synapses stay the same between the Python and Java versions regardless of other divergence! Which is a good thing.</li> 
     *      <li>While the total number of Synapses stay the same, the predictedActive and predictedInactive Columns also stay the same.</li>
     * </ol>     
     */
    @Test
    public void testB11() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 8);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getCellsPerColumn() == 4);
        assertTrue(tm.getConnections().getActivationThreshold() == 8);
        
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
        
        feedTM(sequence, "", true, 1);
        
        sequence = translateSequence(
            "set([96, 1, 66, 43, 48, 33, 72, 31, 10, 71, 44, 16, 49, 19, 20, 87, 89, 58, 63, 74, 52]), " +
            "set([0, 1, 4, 10, 15, 16, 23, 28, 31, 40, 46, 48, 50, 58, 67, 71, 77, 79, 90, 92, 93, 97, 99]), " +
            "set([5, 7, 10, 14, 17, 21, 29, 31, 39, 46, 53, 61, 67, 69, 71, 77, 80, 81, 85, 86, 87, 88, 93, 97]), " +
            "set([5, 6, 10, 11, 14, 17, 26, 30, 33, 38, 39, 42, 45, 52, 53, 58, 59, 60, 64, 67, 74, 97, 98, 99]), " +
            "set([5, 13, 19, 23, 25, 26, 29, 35, 36, 37, 42, 48, 57, 59, 60, 67, 73, 77, 92, 95, 96, 98]), " +
            "set([3, 17, 18, 21, 24, 27, 34, 35, 37, 40, 44, 47, 50, 58, 59, 60, 62, 64, 66, 69, 77, 79, 82, 95, 98]), " +
            "set([5, 15, 18, 24, 36, 37, 39, 40, 42, 45, 58, 59, 64, 68, 69, 77, 83, 87, 89, 90, 91, 98]), " +
            "set([1, 12, 15, 22, 23, 24, 25, 36, 37, 38, 50, 55, 56, 63, 65, 66, 70, 72, 74, 81, 82, 87, 92, 95, 99]), " +
            "set([4, 5, 7, 11, 14, 16, 40, 45, 46, 47, 53, 58, 60, 67, 69, 71, 73, 75, 77, 79, 85, 89, 95, 96]), " +
            "set([0, 4, 10, 12, 18, 22, 26, 30, 31, 33, 39, 44, 45, 53, 70, 73, 76, 77, 80, 83, 88, 90]), " +
            "set([4, 7, 11, 12, 14, 15, 20, 23, 26, 27, 31, 40, 50, 54, 56, 58, 61, 65, 75, 77, 81, 85, 86, 87, 97]), " +
            "set([96, 65, 66, 68, 33, 12, 75, 76, 45, 78, 16, 56, 19, 1, 24, 89, 71, 63, 62, 53]), " +
            "set([1, 3, 6, 7, 11, 12, 16, 19, 22, 29, 31, 33, 34, 37, 43, 50, 53, 61, 62, 72, 77, 80, 91]), " +
            "set([1, 8, 10, 20, 22, 23, 27, 29, 30, 31, 41, 49, 51, 61, 71, 74, 76, 80, 81, 86, 93, 94]), " +
            "set([1, 12, 19, 20, 21, 26, 31, 34, 35, 38, 46, 52, 57, 61, 64, 66, 75, 79, 80, 90, 92, 93, 94, 99]), " +
            "set([0, 5, 7, 17, 19, 24, 25, 38, 40, 43, 44, 45, 46, 48, 54, 55, 66, 74, 77, 86, 90, 95]), " +
            "set([3, 6, 19, 23, 31, 40, 44, 46, 47, 53, 55, 67, 68, 70, 73, 75, 76, 83, 88, 90, 97, 99]), " +
            "set([4, 9, 16, 19, 21, 23, 37, 43, 45, 46, 47, 55, 62, 72, 74, 79, 81, 86, 89, 92, 93, 94]), " +
            "set([1, 34, 59, 36, 38, 6, 71, 8, 2, 12, 66, 14, 45, 49, 82, 54, 56, 26, 47, 60, 61]), " +
            "set([5, 6, 7, 11, 17, 23, 25, 27, 28, 30, 35, 38, 42, 52, 66, 67, 68, 75, 77, 78, 82, 87, 88, 93, 99]), " +
            "set([64, 96, 35, 68, 70, 6, 71, 75, 76, 15, 80, 17, 51, 53, 23, 4, 58, 5, 93, 30, 19]), " +
            "set([0, 11, 13, 14, 17, 18, 21, 23, 24, 26, 33, 37, 58, 62, 71, 77, 80, 81, 84, 89, 90, 93, 96]), " +
            "set([0, 34, 99, 68, 69, 6, 39, 11, 44, 77, 81, 82, 83, 20, 22, 55, 23, 52, 91, 62, 31]), " +
            "set([9, 19, 21, 24, 28, 29, 34, 39, 41, 42, 45, 54, 61, 62, 65, 66, 72, 74, 80, 91, 95, 97, 99]), " +
            "set([4, 8, 9, 11, 13, 15, 16, 35, 36, 37, 39, 49, 50, 56, 66, 67, 68, 72, 78, 84, 91, 98, 99]), " +
            "set([1, 10, 13, 17, 18, 19, 28, 29, 30, 31, 32, 35, 36, 37, 38, 44, 50, 66, 71, 75, 81, 90]), " +
            "set([0, 64, 2, 99, 4, 38, 70, 39, 45, 78, 50, 18, 52, 85, 87, 57, 90, 95, 28, 93, 84]), " +
            "set([6, 9, 11, 13, 15, 17, 26, 27, 29, 33, 35, 37, 39, 48, 54, 55, 57, 62, 63, 84, 89, 95, 98]), " +
            "set([3, 4, 7, 8, 9, 10, 18, 21, 30, 36, 38, 47, 49, 51, 55, 56, 60, 67, 68, 71, 74, 80, 82, 91, 95]), " +
            "set([2, 4, 9, 17, 19, 26, 29, 31, 34, 47, 49, 59, 61, 62, 63, 64, 65, 74, 80, 88, 91, 98]), " +
            "set([4, 18, 20, 21, 25, 28, 35, 38, 44, 50, 51, 59, 60, 62, 63, 64, 70, 73, 75, 79, 85, 93, 97]), " +
            "set([2, 3, 12, 17, 19, 22, 23, 26, 28, 35, 37, 41, 45, 47, 48, 50, 51, 64, 66, 82, 85, 86, 88, 94, 97]), " +
            "set([5, 8, 12, 14, 18, 19, 22, 23, 27, 39, 40, 44, 48, 49, 50, 54, 60, 61, 68, 71, 79, 87, 91, 97]), " +
            "set([10, 25, 29, 33, 34, 40, 43, 46, 57, 58, 65, 66, 75, 77, 78, 79, 84, 86, 91, 92, 93, 99]), " +
            "set([4, 5, 6, 8, 12, 13, 18, 20, 25, 26, 29, 30, 35, 36, 47, 57, 60, 67, 76, 85, 91, 92, 94, 97]), " +
            "set([0, 4, 5, 9, 12, 17, 19, 25, 34, 38, 41, 42, 44, 56, 65, 68, 74, 76, 77, 79, 83, 88, 93, 96, 99]), " +
            "set([16, 67, 86, 35, 7, 41, 10, 11, 44, 77, 73, 48, 17, 75, 19, 56, 53, 76, 9, 24, 58]), " +
            "set([0, 97, 66, 35, 33, 72, 73, 10, 11, 78, 60, 49, 50, 19, 54, 89, 83, 57, 28, 29, 63]), " +
            "set([5, 6, 8, 12, 17, 25, 29, 31, 33, 34, 35, 46, 51, 56, 57, 62, 63, 67, 71, 78, 79, 87, 95]), " +
            "set([3, 9, 17, 19, 23, 27, 29, 30, 32, 35, 36, 40, 52, 56, 58, 60, 63, 67, 83, 85, 88, 90, 95, 96]), " +
            "set([32, 1, 66, 36, 6, 33, 64, 39, 44, 45, 14, 81, 82, 83, 52, 55, 18, 89, 59, 30, 31]), " +
            "set([1, 17, 21, 24, 28, 31, 33, 34, 37, 39, 45, 48, 53, 54, 58, 59, 66, 74, 76, 80, 86, 88, 93, 97]), " +
            "set([4, 15, 18, 20, 29, 32, 33, 35, 36, 39, 40, 42, 44, 53, 58, 69, 79, 80, 83, 86, 95, 97]), " +
            "set([0, 65, 98, 67, 39, 40, 76, 75, 12, 66, 14, 45, 27, 81, 7, 25, 26, 79, 92, 44, 63]), " +
            "set([0, 1, 2, 6, 7, 9, 20, 28, 29, 30, 38, 46, 47, 51, 55, 58, 60, 66, 69, 87, 94, 97]), " +
            "set([5, 7, 12, 14, 16, 19, 22, 23, 27, 31, 32, 41, 43, 53, 57, 65, 68, 73, 75, 78, 82, 91, 92, 96, 99]), " +
            "set([5, 9, 12, 15, 18, 19, 21, 26, 36, 37, 40, 49, 50, 51, 63, 66, 69, 76, 78, 81, 91, 93]), " +
            "set([1, 2, 3, 4, 8, 9, 13, 22, 25, 32, 36, 37, 46, 49, 52, 53, 56, 61, 64, 84, 90, 91, 93, 96]), " +
            "set([0, 1, 3, 5, 6, 7, 11, 12, 18, 21, 25, 30, 45, 50, 75, 76, 77, 78, 79, 84, 87, 91, 95]), " +
            "set([1, 10, 12, 18, 26, 38, 40, 49, 50, 53, 55, 61, 65, 66, 68, 71, 77, 80, 89, 90, 92, 96]), " +
            "set([3, 12, 13, 16, 18, 20, 24, 26, 30, 31, 35, 38, 42, 50, 52, 54, 55, 56, 59, 60, 61, 63, 77, 79, 98]), " +
            "set([3, 4, 5, 9, 11, 13, 16, 19, 21, 22, 24, 48, 49, 60, 61, 65, 66, 69, 70, 78, 85, 87, 96]), " +
            "set([19, 35, 4, 6, 41, 43, 76, 77, 78, 47, 48, 67, 21, 86, 55, 58, 27, 92, 60, 94, 95]), " +
            "set([1, 3, 5, 10, 16, 23, 28, 30, 35, 39, 46, 47, 50, 52, 54, 60, 68, 72, 83, 84, 85, 88, 92, 99]), " +
            "set([2, 6, 8, 13, 15, 16, 27, 33, 37, 41, 48, 52, 61, 62, 64, 68, 73, 74, 75, 79, 89, 91, 96, 98]), " +
            "set([35, 2, 3, 4, 69, 71, 80, 73, 45, 14, 15, 48, 98, 19, 78, 56, 58, 31, 28, 90, 63]), " +
            "set([1, 3, 14, 17, 19, 22, 25, 26, 28, 35, 36, 48, 49, 55, 58, 67, 69, 72, 80, 85, 91, 93, 98]), " +
            "set([96, 97, 2, 99, 68, 7, 73, 74, 75, 12, 66, 81, 51, 84, 23, 89, 90, 60, 29, 63]), " +
            "set([10, 14, 25, 30, 37, 40, 44, 52, 56, 58, 60, 63, 70, 72, 74, 80, 82, 84, 86, 93, 97, 99]), " +
            "set([97, 67, 4, 71, 72, 80, 7, 12, 13, 46, 16, 49, 35, 84, 53, 89, 23, 88, 20, 31]), " +
            "set([0, 97, 36, 6, 7, 96, 10, 43, 76, 12, 16, 18, 83, 84, 78, 54, 73, 23, 31, 29, 19]), " +
            "set([97, 67, 68, 69, 7, 95, 11, 12, 77, 79, 33, 75, 84, 86, 23, 52, 25, 29, 61, 62, 5]), " +
            "set([34, 3, 37, 63, 73, 42, 21, 13, 14, 47, 50, 49, 82, 19, 52, 46, 41, 25, 27, 92, 53]), " +
            "set([0, 1, 7, 10, 11, 22, 30, 36, 44, 47, 53, 56, 57, 58, 59, 65, 66, 75, 76, 84, 85, 95, 98]), " +
            "set([2, 10, 13, 15, 17, 19, 20, 24, 26, 38, 39, 40, 43, 46, 51, 58, 59, 61, 74, 78, 81, 82]), " +
            "set([3, 4, 15, 21, 26, 45, 47, 48, 50, 52, 60, 62, 64, 72, 73, 77, 86, 90, 91, 94, 95, 98]), " +
            "set([0, 2, 3, 5, 6, 8, 11, 23, 25, 30, 34, 37, 43, 49, 52, 57, 61, 63, 68, 79, 90, 98]), " +
            "set([7, 10, 11, 12, 21, 22, 24, 35, 36, 41, 45, 46, 47, 49, 52, 55, 58, 60, 64, 74, 81, 85, 90, 94]), " +
            "set([2, 3, 6, 8, 13, 24, 25, 29, 33, 44, 45, 53, 55, 60, 61, 64, 65, 76, 79, 80, 84, 90, 91, 94, 97]), " +
            "set([0, 5, 10, 15, 22, 30, 31, 32, 33, 37, 43, 47, 48, 49, 58, 59, 60, 66, 67, 79, 93, 97]), " +
            "set([8, 11, 15, 20, 23, 31, 37, 46, 47, 55, 56, 57, 58, 60, 68, 73, 74, 80, 82, 83, 94, 99]), " +
            "set([0, 4, 6, 8, 10, 11, 18, 22, 30, 35, 37, 43, 44, 47, 53, 58, 60, 61, 66, 69, 74, 75, 80, 90, 94]), " +
            "set([0, 2, 7, 11, 14, 19, 20, 21, 28, 33, 35, 37, 43, 53, 64, 68, 69, 72, 73, 79, 80, 83, 86, 98]), " +
            "set([96, 11, 36, 69, 72, 37, 42, 75, 45, 78, 50, 55, 24, 57, 90, 59, 5, 74, 23, 95]), " +
            "set([5, 19, 22, 26, 37, 38, 40, 41, 44, 45, 48, 49, 51, 55, 60, 64, 70, 74, 75, 78, 89, 92, 96]), " +
            "set([7, 16, 18, 26, 30, 35, 41, 42, 43, 45, 48, 52, 53, 54, 74, 78, 84, 85, 86, 90, 94, 99]), " +
            "set([2, 4, 11, 19, 30, 39, 44, 45, 46, 48, 51, 52, 56, 59, 60, 61, 70, 74, 80, 89, 94, 96, 98, 99]), " +
            "set([96, 59, 5, 7, 40, 47, 42, 77, 15, 49, 82, 67, 22, 23, 56, 25, 58, 27, 61, 62]), " +
            "set([4, 5, 12, 15, 17, 20, 21, 23, 29, 32, 37, 43, 54, 60, 70, 71, 74, 77, 83, 88, 93, 96]), " +
            "set([0, 11, 13, 18, 19, 22, 29, 30, 31, 32, 36, 42, 44, 50, 52, 63, 72, 79, 91, 92, 96, 99]), " +
            "set([7, 10, 18, 19, 22, 23, 28, 29, 34, 35, 37, 39, 50, 51, 55, 56, 67, 68, 70, 71, 72, 80, 89, 91, 95]), " +
            "set([0, 1, 3, 9, 10, 12, 21, 32, 42, 48, 52, 53, 54, 62, 64, 67, 69, 73, 75, 78, 80, 81, 90, 98, 99]), " +
            "set([1, 4, 8, 9, 12, 13, 15, 23, 24, 29, 35, 45, 49, 51, 55, 59, 64, 65, 67, 72, 78, 82, 86, 87, 91]), " +
            "set([1, 7, 12, 21, 23, 25, 27, 28, 36, 38, 43, 50, 51, 61, 66, 70, 75, 83, 84, 94, 97, 99]), " +
            "set([2, 4, 6, 8, 11, 13, 15, 16, 23, 27, 38, 42, 46, 48, 51, 55, 56, 59, 62, 65, 76, 77, 78, 82, 89]), " +
            "set([0, 20, 22, 23, 24, 25, 26, 27, 28, 31, 32, 38, 41, 46, 47, 52, 56, 61, 73, 81, 82, 83, 94, 97, 98]), " +
            "set([6, 10, 21, 24, 26, 30, 36, 45, 47, 59, 62, 67, 68, 70, 71, 72, 75, 80, 81, 83, 91, 92, 95]), " +
            "set([2, 7, 10, 12, 19, 25, 38, 39, 44, 46, 49, 52, 56, 72, 77, 78, 79, 83, 84, 88, 94, 97, 98, 99]), " +
            "set([1, 7, 12, 13, 18, 22, 25, 26, 31, 38, 40, 58, 61, 62, 67, 70, 71, 73, 84, 85, 92, 97, 98]), " +
            "set([96, 1, 48, 5, 7, 9, 10, 39, 77, 14, 16, 49, 18, 22, 23, 56, 68, 87, 42, 30, 95]), " +
            "set([0, 2, 3, 7, 10, 14, 16, 19, 21, 27, 28, 29, 31, 35, 38, 40, 43, 49, 52, 55, 62, 82, 93, 96]), " +
            "set([4, 15, 19, 23, 33, 34, 35, 37, 44, 46, 57, 59, 62, 69, 70, 71, 73, 77, 81, 82, 86, 91]), " +
            "set([0, 2, 67, 69, 71, 73, 23, 12, 45, 16, 76, 18, 83, 84, 24, 41, 56, 31, 28, 62, 5]), " +
            "set([0, 11, 13, 17, 22, 26, 36, 38, 41, 44, 61, 62, 63, 74, 77, 80, 81, 85, 86, 88, 90, 94]), " +
            "set([4, 10, 29, 36, 43, 44, 50, 53, 57, 58, 63, 64, 66, 72, 75, 78, 79, 80, 86, 91, 97, 99]), " +
            "set([0, 1, 10, 12, 15, 17, 20, 24, 26, 28, 36, 39, 51, 52, 54, 55, 58, 63, 73, 79, 83, 85, 87, 89, 94]), " +
            "set([15, 17, 19, 23, 26, 27, 33, 34, 45, 46, 47, 49, 51, 53, 62, 63, 68, 69, 72, 74, 80, 81, 84, 94]), " +
            "set([96, 3, 4, 40, 12, 44, 78, 47, 16, 50, 51, 90, 46, 86, 87, 26, 28, 61, 30, 95]), " +
            "set([1, 3, 5, 8, 14, 15, 22, 23, 26, 33, 35, 43, 48, 59, 61, 68, 74, 77, 79, 80, 90, 98]), " +
            "set([1, 3, 13, 24, 25, 26, 30, 33, 37, 43, 49, 58, 63, 66, 67, 71, 76, 77, 78, 79, 90, 91, 97])");
        
        testTM(sequence, "");
        
        Metric unpredictedActiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTraceUnpredictedActiveColumns());
        assertTrue(unpredictedActiveColumnsMetric.mean < 1);
    }
    
    /**
     * Learn two sequences with a short shared pattern.
     * Parameters should be the same as B1.
     * Since cellsPerColumn == 1, it should make more predictions than necessary.
     */
    @Test
    public void testH1() {
        init(null, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            0, 17, 15, 1, 8, 5, 11, 3, 18, 16, 40, 41, 42, 43, 44, 12, 7, 10, 14, 6, -1, 
            39, 36, 35, 25, 24, 32, 34, 27, 23, 26, 40, 41, 42, 43, 44, 28, 37, 31, 20, 21, -1           
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, "", true, 1);
        
        testTM(sequence, "");
        assertAllActiveWerePredicted();
        
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean > 0);
        
        // At the end of both shared sequences, there should be
        // predicted but inactive columns
        assertTrue(predictedInactiveTrace.items.get(14).size() > 0);
        assertTrue(predictedInactiveTrace.items.get(33).size() > 0);
    }
    
    /**
     * Same as H1, but with cellsPerColumn == 4, and train multiple times.
     * It should make just the right number of predictions.
     */
    @Test
    public void testH2() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            0, 17, 15, 1, 8, 5, 11, 3, 18, 16, 40, 41, 42, 43, 44, 12, 7, 10, 14, 6, -1, 
            39, 36, 35, 25, 24, 32, 34, 27, 23, 26, 40, 41, 42, 43, 44, 28, 37, 31, 20, 21, -1           
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        for(int i = 0;i < 10;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        assertAllActiveWerePredicted();
        
        // Without some kind of decay, expect predicted inactive columns at the
        // end of the first shared sequence
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean > 0);
        assertTrue(predictedInactiveColumnsMetric.sum < 26);
        
        // At the end of the second shared sequence, there should be no
        // predicted but inactive columns
        assertTrue(tm.mmGetTracePredictedInactiveColumns().items.get(36).size() == 0);
    }
    
    /**
     * Like H2, except the shared subsequence is in the beginning.
     * (e.g. "ABCDEF" and "ABCGHIJ") At the point where the shared subsequence
     * ends, all possible next patterns should be predicted. As soon as you see
     * the first unique pattern, the predictions should collapse to be a perfect
     * prediction.
     */
    @Test
    public void testH3() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            40, 41, 42, 43, 44, 5, 11, 3, 18, 16, 13, 2, 9, 19, 4, 12, 7, 10, 14, 6, -1, 
            40, 41, 42, 43, 44, 32, 34, 27, 23, 26, 22, 29, 33, 30, 38, 28, 37, 31, 20, 21, -1 
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        feedTM(sequence, "", true, 1);
        
        testTM(sequence, "");
        assertAllActiveWerePredicted();
        
        // Without some kind of decay, expect predicted inactive columns at the
        // end of the first shared sequence
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean > 0);
        assertTrue(predictedInactiveColumnsMetric.sum < 26 * 2);
        
        // At the end of the second shared sequence, there should be
        // predicted but inactive columns
        int i = 0;
        for(Set<Integer> s : tm.mmGetTracePredictedInactiveColumns().items) {
            System.out.println("" + (i++) + "  " + s);
        }
        assertTrue(tm.mmGetTracePredictedInactiveColumns().items.get(4).size() > 0);
        assertTrue(tm.mmGetTracePredictedInactiveColumns().items.get(23).size() > 0);
    }
    
    /**
     * Shared patterns. Similar to H2 except that patterns are shared between
     * sequences.  All sequences are different shufflings of the same set of N
     * patterns (there is no shared subsequence).
     */
    @Test
    public void testH4() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            0, 17, 15, 1, 8, 5, 11, 3, 18, 16, 13, 2, 9, 19, 4, 12, 7, 10, 14, 6, -1, 
            19, 16, 15, 5, 4, 12, 14, 7, 3, 6, 2, 9, 13, 10, 18, 8, 17, 11, 0, 1, -1 
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        for(int i = 0;i < 20;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        assertAllActiveWerePredicted();
        
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean < 3);
    }
    
    /**
     * Combination of H4) and H2).
     * Shared patterns in different sequences, with a shared subsequence.
     */
    @Test
    public void testH5() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        init(p, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] {              
            8, 16, 18, 11, 9, 13, 14, 5, 12, 15, 1, 4, 2, 0, 3, 19, 17, 10, 6, 7, -1, 
            17, 5, 10, 7, 12, 8, 6, 13, 14, 18, 1, 4, 2, 0, 3, 15, 9, 16, 19, 11, -1
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        for(int i = 0;i < 20;i++) {
            feedTM(sequence, "", true, 1);
        }
        
        testTM(sequence, "");
        assertAllActiveWerePredicted();
        
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean < 3);
    }
    
    /**
     * Sensitivity to small amounts of spatial noise during inference
     * (X = 0.05). Parameters the same as B11, and sequences like H2.
     */
    @Test
    public void testH9() {
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 8);
        init(p, PATTERN_MACHINE);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            0, 17, 15, 1, 8, 5, 11, 3, 18, 16, 40, 41, 42, 43, 44, 12, 7, 10, 14, 6, -1, 
            39, 36, 35, 25, 24, 32, 34, 27, 23, 26, 40, 41, 42, 43, 44, 28, 37, 31, 20, 21, -1           
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
        
        for(int i = 0;i < 10;i++) {
            feedTM(sequence, "", true, 1);
        }

        sequence = translateSequence(
            "set([0, 4, 10, 18, 22, 26, 30, 31, 33, 44, 45, 53, 55, 70, 72, 73, 76, 77, 80, 83, 87, 88, 90]), " +
            "set([35, 4, 6, 41, 43, 76, 77, 78, 47, 48, 67, 21, 86, 23, 55, 58, 27, 92, 60, 94, 95]), " +
            "set([1, 10, 13, 17, 18, 19, 28, 30, 31, 35, 36, 37, 44, 53, 66, 71, 75, 80, 81, 84, 86, 90]), " +
            "set([2, 4, 6, 8, 11, 13, 15, 16, 23, 29, 38, 42, 46, 48, 51, 55, 56, 59, 65, 67, 76, 77, 78, 82, 89]), " +
            "set([2, 6, 8, 13, 15, 16, 27, 33, 37, 41, 48, 52, 61, 62, 64, 68, 73, 74, 75, 79, 89, 91, 96, 98]), " +
            "set([0, 4, 5, 9, 12, 17, 25, 34, 38, 39, 41, 42, 44, 56, 65, 68, 69, 74, 76, 77, 83, 88, 93, 96, 99]), " +
            "set([4, 18, 20, 21, 25, 35, 36, 38, 44, 50, 51, 59, 60, 62, 63, 73, 75, 79, 85, 89, 93, 97]), " +
            "set([3, 4, 5, 9, 11, 13, 16, 19, 21, 22, 24, 48, 49, 60, 61, 65, 66, 69, 70, 78, 85, 87, 96]), " +
            "set([4, 7, 11, 12, 14, 19, 20, 23, 26, 27, 31, 35, 40, 50, 54, 56, 58, 65, 75, 81, 84, 85, 87, 97, 98]), " +
            "set([3, 9, 17, 19, 23, 27, 29, 30, 32, 35, 36, 40, 52, 53, 56, 58, 63, 67, 79, 83, 85, 88, 90, 95]), " +
            "set([0, 64, 2, 99, 4, 38, 39, 77, 45, 78, 93, 18, 52, 85, 86, 87, 57, 90, 84, 61, 63]), " +
            "set([0, 1, 4, 6, 10, 11, 15, 18, 22, 30, 37, 43, 44, 47, 53, 58, 60, 61, 69, 74, 75, 78, 80, 90, 94]), " +
            "set([3, 6, 9, 12, 18, 19, 21, 22, 29, 34, 39, 42, 45, 54, 61, 62, 65, 66, 74, 80, 91, 97, 99]), " +
            "set([0, 1, 10, 11, 22, 30, 36, 44, 47, 49, 53, 56, 57, 58, 65, 66, 75, 76, 84, 85, 90, 95, 98]), " +
            "set([96, 1, 67, 35, 36, 37, 73, 42, 77, 61, 80, 92, 19, 98, 25, 26, 59, 60, 29, 95]), " +
            "set([1, 34, 59, 36, 38, 6, 71, 8, 2, 12, 66, 14, 45, 49, 82, 54, 56, 26, 47, 60, 61]), " +
            "set([15, 18, 22, 23, 27, 29, 32, 33, 35, 39, 40, 42, 44, 53, 58, 69, 79, 80, 83, 86, 91, 95, 97]), " +
            "set([4, 5, 7, 11, 14, 16, 40, 45, 46, 47, 53, 58, 60, 67, 69, 71, 73, 75, 77, 79, 85, 86, 89, 92]), " +
            "set([96, 2, 3, 4, 40, 73, 12, 78, 47, 16, 50, 51, 90, 46, 86, 26, 44, 28, 61, 30, 95]), " + 
            "set([1, 3, 14, 17, 19, 25, 26, 27, 28, 35, 36, 48, 49, 55, 58, 67, 69, 72, 77, 80, 85, 93, 98]), None, " +
            "set([3, 17, 18, 21, 24, 27, 34, 35, 37, 40, 44, 47, 50, 58, 59, 60, 62, 64, 66, 69, 77, 79, 82, 95, 98]), " +
            "set([97, 99, 37, 70, 40, 74, 44, 14, 80, 56, 82, 84, 86, 72, 24, 25, 58, 60, 93, 30, 63]), " +
            "set([5, 6, 8, 12, 17, 24, 25, 29, 31, 33, 35, 41, 46, 53, 56, 57, 63, 67, 68, 71, 78, 83, 88]), " + 
            "set([1, 2, 3, 4, 8, 9, 13, 22, 25, 32, 36, 37, 46, 49, 52, 53, 56, 61, 64, 84, 90, 91, 93, 96]), " +
            "set([3, 12, 13, 16, 18, 20, 24, 26, 30, 31, 35, 38, 42, 50, 52, 54, 55, 56, 59, 60, 61, 67, 77, 79, 98]), " +
            "set([5, 7, 15, 22, 23, 25, 27, 40, 42, 47, 49, 50, 56, 58, 59, 62, 69, 77, 82, 89, 92, 96]), " +
            "set([1, 17, 21, 24, 28, 31, 33, 34, 37, 39, 45, 53, 54, 58, 59, 66, 74, 76, 80, 86, 88, 93, 97]), " +
            "set([0, 1, 2, 6, 7, 9, 13, 20, 28, 29, 30, 46, 47, 51, 55, 58, 60, 66, 69, 87, 94, 97]), " +
            "set([1, 7, 12, 13, 22, 25, 26, 31, 38, 40, 44, 58, 61, 62, 67, 70, 71, 73, 84, 85, 92, 97, 98]), " +
            "set([0, 64, 34, 99, 69, 6, 39, 11, 44, 77, 78, 81, 82, 20, 22, 55, 23, 52, 95, 62, 31]), " +
            "set([0, 64, 2, 99, 4, 38, 70, 39, 45, 78, 50, 18, 52, 85, 87, 57, 90, 95, 84, 93, 63]), " +
            "set([4, 6, 8, 10, 11, 18, 22, 23, 30, 35, 37, 43, 44, 47, 53, 58, 60, 61, 66, 69, 74, 75, 80, 90, 94]), " +
            "set([3, 9, 18, 19, 21, 28, 29, 34, 39, 42, 45, 54, 61, 62, 65, 66, 72, 74, 80, 91, 95, 97]), " +
            "set([0, 1, 10, 11, 22, 25, 30, 44, 47, 53, 56, 57, 59, 65, 66, 75, 76, 84, 85, 86, 90, 95, 98]), " +
            "set([5, 13, 18, 19, 23, 25, 26, 29, 35, 36, 37, 42, 48, 57, 59, 60, 67, 77, 92, 95, 96, 98]), " +
            "set([6, 10, 19, 25, 29, 33, 34, 40, 41, 43, 46, 58, 65, 71, 75, 77, 78, 79, 84, 91, 92, 93, 99]), " +
            "set([1, 4, 8, 9, 12, 13, 15, 24, 29, 35, 45, 49, 51, 55, 56, 58, 59, 64, 65, 67, 72, 78, 82, 86, 91]), " +
            "set([5, 6, 7, 11, 17, 22, 23, 25, 27, 28, 30, 35, 38, 42, 52, 66, 67, 68, 75, 78, 82, 87, 88, 93, 99]), " +
            "set([4, 10, 29, 36, 43, 44, 50, 51, 57, 58, 62, 63, 64, 66, 72, 75, 78, 80, 86, 91, 97, 99]), " +
            "set([6, 10, 21, 24, 26, 30, 36, 45, 47, 59, 62, 67, 68, 70, 72, 75, 80, 81, 83, 91, 92, 95, 96])");
        
        
        
        testTM(sequence, "");
        
        IndicesTrace unpredictedActiveTrace = tm.mmGetTraceUnpredictedActiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(unpredictedActiveTrace);
        assertTrue(predictedInactiveColumnsMetric.mean < 3);
    }
    
    /**
     * Orphan Decay mechanism reduce predicted inactive cells (extra predictions).
     * Test feeds in noisy sequences (X = 0.05) to TM with and without orphan decay.
     * TM with orphan decay should has many fewer predicted inactive columns.
     * Parameters the same as B11, and sequences like H9.
     */
    @Test
    public void testH10() {
        // train TM on noisy sequences with orphan decay turned off
        Parameters p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 8);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getPredictedSegmentDecrement() == 0);
        
        // Instead of implementing the Python "shuffle" method, just use the exact output
        Integer[] shuffledNums = new Integer[] { 
            0, 17, 15, 1, 8, 5, 11, 3, 18, 16, 40, 41, 42, 43, 44, 12, 7, 10, 14, 6, -1, 
            39, 36, 35, 25, 24, 32, 34, 27, 23, 26, 40, 41, 42, 43, 44, 28, 37, 31, 20, 21, -1           
        };
        
        List<Integer> numberList = Arrays.asList(shuffledNums);
        List<Set<Integer>> sequence = sequenceMachine.generateFromNumbers(numberList);
         
        List<List<Set<Integer>>> sequenceNoisy = new ArrayList<>();
        for(int i = 0;i < 10;i++) {
            sequenceNoisy.add(sequenceMachine.addSpatialNoise(sequence, 0.05));
            feedTM(sequenceNoisy.get(i), "", true, 1);
        }
        
        testTM(sequence, "");
        
        IndicesTrace predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        double predictedInactiveColumnsMean1 = predictedInactiveColumnsMetric.mean;
        
        p = Parameters.empty();
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 4);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 8);
        p.setParameterByKey(KEY.PREDICTED_SEGMENT_DECREMENT, 0.04);
        init(p, PATTERN_MACHINE);
        
        assertTrue(tm.getConnections().getPredictedSegmentDecrement() == 0.04);
        
        for(int i = 0;i < 10;i++) {
            feedTM(sequenceNoisy.get(0), "", true, 1);
        }
        
        testTM(sequence, "");
        
        predictedInactiveTrace = tm.mmGetTracePredictedInactiveColumns();
        predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(predictedInactiveTrace);
        double predictedInactiveColumnsMean2 = predictedInactiveColumnsMetric.mean;
        
        assertTrue(predictedInactiveColumnsMean1 > 0);
        assertTrue(predictedInactiveColumnsMean1 > predictedInactiveColumnsMean2);
    }
    
    private List<Set<Integer>> translateSequence(String s) {
        List<Set<Integer>> retVal = new ArrayList<>();
        
        s = s.replaceAll("set\\(\\[", "").replaceAll("]", "").replaceAll("\\)$", "").trim();
        String[] sa = s.split("[\\s]*\\)\\,[\\s]*");
        for(String numList : sa) {
            String[] nums = numList.split("[\\s]*\\,[\\s]*");
            Set<Integer> set = Arrays.stream(nums).map(
                n -> n.equals("None") ? -1 : Integer.parseInt(n)).collect(Collectors.toCollection(LinkedHashSet::new));
            retVal.add(set);
        }
        
        List<Set<Integer>> copy = new ArrayList<>(retVal);
        for(int i = 0;i < copy.size();i++) {
            Set<Integer> set = copy.get(i);
            if(set.contains(-1)) {
                retVal.get(i).remove(-1);
                retVal.add(i, SequenceMachine.NONE);
            }
        }
        retVal.add(SequenceMachine.NONE);
        return retVal;
    }
    
    @SuppressWarnings("unused")
    public void feedTM(List<Set<Integer>> sequence, String label, boolean learn, int num) {
        super.feedTM(sequence, label, learn, num);
        
        if(VERBOSITY >= 2) {
            System.out.println(tm.mmPrettyPrintTraces(
                tm.mmGetDefaultTraces(VERBOSITY), null));
            System.out.println("");
        }
        
        if(learn && VERBOSITY >= 3) {
            System.out.println(tm.mmPrettyPrintConnections());
        }
    }
    
    public void testTM(List<Set<Integer>> sequence, String label) {
        feedTM(sequence, label, false, 1);
        
        System.out.println(tm.mmPrettyPrintMetrics(tm.mmGetDefaultMetrics(1), 7));
    }
    
    public void assertAllActiveWerePredicted() {
        Metric unpredictedActiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTraceUnpredictedActiveColumns());
        
        Metric predictedActiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTracePredictedActiveColumns());
        
        assertEquals(unpredictedActiveColumnsMetric.sum, 0, 0);
        assertEquals(predictedActiveColumnsMetric.min, 21, 0);
        assertEquals(predictedActiveColumnsMetric.max, 25, 0);
    }
    
    public void assertAllInactiveWereUnpredicted() {
        Metric predictedInactiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTracePredictedInactiveColumns());
        
        assertEquals(predictedInactiveColumnsMetric.sum, 0, 0);
    }
    
    public void assertAllActiveWereUnpredicted() {
        Metric unpredictedActiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTraceUnpredictedActiveColumns());
        Metric predictedActiveColumnsMetric = tm.mmGetMetricFromTrace(
            tm.mmGetTracePredictedActiveColumns());
        
        assertEquals(predictedActiveColumnsMetric.sum, 0, 0);
        assertEquals(unpredictedActiveColumnsMetric.min, 21, 0);
        assertEquals(unpredictedActiveColumnsMetric.max, 25, 0);
    }
}
