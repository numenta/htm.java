package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class NearestNeighborTest {
    
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
