package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;


public class SetSparseMatrixTest {

    @Test
    public void testConstruction() {
        SetSparseMatrix ssm = new SetSparseMatrix(new int[] { 5 });
        assertNotNull(ssm);
        
        SetSparseMatrix ssm2 = new SetSparseMatrix(new int[] { 5 }, false);
        assertNotNull(ssm2);
        
        assertEquals(ssm, ssm2);
    }
    
    @Test
    public void testSetGet() {
        SetSparseMatrix ssm = new SetSparseMatrix(new int[] { 5 });
        ssm.set(new int[] { 3 }, 1);
        assertEquals(1, (int)ssm.get(3));
        assertEquals(0, (int)ssm.get(0));
        assertEquals(0, (int)ssm.get(1));
        assertEquals(0, (int)ssm.get(2));
        assertEquals(0, (int)ssm.get(4));
        assertEquals(0, (int)ssm.get(5));
    }
    
    @Test
    public void testHashCodeAndEquals() {
        SetSparseMatrix ssm = new SetSparseMatrix(new int[] { 5 });
        assertNotNull(ssm);
        
        SetSparseMatrix ssm2 = new SetSparseMatrix(new int[] { 5 }, false);
        assertNotNull(ssm2);
        
        assertEquals(ssm, ssm2);
        
        assertEquals(ssm.hashCode(), ssm2.hashCode());
    }

}
