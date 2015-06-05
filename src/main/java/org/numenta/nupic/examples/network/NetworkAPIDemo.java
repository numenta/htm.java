package org.numenta.nupic.examples.network;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Layer;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.Region;
import org.numenta.nupic.network.sensor.FileSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;

import rx.Subscriber;

/**
 * Demonstrates the Java version of the NuPIC Network API (NAPI) Demo.
 * 
 * This demo demonstrates many powerful features of the HTM.java 
 * NAPI. Looking at the {@link NetworkAPIDemo#createBasicNetwork()} method demonstrates
 * the conciseness of setting up a basic network. As you can see, the network is
 * constructed in fluent style proceeding from the top-level {@link Network} container,
 * to a single {@link Region}; then to a single {@link Layer}. 
 * 
 * Layers contain most of the operation logic and are the only constructs that contain
 * algorithm components (i.e. {@link CLAClassifier}, {@link Anomaly} (the anomaly computer), 
 * {@link TemoralMemory}, {@link SpatialPooler}, and {@link Encoder} (actually, a {@link MultiEncoder}
 * which can be the parent of many child encoders).
 * 
 * 
 * @author metaware
 *
 */
public class NetworkAPIDemo {
    private static enum Mode { BASIC, MULTILAYER, MULTIREGION };
    
    private Network network;
    
    private File outputFile;
    private PrintWriter pw;
    
    public NetworkAPIDemo(Mode mode) {
        switch(mode) {
            case BASIC: network = createBasicNetwork(); break;
            case MULTILAYER: network = createMultiLayerNetwork(); break;
            case MULTIREGION: network = createMultiRegionNetwork(); break;
        }
        
        network.observe().subscribe(getSubscriber());
        try {
            outputFile = new File(System.getProperty("user.home").concat(File.separator).concat("network_demo_output.txt"));
            pw = new PrintWriter(new FileWriter(outputFile));
        }catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    private Network createBasicNetwork() {
        Parameters p = NetworkDemoHarness.getParameters();
        p = p.union(NetworkDemoHarness.getNetworkDemoTestEncoderParams());
        
        // This is how easy it is to create a full running Network!
        return Network.create("Network API Demo", p)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler())
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv"))))));
    }
    
    private Network createMultiLayerNetwork() {
        Parameters p = NetworkDemoHarness.getParameters();
        p = p.union(NetworkDemoHarness.getNetworkDemoTestEncoderParams());
        
        return Network.create("Network API Demo", p)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory()))
                .add(Network.createLayer("Layer 4", p)
                    .add(new SpatialPooler()))
                .add(Network.createLayer("Layer 5", p)
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))))
                .connect("Layer 2/3", "Layer 4")
                .connect("Layer 4", "Layer 5"));
    }
    
    private Network createMultiRegionNetwork() {
        Parameters p = NetworkDemoHarness.getParameters();
        p = p.union(NetworkDemoHarness.getNetworkDemoTestEncoderParams());
        
        return Network.create("Network API Demo", p)
            .add(Network.createRegion("Region 1")
                .add(Network.createLayer("Layer 2/3", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory()))
                .add(Network.createLayer("Layer 4", p)
                    .add(new SpatialPooler()))
                .connect("Layer 2/3", "Layer 4"))
           .add(Network.createRegion("Region 2")
                .add(Network.createLayer("Layer 2/3", p)
                    .alterParameter(KEY.AUTO_CLASSIFY, Boolean.TRUE)
                    .add(Anomaly.create())
                    .add(new TemporalMemory())
                    .add(new SpatialPooler()))
                .add(Network.createLayer("Layer 4", p)
                    .add(Sensor.create(FileSensor::create, SensorParams.create(
                        Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")))))
                .connect("Layer 2/3", "Layer 4"))
           .connect("Region 1", "Region 2");
                     
    }
    
    private Subscriber<Inference> getSubscriber() {
        return new Subscriber<Inference>() {
            @Override public void onCompleted() {
                try {
                    pw.flush();
                    pw.close();
                }catch(Exception e) {
                    e.printStackTrace();
                }
                // Sample output to demonstrate stream close notification
                System.out.println("Stream completed, see output: " + outputFile.getAbsolutePath());
            }
            @Override public void onError(Throwable e) { e.printStackTrace(); }
            @Override public void onNext(Inference i) {
                writeToFile(i, "consumption");
            }
        };
    }
    
    private void writeToFile(Inference infer, String classifierField) {
        try {
            StringBuilder sb = new StringBuilder()
                .append(infer.getRecordNum()).append(", ")
                .append(infer.getClassifierInput().get(classifierField).get("inputValue")).append(", ")
                .append(infer.getAnomalyScore());
            
            pw.println(sb.toString());
            pw.flush();
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private void runNetwork() {
        network.start();
    }
    
    public static void main(String[] args) {
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.MULTILAYER);
        demo.runNetwork();
    }
}
