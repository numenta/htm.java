package org.numenta.nupic.util;



public class NearestNeighbor {
    //private LinkedList

    /**
     * Creates a new {@code NearestNeighbor} with the specified
     * rows. Rows must be 0 or greater, and cols must be greater
     * than zero (i.e. NearestNeighbor(0, 40) is ok).
     * 
     * @param rows      (optional) number of rows
     * @param cols      number of columns
     */
    public NearestNeighbor(int rows, int cols) {
        
    }
    
    public double[] vecLpDist(double distanceNorm, int[] inputPattern, boolean takeRoot) {
        return null;
    }
    
    public int[] rightVecSumAtNZ(int[] inputVector, int[][] base) {
        int[] results = new int[base.length];
        for (int i = 0; i < base.length; i++) {
            for (int j = 0;j < base[i].length;j++) {
                if (inputVector[j] != 0)
                    results[i] += (inputVector[j] * base[i][j]);
            }
        }
        return results;
    }
}
