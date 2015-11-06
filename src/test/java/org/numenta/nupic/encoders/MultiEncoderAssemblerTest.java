package org.numenta.nupic.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.network.NetworkTestHarness;
import org.numenta.nupic.util.Tuple;

/**
 *  case "CategoryEncoder":
        return CategoryEncoder.builder();
    case "CoordinateEncoder":
        return CoordinateEncoder.builder();
    case "GeospatialCoordinateEncoder":
        return GeospatialCoordinateEncoder.geobuilder();
    case "LogEncoder":
        return LogEncoder.builder();
    case "PassThroughEncoder":
        return PassThroughEncoder.builder();
    case "ScalarEncoder":
        return ScalarEncoder.builder();
    case "SparsePassThroughEncoder":
        return SparsePassThroughEncoder.sparseBuilder();
    case "SDRCategoryEncoder":
        return SDRCategoryEncoder.builder();
    case "RandomDistributedScalarEncoder":
        return RandomDistributedScalarEncoder.builder();
    case "DateEncoder":
        return DateEncoder.builder();
    case "DeltaEncoder":
        return DeltaEncoder.deltaBuilder();
    case "SDRPassThroughEncoder" :
        return SDRPassThroughEncoder.sptBuilder();
    default:
        throw new IllegalArgumentException("Invalid encoder: " + encoderName);
        
 * @author cogmission
 *
 */
public class MultiEncoderAssemblerTest {

    @Test
    public void testAssembleCategoryEncoder() {
//        NetworkTestHarness.setupMap(map, n, w, min, max, radius, resolution, periodic, 
//            clip, forced, fieldName, fieldType, encoderType)
        
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            3,    // w
            0,    // min
            8.0,  // max
            0,    // radius
            0,  // resolution
            null, // periodic
            null,               // clip
            Boolean.TRUE,       // forced
            "testfield",        // fieldName
            "string",           // fieldType
            "CategoryEncoder"); // encoderType
        
        System.out.println("map = " + settings);
        
        // Attempt build omitting the Category list (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Category List cannot be null", e.getMessage());
        }
        
        // Failure specifies additional fields which must be set
        String[] categories = new String[] { "ES", "GB", "US" };
        settings.get("testfield").put("categoryList", Arrays.<String>asList(categories));
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("One of n, radius, resolution must be specified for a CategoryEncoder", e.getMessage());
        }
        
        // Should now work
        settings.get("testfield").put("radius", 1.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) { 
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(CategoryEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleSDRCategoryEncoder() {
        String[] categories = {"ES", "S1", "S2", "S3", "S4", "S5", "S6", "S7", "S8",
            "S9", "S10", "S11", "S12", "S13", "S14", "S15", "S16",
            "S17", "S18", "S19", "GB", "US"};
        
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            100,  // n
            10,   // w
            0,    // min
            0,    // max
            0,    // radius
            0,  // resolution
            null, // periodic
            null,               // clip
            Boolean.TRUE,       // forced
            "testfield",        // fieldName
            "string",           // fieldType
            "SDRCategoryEncoder"); // encoderType
        
        // Attempt build omitting the Category list (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Category List cannot be null", e.getMessage());
        }
        
        // Failure specifies additional fields which must be set
        settings.get("testfield").put("n", 0);
        settings.get("testfield").put("w", 0);
        settings.get("testfield").put("categoryList", Arrays.<String>asList(categories));
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("\"N\" should be set", e.getMessage());
        }
        
        // Should now work
        settings.get("testfield").put("n", 100);
        settings.get("testfield").put("w", 10);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) { 
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(SDRCategoryEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleCoordinateEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "coord",              // fieldType
            "CoordinateEncoder"); // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalArgumentException.class, e.getClass());
            assertEquals("w must be odd, and must be a positive integer", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("n", 33);
        settings.get("testfield").put("w", 3);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(CoordinateEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleGeospatialCoordinateEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "coord",              // fieldType
            "GeospatialCoordinateEncoder"); // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Scale or Timestep not set", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("n", 33);
        settings.get("testfield").put("w", 3);
        settings.get("testfield").put("scale", 30);
        settings.get("testfield").put("timestep", 60);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(GeospatialCoordinateEncoder.class, l.get(0).getEncoder().getClass());
    }

    @Test
    public void testAssembleLogEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "float",              // fieldType
            "LogEncoder"); // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("One of n, radius, resolution must be specified for a LogEncoder", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("w", 5);
        settings.get("testfield").put("resolution", 0.1);
        settings.get("testfield").put("minVal", 1.0);
        settings.get("testfield").put("maxVal", 10000.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(LogEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssemblePassThroughEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "int",              // fieldType
            "PassThroughEncoder"); // encoderType
        
        // Should pass through
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(PassThroughEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleScalarEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "int",                // fieldType
            "ScalarEncoder");     // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("W must be an odd number (to eliminate centering difficulty)", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("w", 5);
        settings.get("testfield").put("resolution", 0.1);
        settings.get("testfield").put("minVal", 1.0);
        settings.get("testfield").put("maxVal", 10000.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(ScalarEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleSparsePassThroughEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "sarr",              // fieldType
            "SparsePassThroughEncoder"); // encoderType
        
        // Should pass through
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(SparsePassThroughEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleRandomDistributedScalarEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "int",                // fieldType
            "RandomDistributedScalarEncoder");     // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("W must be an odd positive integer (to eliminate centering difficulty)", e.getMessage());
        }
        
        // Attempt build omitting needed settings (should fail)
        settings.get("testfield").put("w", 5);
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("Resolution must be a positive number", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("n", 60);
        settings.get("testfield").put("resolution", 1.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(RandomDistributedScalarEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleDateEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,               // clip
            Boolean.TRUE,       // forced
            "testfield",        // fieldName
            "datetime",         // fieldType
            "DateEncoder");     // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            
            List<EncoderTuple> l = me.getEncoders(me); 
            assertTrue(!l.isEmpty());
            assertEquals(DateEncoder.class, l.get(0).getEncoder().getClass());
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("testfield", new DateTime());
            me.encode(entry);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
        }
        
        // Attempt build omitting needed settings (should fail)
        settings.get("testfield").put("w", 5);
        try {
            settings.get("testfield").put(KEY.DATEFIELD_DOFW.getFieldName(), new Tuple(1, 1.0)); // Day of week
            settings.get("testfield").put(KEY.DATEFIELD_TOFD.getFieldName(), new Tuple(5, 4.0)); // Time of day
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            
            List<EncoderTuple> l = me.getEncoders(me); 
            assertTrue(!l.isEmpty());
            assertEquals(DateEncoder.class, l.get(0).getEncoder().getClass());
            
            Map<String, Object> entry = new HashMap<>();
            entry.put("testfield", new DateTime());
            int[] encoding = me.encode(entry);
            assertTrue(encoding.length > 0);
        }catch(Exception e) {
            fail();
        }
        
    }
    
    @Test
    public void testAssembleDeltaEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "float",              // fieldType
            "DeltaEncoder"); // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("W must be an odd number (to eliminate centering difficulty)", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("n", 100);
        settings.get("testfield").put("w", 21);
        settings.get("testfield").put("radius", 1.5);
        settings.get("testfield").put("resolution", 0.5);
        settings.get("testfield").put("minVal", 1.0);
        settings.get("testfield").put("maxVal", 8.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(DeltaEncoder.class, l.get(0).getEncoder().getClass());
    }
    
    @Test
    public void testAssembleSDRPassThroughEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "sarr",              // fieldType
            "SDRPassThroughEncoder"); // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            
            List<EncoderTuple> l = me.getEncoders(me); 
            assertTrue(!l.isEmpty());
            assertEquals(SDRPassThroughEncoder.class, l.get(0).getEncoder().getClass());
        }catch(Exception e) {
            fail();
        }
    }
    
    @Test
    public void testAssembleAdaptiveScalarEncoder() {
        Map<String, Map<String, Object>> settings = NetworkTestHarness.setupMap(
            null, // map
            0,    // n
            0,    // w
            0,    // min
            0,    // max
            0,    // radius
            0,    // resolution
            null, // periodic
            null,                 // clip
            Boolean.TRUE,         // forced
            "testfield",          // fieldName
            "int",                // fieldType
            "ScalarEncoder");     // encoderType
        
        // Attempt build omitting needed settings (should fail)
        try {
            MultiEncoder me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
            fail();
        }catch(Exception e) {
            assertEquals(IllegalStateException.class, e.getClass());
            assertEquals("W must be an odd number (to eliminate centering difficulty)", e.getMessage());
        }
        
        // Should pass
        settings.get("testfield").put("w", 5);
        settings.get("testfield").put("resolution", 0.1);
        settings.get("testfield").put("minVal", 1.0);
        settings.get("testfield").put("maxVal", 10000.0);
        MultiEncoder me = null;
        try {
            me = MultiEncoder.builder().name("").build();
            MultiEncoderAssembler.assemble(me, settings);
        }catch(Exception e) {e.printStackTrace();
            fail();
        }
        
        List<EncoderTuple> l = me.getEncoders(me); 
        assertTrue(!l.isEmpty());
        assertEquals(ScalarEncoder.class, l.get(0).getEncoder().getClass());
    }
}
