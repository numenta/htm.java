/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
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
 * @author cogmission
 *
 */
public class NetworkAPIDemo {
    /** 3 modes to choose from to demonstrate network usage */
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
    
    /**
     * Creates a basic {@link Network} with 1 {@link Region} and 1 {@link Layer}. However
     * this basic network contains all algorithmic components.
     * 
     * @return  a basic Network
     */
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
    
    /**
     * Creates a {@link Network} containing one {@link Region} with multiple 
     * {@link Layer}s. This demonstrates the method by which multiple layers 
     * are added and connected; and the flexibility of the fluent style api.
     * 
     * @return  a multi-layer Network
     */
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
    
    /**
     * Creates a {@link Network} containing 2 {@link Region}s with multiple
     * {@link Layer}s in each.
     * 
     * @return a multi-region Network
     */
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
    
    /**
     * Demonstrates the composition of a {@link Subscriber} (may also use
     * {@link Observer}). There are 3 methods one must be concerned with: 
     * </p>
     * <p>
     * <pre>
     * 1. onCompleted(). Called when the stream is exhausted and will be closed.
     * 2. onError(). Called when there is an underlying exception or error in the processing.
     * 3. onNext(). Called for each processing cycle of the network. This is the method
     * that is overridden to do downstream work in your application.
     * 
     * @return
     */
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
    
    /**
     * Primitive file appender for collecting output. This just demonstrates how to use
     * {@link Subscriber#onNext(Object)} to accomplish some work.
     * 
     * @param infer             The {@link Inference} object produced by the Network
     * @param classifierField   The field we use in this demo for anomaly computing.
     */
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
            pw.flush();
            pw.close();
        }
        
    }
    
    /**
     * Simple run hook
     */
    private void runNetwork() {
        network.start();
    }
    
    /**
     * Main entry point of the demo
     * @param args
     */
    public static void main(String[] args) {
        // Substitute the other modes here to see alternate examples of Network construction
        // in operation.
        NetworkAPIDemo demo = new NetworkAPIDemo(Mode.BASIC);
        demo.runNetwork();
    }
}
