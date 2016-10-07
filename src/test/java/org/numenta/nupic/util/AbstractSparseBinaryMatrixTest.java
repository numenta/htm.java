package org.numenta.nupic.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;

import org.junit.Test;

import gnu.trove.list.array.TIntArrayList;

public class AbstractSparseBinaryMatrixTest {

    public AbstractSparseBinaryMatrix getTestMatrix() {
        AbstractSparseBinaryMatrix matrix = new AbstractSparseBinaryMatrix(new int[] { 2, 2, 2 }) {
            private static final long serialVersionUID = 1L;
            private int value1;
            private int value2;

            @Override
            public AbstractFlatMatrix<Integer> set(int index, Object value) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public AbstractSparseBinaryMatrix setForTest(int index, int value) {
                // TODO Auto-generated method stub
                if(index == 0) value1 = value;
                else value2 = value;
                return this;
            }
            
            @Override
            public AbstractSparseBinaryMatrix set(int value, int... coordinates) {
                // TODO Auto-generated method stub
                if(Arrays.toString(coordinates).equals("[0, 0, 0]")) {
                    value1 = value;
                    if(value1 == 1) {
                        setTrueCount(0, 1);
                    }
                }else{
                    value2 = value;
                    if(value2 == 1) {
                        setTrueCount(1, 1);
                    }
                }
                return this;
            }
            
            @Override
            public void rightVecSumAtNZ(int[] inputVector, int[] results, double stimulusThreshold) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public void rightVecSumAtNZ(int[] inputVector, int[] results) {
                // TODO Auto-generated method stub
                
            }
            
            @Override
            public Object getSlice(int... coordinates) {
                // TODO Auto-generated method stub
                return null;
            }
            
            @Override
            public Integer get(int index) {
                return index == 0 ? value1 : value2;
            }
        };
        
        return matrix;
    }
    
    @Test
    public void testGetSliceIndexes() {
        // Test handling of out of bounds coordinates
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        try {
            matrix.getSliceIndexes(new int[] { 1, 1, 1 });
            fail();
        }catch(Exception e) {
            assertEquals("This method only returns the array holding the specified maximum index: [2, 2, 2]", e.getMessage());
        }
        
        int[] sliceIndexes = matrix.getSliceIndexes(new int[] { 0 });
        assertArrayEquals(new int[] {0, 1, 2, 3}, sliceIndexes);
    }
    
    @Test
    public void setIndexes() {
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        boolean isTest = false;
        matrix.set(new int[] { 0, 1 }, new int[] { 33, 44 }, isTest);
        assertEquals(33, (int)matrix.get(0));
        assertEquals(44, (int)matrix.get(1));
        
        matrix = getTestMatrix();
        isTest = true;
        matrix.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        assertEquals(1, (int)matrix.get(0));
        assertEquals(1, (int)matrix.get(1));
    }
    
    @Test
    public void clearIndexes() {
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        boolean isTest = false;
        matrix.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        assertEquals(1, matrix.getTrueCount(0));
        matrix.clearStatistics(0);
        assertEquals(0, matrix.getTrueCount(0));
    }
    
    @Test
    public void testOr() {
        AbstractSparseBinaryMatrix matrix2 = getTestMatrix();
        boolean isTest = true;
        matrix2.set(new int[] { 1 }, new int[] { 1 }, isTest);
        
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        assertEquals(0, matrix.getTrueCount(1));
        assertEquals(0, matrix.getSparseIndices().length);
        
        matrix.or(matrix2);
        assertEquals(1, matrix.getTrueCount(1));
        assertEquals(7, matrix.getSparseIndices().length);
        
        // Now for trove collection
        matrix = getTestMatrix();
        assertEquals(0, matrix.getTrueCount(1));
        assertEquals(0, matrix.getSparseIndices().length);
        
        TIntArrayList tl = new TIntArrayList();
        tl.add(1);
        matrix.or(tl);
        assertEquals(1, matrix.getTrueCount(1));
        assertEquals(7, matrix.getSparseIndices().length);
        
    }
    
    @Test
    public void testAll() {
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        AbstractSparseBinaryMatrix matrix2 = getTestMatrix();
        assertTrue(matrix.all(matrix2));
        
        boolean isTest = true;
        matrix2.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        assertFalse(matrix.all(matrix2));
        
        // Now with trove
        matrix = getTestMatrix();
        matrix2 = getTestMatrix();
        assertTrue(matrix.all(matrix2));
        
        matrix2.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        TIntArrayList tl = new TIntArrayList();
        tl.add(1);
        assertFalse(matrix.all(tl));
    }
    
    @Test
    public void testAny() {
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        AbstractSparseBinaryMatrix matrix2 = getTestMatrix();
        assertFalse(matrix.any(matrix2));
        
        boolean isTest = true;
        matrix2.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        assertFalse(matrix.any(matrix2));
        
        // Now with trove
        matrix = getTestMatrix();
        matrix2 = getTestMatrix();
        assertFalse(matrix.any(matrix2));
        
        matrix2.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        TIntArrayList tl = new TIntArrayList();
        tl.add(1);
        assertFalse(matrix.any(tl));
        assertTrue(matrix2.any(tl));
        
        int[] onBits = { 0 };
        assertFalse(matrix.any(onBits));
        assertTrue(matrix2.any(onBits));
    }
    
    @Test
    public void testEquals() {
        AbstractSparseBinaryMatrix matrix = getTestMatrix();
        AbstractSparseBinaryMatrix matrix2 = getTestMatrix();
        
        assertTrue(matrix.equals(matrix));
        
        assertFalse(matrix.equals(new Object()));
        
        boolean isTest = false;
        matrix2.set(new int[] { 0, 1 }, new int[] { 1, 1 }, isTest);
        assertFalse(matrix.equals(matrix2));
    }

}
