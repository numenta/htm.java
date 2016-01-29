package org.numenta.nupic.util;



public class NearestNeighbor {
    
    private boolean isSparse;
    
    
    /**
     * Constructs a new {@code NearestNeighbor} matrix
     * @param inputWidth
     * @param isSparse
     */
    public NearestNeighbor(int inputWidth, boolean isSparse) {
        if(inputWidth < 1) {
            throw new IllegalArgumentException("Input width must be greater than 0.");
        }
        
        this.isSparse = isSparse;   
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
