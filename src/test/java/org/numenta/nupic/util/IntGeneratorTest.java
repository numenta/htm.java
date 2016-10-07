/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3 as
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
package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class IntGeneratorTest {

    /**
     * Test that iteration control is managed by the {@link AbstractStuff#exec()}
     * method, and that the execution can be precisely terminated.
     */
    @Test
    public void testIntegerGenerator() {
        int i = 0;
        
        Generator<Integer> generator = IntGenerator.of(0, 31);
        
        for(Integer result : generator) {
            assertNotEquals(result, Integer.valueOf(i - 1));
            assertNotEquals(result, Integer.valueOf(i + 1));
            assertEquals(result, Integer.valueOf(i));
            i++;
        }
        
        assertTrue(i == 31);
        assertFalse(i == 32);
    }
    
    /**
     * Test that iteration control is managed by the {@link AbstractStuff#exec()}
     * method, and that the execution can be precisely terminated.
     */
    @Test
    public void testIntegerGenerator_SpecifyNext() {
        int i = 28;
        
        Generator<Integer> generator = IntGenerator.of(i, 31);
        
        assertFalse(generator.next() == 29);
        
        assertTrue(generator.next() == 29);
        assertTrue(generator.next() == 30);
        assertFalse(generator.hasNext());
    }
    
    @Test
    public void testGet() {
        IntGenerator generator = IntGenerator.of(0, 31);
        assertEquals(0, generator.get());
        assertEquals(0, (int)generator.next());
        assertEquals(1, generator.get());
        assertEquals(1, generator.get());
        assertEquals(1, (int)generator.next());
        assertEquals(2, (int)generator.next());
        assertEquals(3, generator.get());
    }
    
    @Test 
    public void testSize() {
        IntGenerator generator = IntGenerator.of(-4, -4);
        assertEquals(0, generator.size());
        assertEquals(-4, (int)generator.next());
        assertEquals(-4, (int)generator.next());
        assertFalse(generator.hasNext());
    }
    
    @Test
    public void testReset() {
        IntGenerator generator = IntGenerator.of(0, 31);
        assertEquals(0, generator.get());
        assertEquals(0, (int)generator.next());
        assertEquals(1, generator.get());
        assertEquals(1, generator.get());
        assertEquals(1, (int)generator.next());
        assertEquals(2, (int)generator.next());
        assertEquals(3, generator.get());
        
        generator.reset();
        assertEquals(0, generator.get());
        assertEquals(0, (int)generator.next());
        assertEquals(1, generator.get());
        assertEquals(1, generator.get());
        assertEquals(1, (int)generator.next());
        assertEquals(2, (int)generator.next());
        assertEquals(3, generator.get());
    }
}
