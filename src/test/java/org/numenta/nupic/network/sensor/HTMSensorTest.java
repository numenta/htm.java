/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.RandomDistributedScalarEncoder;
import org.numenta.nupic.encoders.SDRCategoryEncoder;
import org.numenta.nupic.network.NetworkTestHarness;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.MersenneTwister;
import org.numenta.nupic.util.Tuple;

/**
 * Higher level test than the individual sensor tests. These
 * tests ensure the complete functionality of sensors as a whole.
 * 
 * @author David Ray
 *
 */
public class HTMSensorTest {
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
    
    private Parameters getArrayTestParams() {
        Map<String, Map<String, Object>> fieldEncodings = setupMap(
                        null,
                        884, // n
                        0, // w
                        0, 0, 0, 0, null, null, null,
                        "sdr_in", "darr", "SDRPassThroughEncoder");
        Parameters p = Parameters.empty();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        return p;
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
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        return p;
    }
    
    private Parameters getCategoryEncoderParams() {
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
        fieldEncodings = setupMap(
            fieldEncodings, 
            25, 
            3, 
            0, 0, 0, 0.0, null, null, Boolean.TRUE, 
            "type", "list", "SDRCategoryEncoder");
        
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
        fieldEncodings.get("timestamp").put(KEY.DATEFIELD_PATTERN.getFieldName(), "MM/dd/YY HH:mm");
        
        String categories = "ES;S1;S2;S3;S4;S5;S6;S7;S8;S9;S10;S11;S12;S13;S14;S15;S16;S17;S18;S19;GB;US";
        fieldEncodings.get("type").put(KEY.CATEGORY_LIST.getFieldName(), categories);
        
        // This will work also
        //fieldEncodings.get("timestamp").put(KEY.DATEFIELD_FORMATTER.getFieldName(), DateEncoder.FULL_DATE);
                
        Parameters p = Parameters.getEncoderDefaultParameters();
        p.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        
        return p;
    }
    
    @Test
    public void testPadTo() {
        List<String[]> l = new ArrayList<>();
        l.add(new String[] { "0", "My"});
        l.add(new String[] { "3", "list"});
        l.add(new String[] { "4", "can "});
        l.add(new String[] { "1", "really"});
        l.add(new String[] { "6", "frustrate."});
        l.add(new String[] { "2", "unordered"});
        l.add(new String[] { "5", "also"});
        
        List<String> out = new ArrayList<>();
        for(String[] sa : l) {
            int idx = Integer.parseInt(sa[0]);
            out.set(HTMSensor.padTo(idx, out), sa[1]);
        }
        
        assertEquals("[My, really, unordered, list, can , also, frustrate.]", out.toString());
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
        
        // Cast the ValueList to the more complex type (Header)
        Header meta = (Header)sensor.getMetaInfo();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B)));
    }
    
    /**
     * Tests the formation of meta constructs using test data with no flags (empty line).
     * This tests that the parsing can proceed and the there is a registered flag
     * of {@link SensorFlags#B} inserted for an empty 3rd line of a row header.
     */
    @Test
    public void testMetaFormation_NO_HEADER_FLAGS() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
                SensorParams.create(Keys::path, "", ResourceLocator.path("rec-center-hourly-small-noheaderflags.csv")));
        
        // Cast the ValueList to the more complex type (Header)
        Header meta = (Header)sensor.getMetaInfo();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.B)));
    }
    
    /**
     * Special test case for extra processing for category encoder lists
     */
    @Test
    public void testCategoryEncoderCreation() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-4period-cat.csv")));
        
        
        // Cast the ValueList to the more complex type (Header)
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        Header meta = (Header)htmSensor.getMetaInfo();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT) || l.equals(FieldMetaType.LIST)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption") || l.equals("type")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B) || l.equals(SensorFlags.C)));
        
        // Set the parameters on the sensor.
        // This enables it to auto-configure itself; a step which will
        // be done at the Region level.
        Encoder<Object> multiEncoder = htmSensor.getEncoder();
        assertNotNull(multiEncoder);
        assertTrue(multiEncoder instanceof MultiEncoder);
        
        // Set the Local parameters on the Sensor
        htmSensor.initEncoder(getCategoryEncoderParams());
        List<EncoderTuple> encoders = multiEncoder.getEncoders(multiEncoder);
        assertEquals(3, encoders.size());
        
        DateEncoder dateEnc = (DateEncoder)encoders.get(1).getEncoder();
        SDRCategoryEncoder catEnc = (SDRCategoryEncoder)encoders.get(2).getEncoder();
        assertEquals("[0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]", Arrays.toString(catEnc.encode("ES")));
        
        // Now test the encoding of an input row
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("timestamp", dateEnc.parse("7/12/10 13:10"));
        d.put("consumption", 35.3);
        d.put("type", "ES");
        int[] output = multiEncoder.encode(d);
        System.out.println("output = "+ Arrays.toString(output));
        int[] expected = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
                          0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertTrue(Arrays.equals(expected, output));
    }
    
    /**
     * Tests that a meaningful exception is thrown when no list category encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testListCategoryEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("list")
            .addHeader("C")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no string category encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testStringCategoryEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("string")
            .addHeader("C")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no date encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testDateEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("datetime")
            .addHeader("T")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null, 
            25, 
            3, 
            0, 0, 0, 0.1, null, null, null, 
            "consumption", "float", "RandomDistributedScalarEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no geo encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGeoEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("geo")
            .addHeader("")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no coordinate encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCoordinateEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("coord")
            .addHeader("")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no int number encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testIntNumberEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("int")
            .addHeader("")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no float number encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testFloatNumberEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("float")
            .addHeader("")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests that a meaningful exception is thrown when no boolean encoder configuration was provided
     */
    @Test(expected = IllegalArgumentException.class)
    public void testBoolEncoderNotInitialized() {
        Publisher manual = Publisher.builder()
            .addHeader("foo")
            .addHeader("bool")
            .addHeader("")
            .build();
        Sensor<File> sensor = Sensor.create(ObservableSensor::create, SensorParams.create(
            Keys::obs, "", manual));
        Map<String, Map<String, Object>> fieldEncodings = setupMap( null,
            0, // n
            0, // w
            0, 0, 0, 0, null, null, null,
            "timestamp", "datetime", "DateEncoder");
        Parameters params = Parameters.getEncoderDefaultParameters();
        params.set(KEY.FIELD_ENCODING_MAP, fieldEncodings);
        HTMSensor<File> htmSensor = (HTMSensor<File>) sensor;
        htmSensor.initEncoder(params);
    }
    
    /**
     * Tests the auto-creation of Encoders from Sensor meta data.
     */
    @Test
    public void testInternalEncoderCreation() {
        
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly.csv")));
        
        
        // Cast the ValueList to the more complex type (Header)
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        Header meta = (Header)htmSensor.getMetaInfo();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B)));
        
        // Set the parameters on the sensor.
        // This enables it to auto-configure itself; a step which will
        // be done at the Region level.
        Encoder<Object> multiEncoder = htmSensor.getEncoder();
        assertNotNull(multiEncoder);
        assertTrue(multiEncoder instanceof MultiEncoder);
        
        // Set the Local parameters on the Sensor
        htmSensor.initEncoder(getTestEncoderParams());
        List<EncoderTuple> encoders = multiEncoder.getEncoders(multiEncoder);
        assertEquals(2, encoders.size());
        
        // Test date specific encoder configuration
        //
        // All encoders in the MultiEncoder are accessed in a particular
        // order (the alphabetical order their corresponding fields are in),
        // so alphabetically "consumption" proceeds "timestamp"
        // so we need to ensure that the proper order is preserved (i.e. exists at index 1)
        DateEncoder dateEnc = (DateEncoder)encoders.get(1).getEncoder();
        try {
            dateEnc.parseEncode("7/12/10 13:10");
            dateEnc.parseEncode("7/12/2010 13:10");
            // Should fail here due to conflict with configured format
            dateEnc.parseEncode("13:10 7/12/10");
            fail();
        }catch(Exception e) {
           assertEquals("Invalid format: \"13:10 7/12/10\" is malformed at \":10 7/12/10\"", e.getMessage());
        }
        
        RandomDistributedScalarEncoder rdse = (RandomDistributedScalarEncoder)encoders.get(0).getEncoder();
        int[] encoding = rdse.encode(35.3);
        System.out.println(Arrays.toString(encoding));
        
        // Now test the encoding of an input row
        Map<String, Object> d = new HashMap<String, Object>();
        d.put("timestamp", dateEnc.parse("7/12/10 13:10"));
        d.put("consumption", 35.3);
        int[] output = multiEncoder.encode(d);
        int[] expected = {0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 
                          0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertTrue(Arrays.equals(expected, output));
    }
    
    /**
     * Test that we can query the stream for its terminal state, which {@link Stream}s
     * don't provide out of the box.
     */
    @Test
    public void testSensorTerminalOperationDetection() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        // We haven't done anything with the stream yet, so it should not be terminal
        assertFalse(htmSensor.isTerminal());
        htmSensor.getInputStream().forEach(l -> System.out.println(Arrays.toString((String[])l)));
        // Should now be terminal after operating on the stream
        assertTrue(htmSensor.isTerminal());
    }
    
    /**
     * Tests mechanism by which {@link Sensor}s will input information
     * and output information ensuring that multiple streams can be created.
     */
    @Test
    public void testSensorMultipleStreamCreation() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("rec-center-hourly-small.csv")));
        
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        htmSensor.initEncoder(getTestEncoderParams());
        
        // Ensure that the HTMSensor's output stream can be retrieved more than once.
        Stream<int[]> outputStream = htmSensor.getOutputStream();
        Stream<int[]> outputStream2 = htmSensor.getOutputStream();
        Stream<int[]> outputStream3 = htmSensor.getOutputStream();
        
        // Check to make sure above multiple retrieval doesn't flag the underlying stream as operated upon
        assertFalse(htmSensor.isTerminal());
        assertEquals(17, outputStream.count());
        
        //After the above we cannot request a new stream, so this will fail
        //however, the above streams that were already requested should be unaffected.
        assertTrue(htmSensor.isTerminal());
        try {
            @SuppressWarnings("unused")
            Stream<int[]> outputStream4 = htmSensor.getOutputStream();
            fail();
        }catch(Exception e) {
            assertEquals("Stream is already \"terminal\" (operated upon or empty)", e.getMessage());
        }
        
        //These Streams were created before operating on a stream
        assertEquals(17, outputStream2.count()); 
        assertEquals(17, outputStream3.count()); 
        
        // Verify that different streams are retrieved.
        assertFalse(outputStream.hashCode() == outputStream2.hashCode());
        assertFalse(outputStream2.hashCode() == outputStream3.hashCode());
    }
    
    @Test
    public void testInputIntegerArray() {
        Sensor<File> sensor = Sensor.create(
            FileSensor::create, 
            SensorParams.create(
                Keys::path, "", ResourceLocator.path("1_100.csv")));
                    
        HTMSensor<File> htmSensor = (HTMSensor<File>)sensor;
        
        htmSensor.initEncoder(getArrayTestParams());
        
        // Ensure that the HTMSensor's output stream can be retrieved more than once.
        Stream<int[]> outputStream = htmSensor.getOutputStream();
        assertEquals(884, ((int[])outputStream.findFirst().get()).length);
    }
    
    @Test
    public void testWithGeospatialEncoder() {
    	Publisher manual = Publisher.builder()
    		.addHeader("timestamp,consumption,location")
    		.addHeader("datetime,float,geo")
    		.addHeader("T,,").build();
    	
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, "", manual));
        
        Parameters p = NetworkTestHarness.getParameters().copy();
        p = p.union(NetworkTestHarness.getGeospatialTestEncoderParams());
        p.set(KEY.RANDOM, new MersenneTwister(42));
        p.set(KEY.AUTO_CLASSIFY, Boolean.TRUE);
        
        HTMSensor<ObservableSensor<String[]>> htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;
        
        
        //////////////////////////////////////////////////////////////
        //                 Test Header Configuration                //
        //////////////////////////////////////////////////////////////
        
        // Cast the ValueList to the more complex type (Header)
        Header meta = (Header)htmSensor.getMetaInfo();
        assertTrue(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT) || l.equals(FieldMetaType.GEO)));
        
        // Negative test (Make sure "GEO" is configured and expected
        assertFalse(meta.getFieldTypes().stream().allMatch(
            l -> l.equals(FieldMetaType.DATETIME) || l.equals(FieldMetaType.FLOAT)));
        
        
        assertTrue(meta.getFieldNames().stream().allMatch(
            l -> l.equals("timestamp") || l.equals("consumption") || l.equals("location")));
        assertTrue(meta.getFlags().stream().allMatch(
            l -> l.equals(SensorFlags.T) || l.equals(SensorFlags.B)));
        
        Encoder<Object> multiEncoder = htmSensor.getEncoder();
        assertNotNull(multiEncoder);
        assertTrue(multiEncoder instanceof MultiEncoder);
        
        
		//////////////////////////////////////////////////////////////
		//                 Test Encoder Composition                 //
		//////////////////////////////////////////////////////////////
        
        List<EncoderTuple> encoders = null;
        
        // NEGATIVE TEST: first so that we can reuse the sensor below - APPLY WRONG PARAMS
        try {
        	htmSensor.initEncoder(getTestEncoderParams()); // <--- WRONG PARAMS
        	// Should fail here
        	fail();
        	encoders = multiEncoder.getEncoders(multiEncoder);
        	assertEquals(2, encoders.size());
        }catch(IllegalArgumentException e) {
        	assertEquals("Coordinate encoder never initialized: location", e.getMessage());
        }
        
        /////////////////////////////////////
        
        // Recreate Sensor for POSITIVE TEST. Set the Local parameters on the Sensor
        sensor = Sensor.create(
            ObservableSensor::create, SensorParams.create(Keys::obs, "", manual));
        htmSensor = (HTMSensor<ObservableSensor<String[]>>)sensor;
        htmSensor.initEncoder(p);
        
        multiEncoder = htmSensor.getEncoder();
        assertNotNull(multiEncoder);
        assertTrue(multiEncoder instanceof MultiEncoder);
        encoders = multiEncoder.getEncoders(multiEncoder);
        assertEquals(3, encoders.size());
        
        Sensor<ObservableSensor<String[]>> finalSensor = sensor;
        
        (new Thread() {
        	public void run() {
        		manual.onNext("7/12/10 13:10,35.3,40.6457;-73.7962;5"); //5 = meters per second
        	}
        }).start();
        
        
        int[] output = ((HTMSensor<ObservableSensor<String[]>>)finalSensor).getOutputStream().findFirst().get();
        
        int[] expected =  { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        					1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        					0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 
        					0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 
        					1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        
        assertTrue(Arrays.equals(expected, output));
    }
   
}
