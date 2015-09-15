package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.numenta.nupic.algorithms.ClassifierResult;
import org.numenta.nupic.util.NamedTuple;


public class ManualInputTest {

    /**
     * ManualInput retVal = new ManualInput();
        retVal.classifierInput = new HashMap<String, NamedTuple>(this.classifierInput);
        retVal.classifiers = this.classifiers;
        retVal.layerInput = this.layerInput;
        retVal.sdr = this.sdr;
        retVal.encoding = this.encoding;
        retVal.activeColumns = this.activeColumns;
        retVal.sparseActives = this.sparseActives;
        retVal.previousPrediction = this.previousPrediction;
        retVal.currentPrediction = this.currentPrediction;
        retVal.classification = this.classification;
        retVal.anomalyScore = this.anomalyScore;
        retVal.customObject = this.customObject;
     */
    @Test
    public void testCopy() {
        HashMap<String, NamedTuple> classifierInput = new HashMap<>();
        NamedTuple classifiers = new NamedTuple(new String[] { "one", "two" }, 1, 2);
        Object layerInput = new Object();
        int[] sdr = new int[20];
        int[] encoding = new int[40];
        int[] activeColumns = new int[25];
        int[] sparseActives = new int[2];
        int[] previousPrediction = new int[4];
        int[] currentPrediction = new int[8];
        ClassifierResult<Object> classification = new ClassifierResult<>();
        double anomalyScore = 0.48d;
        Object customObject = new Network("", NetworkTestHarness.getNetworkDemoTestEncoderParams());
        
        ManualInput mi = new ManualInput()
        .classifierInput(classifierInput)
        .layerInput(layerInput)
        .sdr(sdr)
        .encoding(encoding)
        .activeColumns(activeColumns)
        .sparseActives(sparseActives)
        .predictedColumns(previousPrediction)
        .predictedColumns(currentPrediction) // last prediction internally becomes previous
        .classifiers(classifiers)
        .storeClassification("foo", classification)
        .anomalyScore(anomalyScore)
        .customObject(customObject);
        
        ManualInput copy = mi.copy();
        assertTrue(copy.getClassifierInput().equals(classifierInput));
        assertFalse(copy.getClassifierInput() == classifierInput);
        
        assertTrue(copy.getLayerInput() == layerInput);
        
        assertTrue(Arrays.equals(copy.getSDR(), sdr));
        assertFalse(copy.getSDR() == sdr);
        
        assertTrue(Arrays.equals(copy.getEncoding(), encoding));
        assertFalse(copy.getEncoding() == encoding);
        
        assertTrue(Arrays.equals(copy.getActiveColumns(), activeColumns));
        assertFalse(copy.getActiveColumns() == activeColumns);
        
        assertTrue(Arrays.equals(copy.getSparseActives(), sparseActives));
        assertFalse(copy.getSparseActives() == sparseActives);
        
        assertTrue(Arrays.equals(copy.getPredictedColumns(), currentPrediction));
        assertFalse(copy.getPredictedColumns() == currentPrediction);
        
        assertTrue(Arrays.equals(copy.getPreviousPrediction(), previousPrediction));
        assertFalse(copy.getPreviousPrediction() == previousPrediction);
        
        assertTrue(copy.getClassifiers().equals(classifiers));
        assertFalse(copy.getClassifiers() == classifiers);
        
        assertTrue(copy.getClassification("foo").equals(classification));
        
        assertEquals(copy.getAnomalyScore(), anomalyScore, 0.0); // zero deviation
        
        assertEquals(copy.getCustomObject(), customObject);
    }

}
