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

public class AbstractGeneratorTest {

    /////////////////////////////////
    //       Utility Methods       //
    /////////////////////////////////
    /**
     * Returns an {@link AbstractGenerator} that runs for 30 iterations
     * 
     * @return  an {@link AbstractGenerator} that runs for 30 iterations
     */
    private Generator<Integer> getTerminableGenerator() {
        @SuppressWarnings("serial")
        class TerminableGenerator extends AbstractGenerator<Integer> {
            int i = 0;
            public void exec() {
                while(i < 31) {
                    yield(new Integer(i++));
                }
            }
            
            public boolean isConsumed() { 
                return i > 30;
            }
        }
        
        return new TerminableGenerator();
    }
    
    /**
     * Returns an {@link AbstractGenerator} that runs infinitely
     * until halted.
     * 
     * @return  an {@link AbstractGenerator} that runs infinitely
     *          until halted.
     */
    private Generator<Integer> getInfiniteGenerator() {
        @SuppressWarnings("serial")
        class InfiniteGenerator extends AbstractGenerator<Integer> {
            int i = 0;
            public void exec() {
                while(true) {
                    if(haltRequested()) {
                        break;
                    }
                    
                    yield(new Integer(i++));
                }
            }
            
            public boolean isConsumed() { 
                return false;
            }
        }
        
        return new InfiniteGenerator();
    }
    
    
    /////////////////////////////////
    //          Test Methods       //
    /////////////////////////////////
    /**
     * Test that iteration control is managed by the {@link AbstractGenerator#exec()}
     * method, and that the execution can be precisely terminated.
     */
    @Test
    public void testTerminableGenerator() {
        int i = 0;
        
        Generator<Integer> generator = getTerminableGenerator();
        
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
     * Test that we can create an generator that can run infinitely, but
     * still have its execution time managed by the methods on {@link AbstractGenerator}
     */
    @Test
    public void testInfiniteGenerator() {
        int i = 0;
        
        Generator<Integer> generator = getInfiniteGenerator();
        
        for(Integer result : generator) {
            if(result == 30) {
                generator.halt();
                break;
            }
            
            assertNotEquals(result, Integer.valueOf(i - 1));
            assertNotEquals(result, Integer.valueOf(i + 1));
            assertEquals(result, Integer.valueOf(i));
            i++;
        }
        
        assertTrue(i == 30);
        assertFalse(i == 31);
    }

}
