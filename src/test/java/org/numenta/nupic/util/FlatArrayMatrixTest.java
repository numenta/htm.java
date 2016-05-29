package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


public class FlatArrayMatrixTest {

    @Test
    public void testHashCodeAndEquals() {
        FlatArrayMatrix<?> fam = new FlatArrayMatrix<>(new int[] { 5 });
        assertNotNull(fam);
        
        FlatArrayMatrix<?> fam2 = new FlatArrayMatrix<>(new int[] { 5 }, false);
        assertNotNull(fam2);
        
        assertEquals(fam, fam2);
        
        assertEquals(fam.hashCode(), fam2.hashCode());
    }

}
