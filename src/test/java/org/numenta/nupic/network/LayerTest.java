package org.numenta.nupic.network;

import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;


public class LayerTest {

    @Test
    public void testLayerConstruction() {
        Parameters p = Parameters.getAllDefaultParameters();
        
        Map<String, Object> anomalyParams = new HashMap<>();
        anomalyParams.put(KEY_MODE, Mode.LIKELIHOOD);
        
        Layer layer = null;
        
        Network n = Network.create(p);
        n.add(n.createRegion()
            .add(layer = n.createLayer()
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                .add(new SpatialPooler())
                .add(new TemporalMemory())
                .add(new CLAClassifier())
            )
        );
          
        //layer.compute(input)
    }

}
