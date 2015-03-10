package org.numenta.nupic.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.network.SensorParams.Keys;

/**
 * Higher level test than the individual sensor tests. These
 * tests ensure the complete functionality of sensors as a whole.
 * 
 * @author David Ray
 *
 */
public class SensorTest {
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
            
            // Load defaults before doing union with new values
            Parameters def = Parameters.getEncoderDefaultParameters();
            for(KEY k : def.keys()) {
                inner.put(k.getFieldName(), def.getParameterByKey(k));
            }
        }
        
        if(n != 0) inner.put("n", n);
        if(w != 0) inner.put("w", w);
        if(min != 0) inner.put("minVal", min);
        if(max != 0) inner.put("maxVal", max);
        if(radius != 0) inner.put("w", radius);
        if(resolution != 0) inner.put("resolution", resolution);
        if(periodic != null) inner.put("periodic", periodic);
        if(clip != null) inner.put("clip", clip);
        if(forced != null) inner.put("forced", forced);
        if(fieldName != null) inner.put("fieldName", fieldName);
        if(fieldType != null) inner.put("fieldType", fieldType);
        if(encoderType != null) inner.put("encoderType", encoderType);
        
        return map;
    }

    /**
     * Tests that the creation mechanism detects insufficient state
     * for creating {@link Sensor}s.
     */
    @Test
    public void testHandlesImproperInstantiation() {
        try {
            Sensor.create(null, null);
            fail();
        }catch(Exception e) {
            assertEquals("Factory cannot be null", e.getMessage());
        }
        
        try {
            Sensor.create(FileSensor::create, null);
            fail();
        }catch(Exception e) {
            assertEquals("Properties (i.e. \"SensorParams\") cannot be null", e.getMessage());
        }
    }
    
    /**
     * Tests the formation of meta constructs (i.e. may be header or other) which
     * describe the format of columnated data and processing hints (how and when to reset).
     */
    @Test
    public void testMetaFormation() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")));
        
        // Cast the ValueList to the more complex type (SensorInputMeta)
        SensorInputMeta meta = (SensorInputMeta)sensor.getMeta();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B)));
    }
    
    /**
     * Tests the auto-creation of Encoders from Sensor meta data.
     */
    @Test
    public void testInternalEncoderCreation() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
            null,
            250,
            11,
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        fieldEncodings = setupMap(
            fieldEncodings, 
            250, 
            11, 
            0, 0, 0, 0, null, null, null, 
            "consumption", "float", "RandomDistributedScalarEncoder");
        
        Parameters p = Parameters.getEncoderDefaultParameters();
        p.setParameterByKey(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")));
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        // Cast the ValueList to the more complex type (SensorInputMeta)
        SensorInputMeta meta = (SensorInputMeta)htmSensor.getMeta();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B)));
        
        Encoder<?> enc = htmSensor.getEncoder();
        assertNotNull(enc);
        assertTrue(enc instanceof MultiEncoder);
        // Set the global parameters on the Sensor
        htmSensor.setGlobalParameters(p);
        List<Encoder<Object>> encoders = ((MultiEncoder)enc).getEncoderList();
        assertEquals(2, encoders.size());
    }
}
