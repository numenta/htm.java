/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014-2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.Test;
import org.numenta.nupic.util.GroupBy2.Slot;

import chaschev.lang.Pair;

public class GroupBy2Test {
    
    private List<Slot<?>> none = Arrays.asList(Slot.empty());
    
    public List<Integer> list(int i) {
        return Arrays.asList(new Integer[] { i });
    }
    
    public List<Integer> list(int i, int j) {
        return Arrays.asList(new Integer[] { i, j });
    }
    
    @Test
    public void testOneSequence() {
        List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 12, 16 });
        
        Function<Integer, Integer> identity = Function.identity();
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GroupBy2<Integer> m = GroupBy2.of(
            new Pair(sequence0, identity));
        
        List<Tuple> expectedValues = Arrays.asList(new Tuple[] { 
            new Tuple(7, list(7)),
            new Tuple(12, list(12, 12)),
            new Tuple(16, list(16))
        });
        
        int i = 0;
        for(Tuple t : m) {
            int j = 0;
            for(Object o : t.all()) {
                assertEquals(o, expectedValues.get(i).get(j));
                j++;
            }
            i++;
        }
    }

    @Test
    public void testTwoSequences() {
        List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 16 });
        List<Integer> sequence1 = Arrays.asList(new Integer[] { 3, 4, 5 });
        
        Function<Integer, Integer> identity = Function.identity();
        Function<Integer, Integer> times3 = x -> x * 3;
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GroupBy2<Integer> m = GroupBy2.of(
            new Pair(sequence0, identity), 
            new Pair(sequence1, times3));
        
        List<Tuple> expectedValues = Arrays.asList(new Tuple[] { 
            new Tuple(7, list(7), none),
            new Tuple(9, none, list(3)),
            new Tuple(12, list(12), list(4)),
            new Tuple(15, none, list(5)),
            new Tuple(16, list(16), none)
        });
        
        int i = 0;
        for(Tuple t : m) {
            int j = 0;
            for(Object o : t.all()) {
                assertEquals(o, expectedValues.get(i).get(j));
                j++;
            }
            i++;
        }
    }
        
    @Test
    public void testThreeSequences() {
        List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 16 });
        List<Integer> sequence1 = Arrays.asList(new Integer[] { 3, 4, 5 });
        List<Integer> sequence2 = Arrays.asList(new Integer[] { 3, 3, 4, 5 });
        
        Function<Integer, Integer> identity = x -> x;//Function.identity();
        Function<Integer, Integer> times3 = x -> x * 3;
        Function<Integer, Integer> times4 = x -> x * 4;
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GroupBy2<Integer> m = GroupBy2.of(
            new Pair(sequence0, identity), 
            new Pair(sequence1, times3),
            new Pair(sequence2, times4));
        
        List<Tuple> expectedValues = Arrays.asList(new Tuple[] { 
            new Tuple(7, list(7), none, none),
            new Tuple(9, none, list(3), none),
            new Tuple(12, list(12), list(4), list(3, 3)),
            new Tuple(15, none, list(5), none),
            new Tuple(16, list(16), none, list(4)),
            new Tuple(20, none, none, list(5))
        });
        
        int i = 0;
        for(Tuple t : m) {
            int j = 0;
            for(Object o : t.all()) {
                assertEquals(o, expectedValues.get(i).get(j));
                j++;
            }
            i++;
        }
    }
    
    @Test
    public void testFourSequences() {
        List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 16 });
        List<Integer> sequence1 = Arrays.asList(new Integer[] { 3, 4, 5 });
        List<Integer> sequence2 = Arrays.asList(new Integer[] { 3, 3, 4, 5 });
        List<Integer> sequence3 = Arrays.asList(new Integer[] { 3, 3, 4, 5 });
        
        Function<Integer, Integer> identity = Function.identity();
        Function<Integer, Integer> times3 = x -> x * 3;
        Function<Integer, Integer> times4 = x -> x * 4;
        Function<Integer, Integer> times5 = x -> x * 5;
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GroupBy2<Integer> m = GroupBy2.of(
            new Pair(sequence0, identity), 
            new Pair(sequence1, times3),
            new Pair(sequence2, times4),
            new Pair(sequence3, times5));
        
        List<Tuple> expectedValues = Arrays.asList(new Tuple[] { 
            new Tuple(7, list(7), none, none, none),
            new Tuple(9, none, list(3), none, none),
            new Tuple(12, list(12), list(4), list(3, 3), none),
            new Tuple(15, none, list(5), none, list(3, 3)),
            new Tuple(16, list(16), none, list(4), none),
            new Tuple(20, none, none, list(5), list(4)),
            new Tuple(25, none, none, none, list(5))
        });
        
        int i = 0;
        for(Tuple t : m) {
            int j = 0;
            for(Object o : t.all()) {
                assertEquals(o, expectedValues.get(i).get(j));
                j++;
            }
            i++;
        }
    }
    
    @Test
    public void testFiveSequences() {
        List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 16 });
        List<Integer> sequence1 = Arrays.asList(new Integer[] { 3, 4, 5 });
        List<Integer> sequence2 = Arrays.asList(new Integer[] { 3, 3, 4, 5 });
        List<Integer> sequence3 = Arrays.asList(new Integer[] { 3, 3, 4, 5 });
        List<Integer> sequence4 = Arrays.asList(new Integer[] { 2, 2, 3 });
        
        Function<Integer, Integer> identity = Function.identity();
        Function<Integer, Integer> times3 = x -> x * 3;
        Function<Integer, Integer> times4 = x -> x * 4;
        Function<Integer, Integer> times5 = x -> x * 5;
        Function<Integer, Integer> times6 = x -> x * 6;
        
        @SuppressWarnings({ "unchecked", "rawtypes" })
        GroupBy2<Integer> m = GroupBy2.of(
            new Pair(sequence0, identity), 
            new Pair(sequence1, times3),
            new Pair(sequence2, times4),
            new Pair(sequence3, times5),
            new Pair(sequence4, times6));
        
        List<Tuple> expectedValues = Arrays.asList(new Tuple[] { 
            new Tuple(7, list(7), none, none, none, none),
            new Tuple(9, none, list(3), none, none, none),
            new Tuple(12, list(12), list(4), list(3, 3), none, list(2, 2)),
            new Tuple(15, none, list(5), none, list(3, 3), none),
            new Tuple(16, list(16), none, list(4), none, none),
            new Tuple(18, none, none, none, none, list(3)),
            new Tuple(20, none, none, list(5), list(4), none),
            new Tuple(25, none, none, none, list(5), none)
        });
        
        int i = 0;
        for(Tuple t : m) {
            int j = 0;
            for(Object o : t.all()) {
                assertEquals(o, expectedValues.get(i).get(j));
                j++;
            }
            i++;
        }
    }

}
