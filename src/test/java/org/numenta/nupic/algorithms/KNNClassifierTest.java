package org.numenta.nupic.algorithms;

import static org.junit.Assert.*;

import org.junit.Test;
import org.numenta.nupic.Constants;
import org.numenta.nupic.DistanceMethod;


public class KNNClassifierTest {

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

}
