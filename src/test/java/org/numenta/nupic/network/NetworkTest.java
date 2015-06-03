package org.numenta.nupic.network;

import static org.junit.Assert.assertTrue;
import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.numenta.nupic.Connections;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.MersenneTwister;

import rx.Subscriber;


public class NetworkTest {
    
    @Ignore
    public void test() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
        
        Region r1 = network.lookup("r1");
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                //System.out.println(Arrays.toString(i.getSDR()));
                System.out.println(i.getRecordNum() + "," + 
                    i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
            }
        });
        
        r1.start();
        
        try {
            r1.lookup("1").getLayerThread().join();
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        network.run(1);
        
    }
    
    ManualInput topInference = null;
    ManualInput bottomInference = null;
    @Ignore
    public void testRegionHierarchies() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p)
            .add(Network.createRegion("r1")
                .add(Network.createLayer("2", p)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())))
            .add(Network.createRegion("r2")
                .add(Network.createLayer("1", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
        
        Region r1 = network.lookup("r1");
        Region r2 = network.lookup("r2");
        r1.connect(r2);
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                topInference = (ManualInput)i;
            }
        });
        r2.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                bottomInference = (ManualInput)i;
            }
        });
        
        r2.start();
        
        // Let run for 5 secs.
        try {
            r2.lookup("1").getLayerThread().join(5000);
            assertTrue(!Arrays.equals(topInference.getSparseActives(), 
                bottomInference.getSparseActives()));
            assertTrue(!Arrays.equals(topInference.getPredictedColumns(), 
                bottomInference.getPredictedColumns()));
            assertTrue(topInference.getPredictedColumns().length > 0);
            assertTrue(bottomInference.getPredictedColumns().length > 0);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        network.run(1);
        
    }

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
            Network n = Network.create("test network", p)   // Add Network.add() method for chaining region adds
                .add(Network.createRegion("r1")             // Add version of createRegion(String name) for later connecting by name
                    .add(Network.createLayer("2/3", p)      // so that regions can be added and connecting in one long chain.
                        .using(new Connections())           // Test adding connections before elements which use them
                        .add(Sensor.create(FileSensor::create, SensorParams.create(
                            Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                        .add(new SpatialPooler())
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                )
                    .add(Network.createLayer("1", p)            // Add another Layer, and the Region internally connects it to the 
                        .add(new SpatialPooler())               // previously added Layer
                        .using(new Connections())               // Test adding connections after one element and before another
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                ))            
                .add(Network.createRegion("r2")
                    .add(Network.createLayer("2/3", p)
                        .add(new SpatialPooler())
                        .using(new Connections()) // Test adding connections after one element and before another
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                ))
                .add(Network.createRegion("r3")
                    .add(Network.createLayer("1", p)
                        .add(new SpatialPooler())
                        .add(new TemporalMemory())
                        .add(Anomaly.create(anomalyParams))
                            .using(new Connections()) // Test adding connections after elements which use them.
                ));

            n.lookup("r1").connect(n.lookup("r2")).connect(n.lookup("r3"));

        }catch(Exception e) {
            e.printStackTrace();
        }
   }

}
