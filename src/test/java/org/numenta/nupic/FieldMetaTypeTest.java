package org.numenta.nupic;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.joda.time.DateTime;
import org.junit.Test;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.util.Tuple;


public class FieldMetaTypeTest {

    /**
     * Test decoding of 9 out of 10 types of {@link FieldMetaType}s.
     */
    @Test
    public void testDecodeType() {
        List<String> linput = new ArrayList<>();
        linput.add("test");
        String loutput = FieldMetaType.LIST.decodeType(linput.toString(), null);
        assertEquals(linput.toString(), loutput);
        
        String catinput = "TestString";
        String output = FieldMetaType.STRING.decodeType(catinput, null);
        assertEquals(catinput, output);
        
        String binput = "true";
        Double boutput = FieldMetaType.BOOLEAN.decodeType(binput, null);
        assertEquals(new Double(1), boutput);
        
        String ginput = "100;200;5";
        Tuple texpected = new Tuple(100.0, 200.0, 5.0);
        Tuple tupleOut = FieldMetaType.GEO.decodeType(ginput, null);
        assertEquals(texpected, tupleOut);
        
        String intinput = "1337";
        double intoutput = FieldMetaType.INTEGER.decodeType(intinput, null);
        assertEquals(1337.0d, intoutput, 0);
        
        intoutput = FieldMetaType.FLOAT.decodeType(intinput, null);
        assertEquals(1337.0d, intoutput, 0);
        
        // DARR = Dense Array
        int[] dainput = { 1, 0, 1, 0 };
        int[] daoutput = FieldMetaType.DARR.decodeType(Arrays.toString(dainput), null);
        assertTrue(Arrays.equals(dainput, daoutput));
        
        // SARR = Sparse Array
        int[] sainput = { 0, 2 };
        int[] saoutput = FieldMetaType.SARR.decodeType(Arrays.toString(sainput), null);
        assertTrue(Arrays.equals(sainput, saoutput));
        
        DateTime comparison = new DateTime(2010, 11, 4, 13, 55, 01);
        String compareString = "2010-11-04 13:55:01";
        // 3 bits for season, 1 bit for day of week, 3 for weekend, 5 for time of day
        // use of forced is not recommended, used here for readability.
        DateEncoder.Builder builder = DateEncoder.builder();
        builder.formatPattern("yyyy-MM-dd HH:mm:ss");

        DateEncoder de = builder.season(3)
            .dayOfWeek(1)
            .weekend(3)
            .timeOfDay(5).build();
        
        DateTime dateOutput = FieldMetaType.DATETIME.decodeType(compareString, de);
        assertEquals(comparison, dateOutput);
    }
}
