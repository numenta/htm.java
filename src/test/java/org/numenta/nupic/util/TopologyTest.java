/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TopologyTest {

    @Test
    public void testIndexFromCoordinates() {
        Topology t = new Topology(new int[] { 100 });
        assertEquals(0, t.indexFromCoordinates(new int[] { 0 }));
        assertEquals(50, t.indexFromCoordinates(new int[] { 50 }));
        assertEquals(99, t.indexFromCoordinates(new int[] { 99 }));
        
        t = new Topology(new int[] { 100, 80 });
        assertEquals(0, t.indexFromCoordinates(new int[] { 0, 0 }));
        assertEquals(10, t.indexFromCoordinates(new int[] { 0, 10 }));
        assertEquals(80, t.indexFromCoordinates(new int[] { 1, 0 }));
        assertEquals(90, t.indexFromCoordinates(new int[] { 1, 10 }));
        
        t = new Topology(new int[] { 100, 10, 8 });
        assertEquals(0, t.indexFromCoordinates(new int[] { 0, 0, 0 }));
        assertEquals(7, t.indexFromCoordinates(new int[] { 0, 0, 7 }));
        assertEquals(8, t.indexFromCoordinates(new int[] { 0, 1, 0 }));
        assertEquals(80, t.indexFromCoordinates(new int[] { 1, 0, 0 }));
        assertEquals(88, t.indexFromCoordinates(new int[] { 1, 1, 0 }));
        assertEquals(89, t.indexFromCoordinates(new int[] { 1, 1, 1 }));
    }
    
    @Test
    public void testCoordinateFromIndex() {
        Topology t = new Topology(new int[] { 100 });
        assertArrayEquals(new int[] { 0 }, t.coordinatesFromIndex(0));
        assertArrayEquals(new int[] { 50 }, t.coordinatesFromIndex(50));
        assertArrayEquals(new int[] { 99 }, t.coordinatesFromIndex(99));
        
        t = new Topology(new int[] { 100, 80 });
        assertArrayEquals(new int[] { 0, 0 }, t.coordinatesFromIndex(0));
        assertArrayEquals(new int[] { 0, 10 }, t.coordinatesFromIndex(10));
        assertArrayEquals(new int[] { 1, 0 }, t.coordinatesFromIndex(80));
        assertArrayEquals(new int[] { 1, 10 }, t.coordinatesFromIndex(90));
        
        t = new Topology(new int[] { 100, 10, 8 });
        assertArrayEquals(new int[] { 0, 0, 0 }, t.coordinatesFromIndex(0));
        assertArrayEquals(new int[] { 0, 0, 7 }, t.coordinatesFromIndex(7));
        assertArrayEquals(new int[] { 0, 1, 0 }, t.coordinatesFromIndex(8));
        assertArrayEquals(new int[] { 1, 0, 0 }, t.coordinatesFromIndex(80));
        assertArrayEquals(new int[] { 1, 1, 0 }, t.coordinatesFromIndex(88));
        assertArrayEquals(new int[] { 1, 1, 1 }, t.coordinatesFromIndex(89));
    }
    
    
    /////////////////////////////////////////
    //            NEIGHBORHOOD             //
    /////////////////////////////////////////
    
    private void expectNeighborhoodIndices(Topology t, int[] centerCoords, int radius, int[] expected) {
        int centerIndex = t.indexFromCoordinates(centerCoords);
        
        int numIndices = 0;
        int index = 0;
        for(int actual : t.neighborhood(centerIndex, radius)) {
            numIndices++;
            assertEquals(expected[index], actual);
            index++;
        }
        
        assertEquals(expected.length, numIndices);
    }
    
    private void expectNeighborhoodCoords(Topology t, int[] centerCoords, int radius, int[]... expected) {
        int centerIndex = t.indexFromCoordinates(centerCoords);
        
        int numIndices = 0;
        
        int index = 0;
        for(int actual : t.neighborhood(centerIndex, radius)) {
            numIndices++;
            assertEquals(t.indexFromCoordinates(expected[index]), actual);
            index++;
        }
        
        assertEquals(expected.length, numIndices);
    }
    
    @Test
    public void testNeighborhoodOfOrigin1D() {
        Topology t = new Topology(100);
        int radius = 2;
        int[] expected = { 0, 1, 2 };
        expectNeighborhoodIndices(t, new int[] { 0 }, radius, expected);
    }

    @Test
    public void testNeighborhoodOfOrigin2D() {
        Topology t = new Topology(100, 80);
        int radius = 2;
        int[][] expected = 
            {{ 0, 0 }, { 0, 1 }, { 0, 2 },
            {1, 0 }, { 1, 1 }, { 1, 2 },
            {2, 0 }, { 2, 1 }, { 2, 2 }};
        expectNeighborhoodCoords(t, new int[] { 0, 0 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodOfOrigin3D() {
        Topology t = new Topology(100, 80, 60);
        int radius = 1;
        int[][] expected = 
            {{ 0, 0, 0 }, { 0, 0, 1 },
            { 0, 1, 0 }, { 0, 1, 1 },
            { 1, 0, 0 }, { 1, 0, 1 }, 
            { 1, 1, 0 }, { 1, 1, 1 }};
        expectNeighborhoodCoords(t, new int[] { 0, 0, 0 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodInMiddle1D() {
        Topology t = new Topology(100);
        int radius = 1;
        int[] expected = 
            { 49, 50, 51};
        expectNeighborhoodIndices(t, new int[] { 50 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodOfMiddle2D() {
        Topology t = new Topology(100, 80);
        int radius = 1;
        int[][] expected = 
            {{ 49, 49 }, { 49, 50 }, { 49, 51 },
             { 50, 49 }, { 50, 50 }, { 50, 51 },
             { 51, 49 }, { 51, 50 }, { 51, 51 }};
        expectNeighborhoodCoords(t, new int[] { 50, 50 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodOfEnd2D() {
        Topology t = new Topology(100, 80);
        int radius = 2;
        int[][] expected = 
            {{ 97, 77 }, { 97, 78 }, { 97, 79 },
             { 98, 77 }, { 98, 78 }, { 98, 79 },
             { 99, 77 }, { 99, 78 }, { 99, 79 }};
        expectNeighborhoodCoords(t, new int[] { 99, 79 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodWiderThanWorld() {
        Topology t = new Topology(3, 2);
        int radius = 3;
        int[][] expected = 
            {{ 0, 0 }, { 0, 1 },
             { 1, 0 }, { 1, 1 },
             { 2, 0 }, { 2, 1 }};
        expectNeighborhoodCoords(t, new int[] { 0, 0 }, radius, expected);
    }
    
    @Test
    public void testNeighborhoodRadiusZero() {
        Topology t = new Topology(100);
        int radius = 0;
        int[] expected = 
            { 0 };
        expectNeighborhoodIndices(t, new int[] { 0 }, radius, expected);
        
        t = new Topology(100, 80);
        radius = 0;
        int[][] expected2D = 
            {{ 0, 0 }};
        expectNeighborhoodCoords(t, new int[] { 0, 0 }, radius, expected2D);
        
        t = new Topology(100, 80, 60);
        radius = 0;
        expected2D = 
            new int[][] {{ 0, 0, 0 }};
        expectNeighborhoodCoords(t, new int[] { 0, 0, 0 }, radius, expected2D);
    }
    
    @Test
    public void testNeighborhoodDimensionOne() {
        Topology t = new Topology(10, 1);
        int radius = 1;
        int[][] expected = { { 4, 0 }, { 5, 0 }, { 6, 0 }};
        expectNeighborhoodCoords(t, new int[] { 5, 0 }, radius, expected);
        
        t = new Topology(10, 1, 1);
        radius = 1;
        expected = new int[][] { { 4, 0, 0 }, { 5, 0, 0 }, { 6, 0, 0 }};
        expectNeighborhoodCoords(t, new int[] { 5, 0, 0 }, radius, expected);
    }
    
    
    /////////////////////////////////////////
    //        WRAPPING NEIGHBORHOOD        //
    /////////////////////////////////////////
    
    private void expectWrappingNeighborhoodIndices(Topology t, int[] centerCoords, int radius, int[] expected) {
        int centerIndex = t.indexFromCoordinates(centerCoords);
        
        int numIndices = 0;
        int index = 0;
        for(int actual : t.wrappingNeighborhood(centerIndex, radius)) {
            numIndices++;
            assertEquals(expected[index], actual);
            index++;
        }
        
        assertEquals(expected.length, numIndices);
    }
    
    private void expectWrappingNeighborhoodCoords(Topology t, int[] centerCoords, int radius, int[]... expected) {
        int centerIndex = t.indexFromCoordinates(centerCoords);
        
        int numIndices = 0;
        
        int index = 0;
        for(int actual : t.wrappingNeighborhood(centerIndex, radius)) {
            numIndices++;
            assertEquals(t.indexFromCoordinates(expected[index]), actual);
            index++;
        }
        
        assertEquals(expected.length, numIndices);
    }
    
    @Test
    public void testWrappingNeighborhoodOfOrigin1D() {
        Topology t = new Topology(100);
        int radius = 1;
        int[] expected = { 99, 0, 1 };
        expectWrappingNeighborhoodIndices(t, new int[] { 0 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodOfOrigin2D() {
        Topology t = new Topology(100, 80);
        int radius = 1;
        int[][] expected = {{ 99, 79 }, { 99, 0 }, { 99, 1 },
                          { 0, 79 }, { 0, 0 }, { 0, 1 },
                          { 1, 79 }, { 1, 0 }, { 1, 1 }};
        expectWrappingNeighborhoodCoords(t, new int[] { 0, 0 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodOfOrigin3D() {
        Topology t = new Topology(100, 80, 60);
        int radius = 1;
        int[][] expected = {{99, 79, 59}, {99, 79, 0}, {99, 79, 1},
                            {99, 0, 59}, {99, 0, 0}, {99, 0, 1},
                            {99, 1, 59}, {99, 1, 0}, {99, 1, 1},
                            {0, 79, 59}, {0, 79, 0}, {0, 79, 1},
                            {0, 0, 59}, {0, 0, 0}, {0, 0, 1},
                            {0, 1, 59}, {0, 1, 0}, {0, 1, 1},
                            {1, 79, 59}, {1, 79, 0}, {1, 79, 1},
                            {1, 0, 59}, {1, 0, 0}, {1, 0, 1},
                            {1, 1, 59}, {1, 1, 0}, {1, 1, 1}};
        expectWrappingNeighborhoodCoords(t, new int[] { 0, 0, 0 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodInMiddle1D() {
        Topology t = new Topology(100);
        int radius = 1;
        int[] expected = { 49, 50, 51 };
        expectWrappingNeighborhoodIndices(t, new int[] { 50 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodOfMiddle2D() {
        Topology t = new Topology(100, 80);
        int radius = 1;
        int[][] expected = {{49, 49}, {49, 50}, {49, 51},
                            {50, 49}, {50, 50}, {50, 51},
                            {51, 49}, {51, 50}, {51, 51}};
        expectWrappingNeighborhoodCoords(t, new int[] { 50, 50 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodOfEnd2D() {
        Topology t = new Topology(100, 80);
        int radius = 1;
        int[][] expected = {{98, 78}, {98, 79}, {98, 0},
                            {99, 78}, {99, 79}, {99, 0},
                            {0, 78}, {0, 79}, {0, 0}};
        expectWrappingNeighborhoodCoords(t, new int[] { 99, 79 }, radius, expected);
    }
    
    @Test
    public void testWrappingNeighborhoodRadiusZero() {
        Topology t = new Topology(100);
        int radius = 0;
        int[] expected = 
            { 0 };
        expectWrappingNeighborhoodIndices(t, new int[] { 0 }, radius, expected);
        
        t = new Topology(100, 80);
        radius = 0;
        int[][] expected2D = 
            {{ 0, 0 }};
        expectWrappingNeighborhoodCoords(t, new int[] { 0, 0 }, radius, expected2D);
        
        t = new Topology(100, 80, 60);
        radius = 0;
        expected2D = 
            new int[][] {{ 0, 0, 0 }};
        expectWrappingNeighborhoodCoords(t, new int[] { 0, 0, 0 }, radius, expected2D);
    }
    
    @Test
    public void testWrappingNeighborhoodDimensionOne() {
        Topology t = new Topology(10, 1);
        int radius = 1;
        int[][] expected = { { 4, 0 }, { 5, 0 }, { 6, 0 }};
        expectWrappingNeighborhoodCoords(t, new int[] { 5, 0 }, radius, expected);
        
        t = new Topology(10, 1, 1);
        radius = 1;
        expected = new int[][] { { 4, 0, 0 }, { 5, 0, 0 }, { 6, 0, 0 }};
        expectWrappingNeighborhoodCoords(t, new int[] { 5, 0, 0 }, radius, expected);
    }
}
