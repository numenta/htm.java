package org.numenta.nupic.examples.qt;

import static org.junit.Assert.*;

import java.util.Arrays;

import gnu.trove.list.array.TIntArrayList;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.examples.qt.QuickTest.Layer;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;

/**
 * Mostly added so that coveralls won't bitch and whine. The code
 * this tests is just some quick and dirty example code, but anyway...
 * 
 * @author David Ray
 */
public class QuickTestTest {
    
    @Test
    public void testGetParameters() {
        Parameters params = QuickTest.getParameters();
        assertTrue(((int[])params.getParameterByKey(KEY.INPUT_DIMENSIONS))[0] == 8);
        assertTrue(((int[])params.getParameterByKey(KEY.COLUMN_DIMENSIONS))[0] == 20);
        assertTrue(((int)params.getParameterByKey(KEY.CELLS_PER_COLUMN)) == 6);
        assertEquals(((double)params.getParameterByKey(KEY.PERMANENCE_INCREMENT)), 
            ((double)params.getParameterByKey(KEY.PERMANENCE_DECREMENT)), 0.0);
    }

    @Test
    public void testRunThroughLayer() {
        Layer<Double> layer = null;
        try {
            Parameters params = QuickTest.getParameters();
            
            //Layer components
            ScalarEncoder.Builder dayBuilder =
                ScalarEncoder.builder()
                    .n(8)
                    .w(3)
                    .radius(1.0)
                    .minVal(1.0)
                    .maxVal(8)
                    .periodic(true)
                    .forced(true)
                    .resolution(1);
            ScalarEncoder encoder = dayBuilder.build();
            SpatialPooler sp = new SpatialPooler();
            TemporalMemory tm = new TemporalMemory();
            CLAClassifier classifier = new CLAClassifier(new TIntArrayList(new int[] { 1 }), 0.1, 0.3, 0);
            
            layer = QuickTest.getLayer(params, encoder, sp, tm, classifier);
            
            for(double i = 1, x = 0;x < 10000;i = (i == 7 ? 1 : i + 1), x++) {// USE "X" here to control run length
                if (i == 1) tm.reset(layer.getMemory());
                QuickTest.runThroughLayer(layer, i, (int)i, (int)x);
            }
        }catch(Exception e) {
            fail();
        }
        
        System.out.println("actual = " + Arrays.toString(layer.getActual()) + ",  predicted = " + Arrays.toString(layer.getPredicted()));
        //assertArrayEquals(layer.getActual(), layer.getPredicted());
    }

}
