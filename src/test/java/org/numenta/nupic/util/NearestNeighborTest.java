package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;


public class NearestNeighborTest {

    @Test
    public void testInstantiation() {
        new NearestNeighbor(40, true);
        
        try {
            new NearestNeighbor(0, true);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("Input width must be greater than 0.", e.getMessage());
        }
    }
    
    @Test
    public void testAddRow() {
//        double[] sample = new double[] { 0, 1, 3, 7, 11 };
//        NearestNeighbor nn = new NearestNeighbor(40, true);
//        nn.addRow(sample);
//        double[] data = nn.getRow(0);
//        System.out.println("data = " + Arrays.toString(data));
//        //assertTrue(Arrays.equals(sample, data));
//        System.out.println("rows = " + nn.size());
//        nn.addRow(new double[] { 0, 1, 3, 7, 11 });
//        System.out.println("rows = " + nn.size());
//        nn.addRow(new double[] { 0, 1, 3, 7, 11 });
//        System.out.println("rows = " + nn.size());
        
        FlexCompRowMatrix matrix = new FlexCompRowMatrix(1, 40);
        System.out.println("matrix = " + Matrices.cardinality(matrix));
        matrix.add(0, 2, 1);
        System.out.println("matrix = " + Matrices.cardinality(matrix));
        matrix.add(0, 4, 1);
        System.out.println("matrix = " + Matrices.cardinality(matrix));
        matrix.add(0, 3, 0);
        System.out.println("matrix = " + Matrices.cardinality(matrix));
    }

}
