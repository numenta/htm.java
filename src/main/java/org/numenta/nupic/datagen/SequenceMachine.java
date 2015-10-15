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

package org.numenta.nupic.datagen;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

/**
 * Utilities for generating and manipulating sequences, for use in
 * experimentation and tests.
 * 
 * @author David Ray
 */
public class SequenceMachine { 
    private PatternMachine patternMachine;
    
    /**
     * Represents the end of a pattern or sequence when inserted in
     * a {@link Collection}, otherwise the primitive form of "None"
     * is -1
     */
    public static final Set<Integer> NONE = new HashSet<Integer>() {
        private static final long serialVersionUID = 1L;

        public String toString() { return "None"; }
    };
    
    /**
     * Constructs a new {@code SequenceMachine}
     * @param pMachine
     */
    public SequenceMachine(PatternMachine pMachine) {
        this.patternMachine = pMachine;
    }
    
    /**
     * Generates a series of sequences which may optionally contain shared ranges.
     * 
     * @param numSequences
     * @param sequenceLength
     * @param sharedRange
     * @return
     */
    public List<Integer> generateNumbers(int numSequences, int sequenceLength, Tuple sharedRange) {
        List<Integer> numbers = new ArrayList<>();
        
        TIntList sharedNumbers = null;
        int sharedStart = 0;
        int sharedEnd = 0;
        if(sharedRange != null && sharedRange.size() == 2) {
            int sharedLength = (sharedEnd = (int)sharedRange.get(1)) - (sharedStart = (int)sharedRange.get(0));
            sharedNumbers = new TIntArrayList(ArrayUtils.xrange(
                numSequences * sequenceLength, numSequences * sequenceLength + sharedLength, 1));
        }
        
        for(int i = 0;i < numSequences;i++) {
            int start = i * sequenceLength;
            int[] newNumbers = ArrayUtils.xrange(start, start + sequenceLength, 1);
            ArrayUtils.shuffle(newNumbers);
            
            if(sharedRange != null) {
                ArrayUtils.replace(sharedStart, sharedEnd, newNumbers, sharedNumbers.toArray());
            }
            
            numbers.addAll(Arrays.asList(ArrayUtils.toBoxed(newNumbers)));
            numbers.add(-1);
        }
        
        return numbers;
    }
    
    /**
     * Generate a sequence from a list of numbers.
     * 
     * @param numbers
     * @return
     */
    public List<Set<Integer>> generateFromNumbers(List<Integer> numbers) {
        List<Set<Integer>> sequence = new ArrayList<Set<Integer>>();
        for(Integer i : numbers) {
            if(i == -1) {
                sequence.add(NONE);
            }else{
                Set<Integer> pattern = patternMachine.get(i);
                sequence.add(pattern);
            }
        }
        
        return sequence;
    }
    
    /**
     * Add spatial noise to each pattern in the sequence.
     * 
     * @param sequence      List of patterns
     * @param amount        Amount of spatial noise
     * @return  Sequence with noise added to each non-empty pattern
     */
    public List<Set<Integer>> addSpatialNoise(List<Set<Integer>> sequence, double amount) {
        List<Set<Integer>> newSequence = new ArrayList<Set<Integer>>();
        
        for(Set<Integer> pattern : sequence) {
            if(!pattern.equals(NONE)) {
                pattern = patternMachine.addNoise(pattern, amount);
            }
            newSequence.add(pattern);
        }
        
        return newSequence;
    }
    
    /**
     * Pretty print a sequence.
     * 
     * @param sequence      the sequence of numbers to print
     * @param verbosity     the extent of output chatter
     * @return
     */
    public String prettyPrintSequence(List<Set<Integer>> sequence, int verbosity) {
        String text = "";
        
        for(int i = 0;i < sequence.size();i++) {
            Set<Integer> pattern = sequence.get(i);
            if(pattern == NONE) {
                text += "<reset>";
                if(i < sequence.size() - 1) {
                    text += "\n";
                }
            }else{
                text += patternMachine.prettyPrintPattern(pattern, verbosity);
            }
        }
        return text;
    }
     
}
