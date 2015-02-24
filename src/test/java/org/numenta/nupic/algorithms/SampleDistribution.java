package org.numenta.nupic.algorithms;

import java.util.Random;

/**
 * Generate a number of samples from a given configured
 * distribution
 * 
 * @author David Ray
 */
public class SampleDistribution {
    private double mean;
    private double variance;
    private int size;
    
    public SampleDistribution(double mean, double variance, int size) {
        this.mean = mean;
        this.variance = variance;
        this.size = size;
    }
    
    /**
     * Returns an array of normally distributed values with the configured 
     * mean and variance.
     * 
     * @return
     */
    public double[] getSample(Random random) {
        double[] sample = new double[size];
        for(int i = 0;i < size;i++) {
            sample[i] = getGaussian(random, mean, variance);
        }
        
        return sample;
    }
    
    /**
     * Return the next distributed value with the specified
     * mean and variance.
     * 
     * @param aMean         the centered location
     * @param aVariance     the 
     * @return
     */
    private double getGaussian(Random random, double mean, double variance){
        return mean + random.nextGaussian() * variance;
    }
}
