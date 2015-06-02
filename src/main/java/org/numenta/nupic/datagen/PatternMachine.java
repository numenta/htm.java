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

package org.numenta.nupic.datagen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import org.numenta.nupic.util.MersenneTwister;

/**
 * Utilities for generating and manipulating patterns, for use in
 * experimentation and tests.
 * 
 * @author Chetan Surpur
 * @author David Ray
 *
 * @see ConsecutivePatternMachine
 * @see SequenceMachine
 */
public class PatternMachine {
    protected int numPatterns = 100;
    protected int n;
    protected int w;
    
    protected Random random;
    
    protected Map<Integer, LinkedHashSet<Integer>> patterns;
    
    protected static final int SEED = 42;
    
    
    /**
     * @param n   Number of available bits in pattern
     * @param w   Number of on bits in pattern
     * @param num Number of available patterns
     * 
     * Constructs a new {@code PatternMachine}
     */
    public PatternMachine(int n, int w) {
        this(n, w, SEED);
    }
    
    /**
     * @param n   Number of available bits in pattern
     * @param w   Number of on bits in pattern
     * @param num Number of available patterns
     * 
     * Constructs a new {@code PatternMachine}
     */
    public PatternMachine(int n, int w, int seed) {
        this.n = n;
        this.w = w;
        random = new MersenneTwister(new int[] { seed });
        patterns = new LinkedHashMap<Integer, LinkedHashSet<Integer>>();
        
        generate();
    }
    
    /**
     * Instructs the PatternMachine to construct the internal patterns.
     */
    public void generate() {
        LinkedHashSet<Integer> pattern;
        for(int i = 0;i < numPatterns;i++) {
            pattern = sample(new ArrayList<Integer>(xrange(0, n)), w);
            patterns.put(i, pattern);
        }
    }
    
    /**
     * Returns the ordered set of indices of on bits.
     * 
     * @param key
     * @return
     */
    public LinkedHashSet<Integer> get(int key) {
        return patterns.get(key);
    }
    
    /**
     * Returns a {@link Set} of indexes mapped to patterns
     * which contain the specified bit.
     * 
     * @param bit
     * @return
     */
    public LinkedHashSet<Integer> numbersForBit(int bit) {
        LinkedHashSet<Integer> retVal = new LinkedHashSet<Integer>();
        for(Integer i : patterns.keySet()) {
            if(patterns.get(i).contains(bit)) {
                retVal.add(i);
            }
        }
        
        return retVal;
    }
    
    /**
     * Return a map from number to matching on bits,
     * for all numbers that match a set of bits.
     * 
     * @param bits
     * @return
     */
    public Map<Integer, Set<Integer>> numberMapForBits(Set<Integer> bits) {
        Map<Integer, Set<Integer>> numberMap = new TreeMap<Integer, Set<Integer>>();
        
        for(Integer bit : bits) {
            Set<Integer> numbers = numbersForBit(bit);
            
            for(Integer number : numbers) {
                Set<Integer> set = null;
                if((set = numberMap.get(number)) == null) {
                    numberMap.put(number, set = new HashSet<Integer>());
                }
                set.add(bit);
            }
        }
        
        return numberMap;
    }
    
    public String prettyPrintPattern(Set<Integer> bits, int verbosity) {
        Map<Integer, Set<Integer>> numberMap = numberMapForBits(bits);
        String text = null;
        List<String> numberList = new ArrayList<String>();
        LinkedHashMap<Integer, LinkedHashSet<Integer>> numberItems = sortedMap(numberMap);
        for(Integer number : numberItems.keySet()) {
            String numberText = null;
            if(verbosity > 2) {
                numberText = number + " (bits: " + numberItems.get(number) + ")";
            }else if(verbosity > 1) {
                numberText = number + " (" + numberItems.get(number).size() + "bits)";
            }else{
                numberText = "" + number;
            }
            
            numberList.add(numberText);
        }
        
        text = numberList.toString();
        return text;
    }
    
    /**
     * Returns a sorted map whose set entries are also sorted.
     * 
     * @param map
     * @return
     */
    public LinkedHashMap<Integer, LinkedHashSet<Integer>> sortedMap(final Map<Integer, Set<Integer>> map) {
        LinkedHashMap<Integer, LinkedHashSet<Integer>> retVal = new LinkedHashMap<Integer, LinkedHashSet<Integer>>();
        
        List<Integer> sortByKeys = new ArrayList<Integer>(map.keySet());
        Collections.sort(sortByKeys, new Comparator<Integer>() {
            @Override public int compare(Integer arg0, Integer arg1) {
                int len0 = map.get(arg0).size();
                int len1 = map.get(arg1).size();
                return len0 == len1 ? 0 : len0 > len1 ? -1 : 1;
            }
            
        });
        for(Integer key : sortByKeys) {
            retVal.put(key, new LinkedHashSet<Integer>(map.get(key)));
        }
        
        return retVal;
    }
    
    /**
     * Returns an ordered {@link Set} of Integers starting at 
     * the specified number (start), and ending with the specified
     * number (upperBounds).
     * 
     * @param start
     * @param upperBounds
     * @return
     */
    public LinkedHashSet<Integer> xrange(int start, int upperBounds) {
        LinkedHashSet<Integer> retVal = new LinkedHashSet<Integer>();
        for(int i = start;i < upperBounds;i++) {
            retVal.add(i);
        }
        return retVal;
    }
    
    /**
     * Returns a {@link Set} of numbers whose size equals the 
     * number num and whose values range from 0 to "population".
     * 
     * @param population
     * @param num
     * @return
     */
    public LinkedHashSet<Integer> sample(List<Integer> population, int num) {
        List<Integer> retVal = new ArrayList<Integer>();
        int len = population.size();
        for(int i = 0;i < num;i++) {
            int j = (int)(random.nextDouble() * (len - i));
            retVal.add(population.get(j));
            population.set(j, population.get(len - i - 1));
        }
        
        Collections.sort(retVal);
        return new LinkedHashSet<Integer>(retVal);
    }
    
    
}
