package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.AnomalyLikelihood;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;


public class NetworkTest {

    /**
     * Test that a null {@link Assembly.Mode} results in exception
     */
    @Test
    public void testFluentBuildSemantics() {
        Map<String, Object> params = new HashMap<>();
        params.put(KEY_MODE, Mode.LIKELIHOOD);
        
        Parameters p = Parameters.getAllDefaultParameters();
        
        try {
            Network n = Network.create(p);
            Region r1 = n.createRegion(p)
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                .add(new SpatialPooler())
                    .using(new Connections())
                .add(new TemporalMemory())
                .add(new CLAClassifier())
                .add(Anomaly.create(params));
            Region r2 = n.createRegion(p)
                .add(new SpatialPooler())
                    .using(new Connections())
                .add(new TemporalMemory())
                .add(new CLAClassifier())
                .add(Anomaly.create(params));
            Region r3 = n.createRegion(p)
                .add(new SpatialPooler())
                    .using(new Connections())
                .add(new TemporalMemory())
                .add(new CLAClassifier())
                .add(Anomaly.create(params))
                .connect(r1)
                .connect(r2);
                           
        }catch(Exception e) {
            e.printStackTrace();
            assertTrue(IllegalArgumentException.class.isAssignableFrom(e.getClass()));
            assertEquals("Mode cannot be null and must be one of: { MANUAL, AUTO, REACTIVE }", e.getMessage());
        }
   }

}
