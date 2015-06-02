package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.ValueList;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.SensorParams.Keys;
import org.numenta.nupic.util.Tuple;

/**
 * Tests {@link Header} condition flag configuration and 
 * state management.
 * 
 * @author David Ray
 * @see SensorFlags
 * @see FieldMetaType
 * @see Header
 */
public class HeaderTest {
    @Test
    public void testHeader() {
        Object[] n = { "some name", ResourceLocator.path("rec-center-hourly-4reset.csv") };
        
        Sensor<File> sensor2 = Sensor.create(
            FileSensor::create, 
                SensorParams.create(Keys::path, n));
        
        Header header = new Header(sensor2.getMetaInfo());
        assertEquals("[T, B, R]", header.getFlags().toString());
    }

    @Test
    public void testProcessSequence() {
        Header header = new Header(getTestHeaderOff());
        List<String[]> lines = getLines(ResourceLocator.path("rec-center-hourly-4period.csv"));
        
        for(String[] line: lines) {
            header.process(line);
            assertFalse(header.isReset());
            assertTrue(header.isLearn());
        }
        
        header = new Header(getTestHeaderSeq());
        lines = getLines(ResourceLocator.path("rec-center-hourly-4seqReset.csv"));
        int idx = 0;
        for(String[] line : lines) {
            String[] shifted = new String[line.length + 1];
            System.arraycopy(line, 0, shifted, 1, line.length);
            shifted[0] = String.valueOf(idx);
            
            header.process(shifted);
            
            if(idx > 0 && idx % 24 == 0) {
                assertTrue(header.isReset());
            }else{
                assertFalse(header.isReset());
            }
            idx++;
        }
    }
    
    @Test
    public void testProcessReset() {
        Header header = new Header(getTestHeaderOff());
        List<String[]> lines = getLines(ResourceLocator.path("rec-center-hourly-4period.csv"));
        
        for(String[] line: lines) {
            header.process(line);
            assertFalse(header.isReset());
            assertTrue(header.isLearn());
        }
        
        header = new Header(getTestHeaderReset());
        lines = getLines(ResourceLocator.path("rec-center-hourly-4reset.csv"));
        int idx = 0;
        for(String[] line : lines) {
            String[] shifted = new String[line.length + 1];
            System.arraycopy(line, 0, shifted, 1, line.length);
            shifted[0] = String.valueOf(idx);
            
            header.process(shifted);
            
            if(line[2].equals("1")) {
                assertTrue(header.isReset());
            }else{
                assertFalse(header.isReset());
            }
            idx++;
        }
    }
    
    @Test
    public void testProcessLearn() {
        Header header = new Header(getTestHeaderOff());
        List<String[]> lines = getLines(ResourceLocator.path("rec-center-hourly-4period.csv"));
        
        for(String[] line: lines) {
            header.process(line);
            assertFalse(header.isReset());
            assertTrue(header.isLearn());
        }
        
        header = new Header(getTestHeaderReset());
        lines = getLines(ResourceLocator.path("rec-center-hourly-4learn.csv"));
       
        int idx = 0;
        for(String[] line : lines) {
            String[] shifted = new String[line.length + 1];
            System.arraycopy(line, 0, shifted, 1, line.length);
            shifted[0] = String.valueOf(idx);
            
            header.process(shifted);
            
            if(line[2].equals("1")) {
                assertTrue(header.isReset());
            }else{
                assertFalse(header.isReset());
            }
            idx++;
        }
    }
    
    private List<String[]> getLines(String path) {
        List<String[]> retVal = new ArrayList<>();
        File f = new File(path);
        BufferedReader buf = null;
        try {
            buf = new BufferedReader(new FileReader(f));
            String line = null;
            int headerCount = 0;
            while((line = buf.readLine()) != null) {
                if(headerCount++ < 3) continue;
                retVal.add(line.split("\\,"));
            }
        }catch(Exception e) {
            e.printStackTrace();
        }finally{
            try {
                buf.close();
            }catch(Exception ignore) {}
        }
        
        return retVal;
    }

    private ValueList getTestHeaderOff() {
        return new ValueList() {
            Tuple[] ta = new Tuple[] {
                new Tuple("timestamp", "consumption"),
                new Tuple("datetime", "float"),
                new Tuple("T"),
            };

            @Override
            public Tuple getRow(int row) {
                return ta[row];
            }

            @Override
            public int size() {
                return ta.length;
            }
        };
    }
    
    private ValueList getTestHeaderSeq() {
        return new ValueList() {
            Tuple[] ta = new Tuple[] {
                new Tuple("timestamp", "consumption"),
                new Tuple("datetime", "float"),
                new Tuple("T", "B", "S"),
            };

            @Override
            public Tuple getRow(int row) {
                return ta[row];
            }

            @Override
            public int size() {
                return ta.length;
            }
        };
    }
    
    private ValueList getTestHeaderReset() {
        return new ValueList() {
            Tuple[] ta = new Tuple[] {
                new Tuple("timestamp", "consumption"),
                new Tuple("datetime", "float"),
                new Tuple("T", "B", "R"),
            };

            @Override
            public Tuple getRow(int row) {
                return ta[row];
            }

            @Override
            public int size() {
                return ta.length;
            }
        };
    }
    
    public static void main(String[] args) {
        File f = new File("/Users/cogmission/git/htm.java/src/test/resources/rec-center-hourly-4period.csv");
        File fout = new File("/Users/cogmission/git/htm.java/src/test/resources/rec-center-hourly-4temp.csv");
        BufferedReader buf = null;
        PrintWriter p = null;
        try {
            buf = new BufferedReader(new FileReader(f));
            p = new PrintWriter(new FileWriter(fout));
            String line = null;
            int counter = 0;
            int headerCount = 0;
            while((line = buf.readLine()) != null) {
                if(headerCount++ > 2) {
                    if(counter < 24) {
                        line = line.concat(",.");
                        counter++;
                    }else{
                        line = line.concat(",?");
                        counter = 0;
                    }
                }
                p.println(line);
            }
        }catch(Exception e) {
            e.printStackTrace();
        }finally{
            try {
                buf.close();
                p.flush();
                p.close();
            }catch(Exception ignore) {}
        }
    }
}
