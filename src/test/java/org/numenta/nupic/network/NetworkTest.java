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
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;


public class NetworkTest {

    /**
     * Test that a null {@link Assembly.Mode} results in exception
     */
    @Test
    public void testFluentBuildSemantics() {
        Parameters p = Parameters.getAllDefaultParameters();
        
        Map<String, Object> anomalyParams = new HashMap<>();
        anomalyParams.put(KEY_MODE, Mode.LIKELIHOOD);
        
        try {
            // Idea: Build up ResourceLocator paths in fluent style such as:
            // Layer.using(
            //     ResourceLocator.addPath("...") // Adds a search path for later mentioning terminal resources (i.e. files)
            //         .addPath("...")
            //         .addPath("..."))
            //     .add(new SpatialPooler())
            //     ...
            Network n = Network.create(p); // Add Network.add() method for chaining region adds
            Region r1 = n.createRegion()   // Add version of createRegion(String name) for later connecting by name
                .add(n.createLayer(p)      // so that regions can be added and connecting in one long chain.
                    .using(new Connections()) // Test adding connections before elements which use them
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                    .add(new SpatialPooler())
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                )
                .add(n.createLayer(p)         // Add another Layer, and the Region internally connects it to the 
                                              // previously added Layer
                    .add(new SpatialPooler())
                    .using(new Connections()) // Test adding connections after one element and before another
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                );
            
            Region r2 = n.createRegion()
                .add(n.createLayer(p)
                    .add(new SpatialPooler())
                    .using(new Connections()) // Test adding connections after one element and before another
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                );
            
            Region r3 = n.createRegion()
                .add(n.createLayer(p)
                    .add(new SpatialPooler())
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                        .using(new Connections()) // Test adding connections after elements which use them.
                )
                .connect(r1)
                .connect(r2);
        }catch(Exception e) {
            e.printStackTrace();
        }
   }

}
