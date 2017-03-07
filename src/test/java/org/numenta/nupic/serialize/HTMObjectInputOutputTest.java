package org.numenta.nupic.serialize;

import static org.numenta.nupic.network.NetworkTestHarness.getInferredFieldsMap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.algorithms.SpatialPooler;
import org.numenta.nupic.algorithms.TemporalMemory;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.NetworkTestHarness;
import org.numenta.nupic.network.Persistence;
import org.numenta.nupic.network.PublisherSupplier;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.FastRandom;


public class HTMObjectInputOutputTest {

    @Test
    public void testRoundTrip() {
        Network network = getLoadedHotGymNetwork();
        SerializerCore serializer = Persistence.get().serializer();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        HTMObjectOutput writer = serializer.getObjectOutput(baos);
        try {
            writer.writeObject(network, Network.class);
            writer.flush();
            writer.close();
        }catch(Exception e) {
            fail();
        }
        
        byte[] bytes = baos.toByteArray();
        
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        try {
            HTMObjectInput reader = serializer.getObjectInput(bais);
            Network serializedNetwork = (Network)reader.readObject(Network.class);
            assertNotNull(serializedNetwork);
            assertTrue(serializedNetwork.equals(network));
        }catch(Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    private Network getLoadedHotGymNetwork() {
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getHotGymTestEncoderParams());
        p.set(KEY.RANDOM, new FastRandom(42));
        p.set(KEY.INFERRED_FIELDS, getInferredFieldsMap("consumption", CLAClassifier.class));

        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
                PublisherSupplier.builder()
                .addHeader("timestamp, consumption")
                .addHeader("datetime, float")
                .addHeader("B").build() }));

        Network network = Network.create("test network", p).add(Network.createRegion("r1")
            .add(Network.createLayer("1", p)
                .alterParameter(KEY.AUTO_CLASSIFY, true)
                .add(Anomaly.create())
                .add(new TemporalMemory())
                .add(new SpatialPooler())
                .add(sensor)));

        return network;
    }
}
