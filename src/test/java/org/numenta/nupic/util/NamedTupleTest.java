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

package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.Test;


public class NamedTupleTest {
    
    @Test
    public void testCannotIntantiateAsymmetric() {
        // Test that keys and values are tested for symmetry
        try {
            new NamedTuple(new String[] { "one", "two" }, 1, 2, 3);
            fail();
        }catch(Exception e) {
            assertEquals("Keys and values must be same length.", e.getMessage());
        }
        try {
            new NamedTuple(new String[] { "one", "two", "three" }, 1, 2);
            fail();
        }catch(Exception e) {
            assertEquals("Keys and values must be same length.", e.getMessage());
        }
        
        try {
            new NamedTuple(new String[] { "one", "two", "three" }, 1, 2, 3);
        }catch(Exception e) {
            fail();
        }
    }
    
    @Test 
    public void testHashStrategy() {
        String[][] elementsToAdd = {
            { "ace", "Very good" },
            { "act", "Take action" },
            { "add", "Join (something) to something else" },
            { "age", "Grow old" },
            { "ago", "Before the present" },
            { "aid", "Help, assist, or support" },
            { "aim", "Point or direct" },
            { "air", "Invisible gaseous substance" },
            { "all", "Used to refer to the whole quantity" },
            { "amp", "Unit of measure for the strength of an electrical current" },
            { "and", "Used to connect words" }, 
            { "ant", "A small insect" },
            { "any", "Used to refer to one or some of a thing" },
            { "ape", "A large primate" },
            { "apt", "Appropriate or suitable in the circumstances" },
            { "arc", "A part of the circumference of a curve" },
            { "are", "Unit of measure, equal to 100 square meters" },
            { "ark", "The ship built by Noah" },
            { "arm", "Two upper limbs of the human body" },
            { "art", "Expression or application of human creative skill" },
            { "ash", "Powdery residue left after the burning" },
            { "ask", "Say something in order to obtain information" },
            { "asp", "Small southern European viper" },
            { "ass", "Hoofed mammal" },
            { "ate", "To put (food) into the mouth and swallow it" },
            { "atm", "Unit of pressure" },
            { "awe", "A feeling of reverential respect" },
            { "axe", "Edge tool with a heavy bladed head" },
            { "aye", "An affirmative answer" } 
        };
        
        NamedTuple nt = new NamedTuple(new String[] { "one", "two", "three" }, 1, 2, 3);
        assertEquals(nt.get("one"), 1);
        assertEquals(nt.get("two"), 2);
        assertEquals(nt.get("three"), 3);
        assertNull(nt.get("four"));
        assertNull(nt.get(null));
        assertNull(nt.get(""));
        
        String[] keys = new String[elementsToAdd.length];
        Object[] vals = new Object[elementsToAdd.length];
        int i = 0;
        for(String[] elem : elementsToAdd) {
            keys[i] = elem[0];
            vals[i++] = elem[1];
        }
        nt = new NamedTuple(keys, vals);
        
        for(String[] elem : elementsToAdd) {
            assertEquals(nt.get(elem[0]), elem[1]);
        }
    }
    
    @Test
    public void testEquality() {
        NamedTuple nt = new NamedTuple(new String[] { "one", "two", "three" }, 1, 2, 3);
        NamedTuple nt2 = new NamedTuple(new String[] { "one", "two", "three" }, 1, 2, 3);
        assertEquals(nt, nt2);
        
        //Test inequality due to (colliding) key order
        nt = new NamedTuple(new String[] { "one", "two", "three" }, 1, 2, 3);
        nt2 = new NamedTuple(new String[] { "two", "one", "three" }, 1, 2, 3);
        assertNotEquals(nt, nt2);
        
        //Test inequality due to (non-colliding) key order
        nt = new NamedTuple(new String[] { "one", "different" }, 1, 3);
        nt2 = new NamedTuple(new String[] { "different", "one" }, 3, 1);
        assertNotEquals(nt, nt2);
    }

    @Test
    public void testInterleave() {
        String[] f = { "0" };
        double[] s = { 0.8 };
         
        // Test most simple interleave of equal length arrays
        Object[] result = NamedTuple.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.8, result[1]);
        
        // Test simple interleave of larger array
        f = new String[] { "0", "1" };
        s = new double[] { 0.42, 2.5 };
        result = NamedTuple.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        
        // Test complex interleave of larger array
        f = new String[] { "0", "1", "bob", "harry", "digit", "temperature" };
        s = new double[] { 0.42, 2.5, .001, 1e-2, 34.0, .123 };
        result = NamedTuple.interleave(f, s);
        for(int i = 0, j = 0;j < result.length;i++, j+=2) {
            assertEquals(f[i], result[j]);
            assertEquals(s[i], result[j + 1]);
        }
        
        // Test interleave with zero length of first
        f = new String[0];
        s = new double[] { 0.42, 2.5 };
        result = NamedTuple.interleave(f, s);
        assertEquals(0.42, result[0]);
        assertEquals(2.5, result[1]);
        
        // Test interleave with zero length of second
        f = new String[] { "0", "1" };
        s = new double[0];
        result = NamedTuple.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals("1", result[1]);
        
        // Test complex unequal length: left side smaller
        f = new String[] { "0", "1", "bob" };
        s = new double[] { 0.42, 2.5, .001, 1e-2, 34.0, .123 };
        result = NamedTuple.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        assertEquals("bob", result[4]);
        assertEquals(.001, result[5]);
        assertEquals(1e-2, result[6]);
        assertEquals(34.0, result[7]);
        assertEquals(.123, result[8]);
        
        // Test complex unequal length: right side smaller
        f = new String[] { "0", "1", "bob", "harry", "digit", "temperature" };
        s = new double[] { 0.42, 2.5, .001 };
        result = NamedTuple.interleave(f, s);
        assertEquals("0", result[0]);
        assertEquals(0.42, result[1]);
        assertEquals("1", result[2]);
        assertEquals(2.5, result[3]);
        assertEquals("bob", result[4]);
        assertEquals(.001, result[5]);
        assertEquals("harry", result[6]);
        assertEquals("digit", result[7]);
        assertEquals("temperature", result[8]);
        
        // Negative testing
        try {
            f = null;
            s = new double[] { 0.42, 2.5, .001 };
            result = NamedTuple.interleave(f, s);
            fail();
        }catch(Exception e) {
            assertEquals(NullPointerException.class, e.getClass());
        }
    }
    
    @Test
    public void testGetValues() {
        Set<Integer> set = new LinkedHashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        
        NamedTuple nt = new NamedTuple(new String[] { "one", "two", "three" }, set.toArray());
        Collection<?> values = nt.values();
        assertTrue(values.size() == 3);
        assertTrue(set.containsAll(values) && values.containsAll(set));
    }
}
