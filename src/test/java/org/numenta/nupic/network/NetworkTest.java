package org.numenta.nupic.network;

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
        
        Network network = Network.create("test network", p);
        Region r1 = network.createRegion("r1")
            .add(network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))));
        
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
    
    @Ignore
    public void testRegionHierarchies() {
        Parameters p = NetworkTestHarness.getParameters();
        p = p.union(NetworkTestHarness.getNetworkDemoTestEncoderParams());
        p.setParameterByKey(KEY.RANDOM, new MersenneTwister(42));
        
        Network network = Network.create("test network", p);
        Region r1 = network.createRegion("r1")
            .add(network.createLayer("2", p)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler()));
        Region r2 = network.createRegion("r2")
            .add(network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))));
        
        r1.connect(r2);
        
        r1.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                System.out.println("R1 - " + i.getRecordNum() + ":  " + Arrays.toString(i.getPreviousPrediction()) + "  -  " + Arrays.toString(i.getSparseActives()));
                //System.out.println(i.getRecordNum() + "," + 
                    //i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
            }
        });
        r2.observe().subscribe(new Subscriber<Inference>() {
            @Override public void onCompleted() {}
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                System.out.println("R2 - " + i.getRecordNum() + ":  " + Arrays.toString(i.getPreviousPrediction()) + "  -  " + Arrays.toString(i.getSparseActives()) + "\n\n");
                //System.out.println(i.getRecordNum() + "," + 
                    //i.getClassifierInput().get("consumption").get("inputValue") + "," + i.getAnomalyScore());
            }
        });
        
        r2.start();
        
        r1.lookup("2").getConnections().printParameters();
        
        try {
            r2.lookup("1").getLayerThread().join();
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
            Network n = Network.create("test network", p); // Add Network.add() method for chaining region adds
            Region r1 = n.createRegion("r1")   // Add version of createRegion(String name) for later connecting by name
                .add(n.createLayer("2/3", p)      // so that regions can be added and connecting in one long chain.
                    .using(new Connections()) // Test adding connections before elements which use them
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))
                    .add(new SpatialPooler())
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                )
                .add(n.createLayer("1", p)         // Add another Layer, and the Region internally connects it to the 
                                              // previously added Layer
                    .add(new SpatialPooler())
                    .using(new Connections()) // Test adding connections after one element and before another
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                );
            
            Region r2 = n.createRegion("r2")
                .add(n.createLayer("2/3", p)
                    .add(new SpatialPooler())
                    .using(new Connections()) // Test adding connections after one element and before another
                    .add(new TemporalMemory())
                    .add(Anomaly.create(anomalyParams))
                );
            
            Region r3 = n.createRegion("r3")
                .add(n.createLayer("1", p)
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
