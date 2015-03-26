package org.numenta.nupic.network;

import static org.numenta.nupic.algorithms.Anomaly.KEY_MODE;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.Anomaly.Mode;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.SensorParams.Keys;
import org.numenta.nupic.research.SpatialPooler;
import org.numenta.nupic.research.TemporalMemory;
import org.numenta.nupic.util.Tuple;


public class LayerTest {
    
    private Map<String, Map<String, Object>> setupMap(
        Map<String, Map<String, Object>> map,
            int n, int w, double min, double max, double radius, double resolution, Boolean periodic,
                Boolean clip, Boolean forced, String fieldName, String fieldType, String encoderType) {
        
        if(map == null) {
            map = new HashMap<String, Map<String, Object>>();
        }
        Map<String, Object> inner = null;
        if((inner = map.get(fieldName)) == null) {
            map.put(fieldName, inner = new HashMap<String, Object>());
        }
        
        inner.put("n", n);
        inner.put("w", w);
        inner.put("minVal", min);
        inner.put("maxVal", max);
        inner.put("radius", radius);
        inner.put("resolution", resolution);
        
        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clip", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);
        
        return map;
    }
    
    private Parameters getTestEncoderParams() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
            null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
            fieldEncodings, 
            25, 
            3, 
            0, 0, 0, 0.1, null, null, null, 
            "consumption", "float", "RandomDistributedScalarEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        // This will work also
        //fieldEncodings.get("timestamp").put(KEY.DATEFIELD_FORMATTER.getFieldName(), DateEncoder.FULL_DATE);
                
        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        return p;
    }

    @Test
    public void testLayerConstruction() {
        Parameters p = Parameters.getAllDefaultParameters();
        p = p.union(getTestEncoderParams());
        
        Map<String, Object> anomalyParams = new HashMap<>();
        anomalyParams.put(KEY_MODE, Mode.LIKELIHOOD);
        
        Layer layer = null;
        
        Network n = Network.create(p);
        n.add(n.createRegion()
            .add(layer = n.createLayer()
                .add(Sensor.create(FileSensor::create, SensorParams.create(
                    Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv"))))
                .add(new SpatialPooler())
                .add(new TemporalMemory())
                .add(new CLAClassifier())
            )
        );
          
        int[] vector = layer.compute(new String[] { "0", "7/2/10 0:00","21.2" });
        System.out.println("output vector = " + Arrays.toString(vector));
    }

}
