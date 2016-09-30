package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import no.uib.cipr.matrix.Matrices;
import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;


public class NearestNeighborTest {

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
    
    @Test
    public void testVecLpDist() {
        NearestNeighbor nn = new NearestNeighbor(5, 10);
        assertNull(nn.vecLpDist(0.0, null, false));
    }

    @Test
    public void testRightVecSumAtNZ() {
        int[][] connectedSynapses = new int[][]{
            {1, 0, 0, 0, 0, 1, 0, 0, 0, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 0, 0},
            {0, 0, 1, 0, 0, 0, 0, 1, 0, 0},
            {0, 0, 0, 1, 0, 0, 0, 0, 1, 0},
            {0, 0, 0, 0, 1, 0, 0, 0, 0, 1}};
            
        int[] inputVector = new int[]{1, 0, 1, 0, 1, 0, 1, 0, 1, 0};
        int[] trueResults = new int[]{1, 1, 1, 1, 1};
        
        NearestNeighbor nn = new NearestNeighbor(5, 10);
        int[] result = nn.rightVecSumAtNZ(inputVector, connectedSynapses);
        
        for (int i = 0; i < result.length; i++) {
            assertEquals(trueResults[i], result[i]);
        }
    }

}
