package org.numenta.nupic.algorithms;

import org.junit.Test;
import org.numenta.nupic.Constants;
import org.numenta.nupic.datagen.KNNDataArray;
import org.numenta.nupic.datagen.PCAKNNData;


public class KNNClassifierIntegrationTest {

    private PCAKNNData knnData = new PCAKNNData();
    
    private static final int TRAIN = 0;
    private static final int TEST = 1;
    
    @Test
    public void testPCAKNNShort() {
        runTestPCAKNN(0);
    }
    
    private void runTestPCAKNN(int _short) {
        System.out.println("\nTesting PCA/k-NN classifier");
        System.out.println("Mode=" + _short);
        
        int numDims = 10;
        int numClasses = 10;
        int k = 10;
        int numPatternsPerClass = 100;
        int numPatterns = (int)Math.rint(.9 * numClasses * numPatternsPerClass);
        int numTests = numClasses * numPatternsPerClass - numPatterns;
        int numSVDSamples = (int)Math.rint(.1 * numPatterns);
        int keep = 1;
        
        KNNClassifier pcaknn = KNNClassifier.builder()
            .k(k)
            .numSVDSamples(numSVDSamples)
            .numSVDDims(Constants.KNN.ADAPTIVE)
            .build();
        
        KNNClassifier knn = KNNClassifier.builder()
            .k(k)
            .build();
        
        System.out.println("Training PCA k-NN");
        
        double[][] trainData = knnData.getPCAKNNShortData()[TRAIN].getDataArray();
        int[] trainClass = knnData.getPCAKNNShortData()[TRAIN].getClassArray();
        for(int i = 0;i < numPatterns;i++) {
            knn.learn(trainData[i], trainClass[i], -1, 0, -1);
        }
    }

}
