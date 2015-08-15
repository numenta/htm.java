# ![Hot Gym Demo](http://metaware.us/hotgym.png) 
# Hot Gym Demo 
_... demonstrates the Network API in action!_ **3 Modes !!**

### Made by developers _for_ developers (no GUI yet)...

This demo shows the Network API running in **3 modes** to demonstrate the flexibility and ease of constructing NAPI networks.

### _Extremely Easy to Run_
Just type ( from demo dir: htm.java/src/main/java/org/numenta/nupic/examples/napi/hotgym ):
>&gt; java -jar NAPI-Hotgym-Demo-1.0.jar 

![](http://metaware.us/napi-hotgym-demo.gif)



### Basic Mode
1 Layer, 1 Region - Everything running in one layer...

``` Java
/**
 * Creates a basic {@link Network} with 1 {@link Region} and 1 {@link Layer}. However
 * this basic network contains all algorithmic components.
 * 
 * @return  a basic Network
 */
Network createBasicNetwork() {
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
```

### Multi-Layer Mode
1 Region, 3 Layers - Performs the same job while demonstrating breaking things up into layers.

``` Java
/**
 * Creates a {@link Network} containing one {@link Region} with multiple 
 * {@link Layer}s. This demonstrates the method by which multiple layers 
 * are added and connected; and the flexibility of the fluent style api.
 * 
 * @return  a multi-layer Network
 */
Network createMultiLayerNetwork() {
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
```

### Multi-Region Mode
2 Regions (Each with 2 Layers containing various mixes of Spatial Poolers and Temporal Memories !!!)

``` Java
/**
 * Creates a {@link Network} containing 2 {@link Region}s with multiple
 * {@link Layer}s in each.
 * 
 * @return a multi-region Network
 */
Network createMultiRegionNetwork() {
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
```

### Experiment by changing modes within the demo file's "main()" method...

Try substituting Mode.MULTILAYER or Mode.MULTIREGION, for Mode.BASIC below...

``` Java
/**
 * Main entry point of the demo
 * @param args
 */
public static void main(String[] args) {
    // Substitute the other modes here to see alternate examples of Network construction
    // in operation.
    NetworkAPIDemo demo = new NetworkAPIDemo(Mode.BASIC);  <-- HERE!
    demo.runNetwork();
}
```

### Enjoy!!
