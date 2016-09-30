package org.numenta.nupic.algorithms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.Constants;
import org.numenta.nupic.DistanceMethod;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;


public class KNNClassifierTest {
    
    private KNNClassifier initClassifier(Parameters p) {
        KNNClassifier.Builder builder = KNNClassifier.builder();
        KNNClassifier knn = builder.build();
        p.apply(knn);
        
        return knn;
    }

    @Test
    public void testBuilder() {
        KNNClassifier.Builder builder = KNNClassifier.builder();
        
        builder.k(42)
        .exact(true)
        .distanceNorm(12.5)
        .distanceMethod(DistanceMethod.PCT_INPUT_OVERLAP)
        .distanceThreshold(2.3)
        .doBinarization(true)
        .binarizationThreshold(3.0)
        .useSparseMemory(true)
        .sparseThreshold(349.0)
        .relativeThreshold(true)
        .numWinners(100)
        .numSVDSamples(4)
        .numSVDDims(Constants.KNN.ADAPTIVE)
        .fractionOfMax(.84)
        .maxStoredPatterns(30)
        .replaceDuplicates(true)
        .cellsPerCol(32);
        
        KNNClassifier knn = builder.build();
        
        assertEquals(knn.getK(), 42);
        assertTrue(knn.isExact());
        assertEquals(12.5, knn.getDistanceNorm(), 0.0);
        assertEquals(DistanceMethod.PCT_INPUT_OVERLAP, knn.getDistanceMethod());
        assertEquals(2.3, knn.getDistanceThreshold(), 0.0);
        assertTrue(knn.isDoBinarization());
        assertEquals(3.0, knn.getBinarizationThreshold(), 0.0);
        assertTrue(knn.isRelativeThreshold());
        assertTrue(knn.isUseSparseMemory());
        assertEquals(349.0, knn.getSparseThreshold(), 0.0);
        assertEquals(100, knn.getNumWinners());
        assertEquals(4, knn.getNumSVDSamples());
        assertEquals(Constants.KNN.ADAPTIVE, knn.getNumSVDDims());
        assertEquals(.84, knn.getFractionOfMax(), 0.0);
        assertEquals(30, knn.getMaxStoredPatterns());
        assertTrue(knn.isReplaceDuplicates());
        assertEquals(32, knn.getCellsPerCol());
    }
    
    @Test
    public void testParameterBuild() {
        Parameters p = Parameters.getKNNDefaultParameters();
        p.set(KEY.DISTANCE_METHOD, DistanceMethod.NORM);
        p.set(KEY.DISTANCE_NORM, 12.5);
        p.set(KEY.K, 42);
        p.set(KEY.EXACT, true);
        p.set(KEY.DISTANCE_THRESHOLD, 2.3);
        p.set(KEY.DO_BINARIZATION, true);
        p.set(KEY.BINARIZATION_THRESHOLD, 3.0);
        p.set(KEY.RELATIVE_THRESHOLD, true);
        p.set(KEY.USE_SPARSE_MEMORY, true);
        p.set(KEY.SPARSE_THRESHOLD, 349.0);
        p.set(KEY.NUM_WINNERS, 100);
        p.set(KEY.NUM_SVD_SAMPLES, 4);
        p.set(KEY.NUM_SVD_DIMS, Constants.KNN.ADAPTIVE);
        p.set(KEY.FRACTION_OF_MAX, .84);
        p.set(KEY.MAX_STORED_PATTERNS, 30);
        p.set(KEY.REPLACE_DUPLICATES, true);
        p.set(KEY.KNN_CELLS_PER_COL, 32);
        
        KNNClassifier knn = initClassifier(p);
        
        assertEquals(knn.getK(), 42);
        assertTrue(knn.isExact());
        assertEquals(12.5, knn.getDistanceNorm(), 0.0);
        assertEquals(DistanceMethod.NORM, knn.getDistanceMethod());
        assertEquals(2.3, knn.getDistanceThreshold(), 0.0);
        assertTrue(knn.isDoBinarization());
        assertEquals(3.0, knn.getBinarizationThreshold(), 0.0);
        assertTrue(knn.isRelativeThreshold());
        assertTrue(knn.isUseSparseMemory());
        assertEquals(349.0, knn.getSparseThreshold(), 0.0);
        assertEquals(100, knn.getNumWinners());
        assertEquals(4, knn.getNumSVDSamples());
        assertEquals(Constants.KNN.ADAPTIVE, knn.getNumSVDDims());
        assertEquals(.84, knn.getFractionOfMax(), 0.0);
        assertEquals(30, knn.getMaxStoredPatterns());
        assertTrue(knn.isReplaceDuplicates());
        assertEquals(32, knn.getCellsPerCol());
    }
    
    @Test
    public void sparsifyVector() {
        Parameters p = Parameters.getKNNDefaultParameters();
        p.set(KEY.DISTANCE_METHOD, DistanceMethod.NORM);
        p.set(KEY.DISTANCE_NORM, 2.0);
        
        KNNClassifier classifier = initClassifier(p);
        double[] inputPattern = { 0, 1, 3, 7, 11 };
        double[] outputPattern = classifier.sparsifyVector(inputPattern, true);
        assertTrue(Arrays.equals(inputPattern, outputPattern));
        
        p.set(KEY.RELATIVE_THRESHOLD, true);
        p.set(KEY.SPARSE_THRESHOLD, 0.2);
        classifier = initClassifier(p);
        outputPattern = classifier.sparsifyVector(inputPattern, true);
        assertTrue(Arrays.equals(new double[] { 0, 0, 3, 7, 11 }, outputPattern));
        
        p.set(KEY.RELATIVE_THRESHOLD, true);
        p.set(KEY.SPARSE_THRESHOLD, 0.2);
        p.set(KEY.NUM_WINNERS, 2);
        classifier = initClassifier(p);
        outputPattern = classifier.sparsifyVector(inputPattern, true);
        assertTrue(Arrays.equals(new double[] { 0, 0, 0, 0, 0 }, outputPattern));
        
        p.set(KEY.RELATIVE_THRESHOLD, true);
        p.set(KEY.SPARSE_THRESHOLD, 0.2);
        p.set(KEY.DO_BINARIZATION, true);
        p.clearParameter(KEY.NUM_WINNERS);
        classifier = initClassifier(p);
        outputPattern = classifier.sparsifyVector(inputPattern, true);
        assertTrue(Arrays.equals(new double[] { 0., 0., 1., 1., 1. }, outputPattern));
    }
    
    @Test
    public void testDistanceMetrics() {
        Parameters p = Parameters.getKNNDefaultParameters();
        p.set(KEY.DISTANCE_METHOD, DistanceMethod.NORM);
        p.set(KEY.DISTANCE_NORM, 2.0);
        
        KNNClassifier classifier = initClassifier(p);
        
        int dimensionality = 40;
        double[] protoA = { 0, 1, 3, 7, 11 };
        double[] protoB = { 20, 28, 30 };
        
        classifier.learn(protoA, 0, -1, dimensionality, -1);
        classifier.learn(protoB, 0, -1, dimensionality, -1);
    }

}
