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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.SensorParams.Keys;

import rx.Observable;


/**
 * <ul>
 * <li>Test that the ObservableSensor can be created using the same
 * idioms as other {@link Sensor} types.
 * </li>
 * <li>Test that the {@link SensorFactory}
 * "functional" method of creation works generically.
 * </li>
 * <li>Test that a {@link Stream}
 * can be retrieved from the Sensor, and that that Stream can be "operated"
 * upon. 
 * </li>
 * <li>Test that the Stream operation results are consistent.
 * </li>
 * </ul>
 * 
 * @author David Ray
 */
public class ObservableSensorTest {
    
    @SuppressWarnings("unchecked")
    private Observable<String> makeFileObservable() {
        File f = new File(ResourceLocator.path("rec-center-hourly.csv"));
        try {
            Observable<?> o = Observable.from(Files.lines(f.toPath(), Charset.forName("UTF-8")).toArray());
            return (Observable<String>)o;
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * <ul>
     * <li>Test that the ObservableSensor can be created using the same
     * idioms as other {@link Sensor} types.
     * </li>
     * <li>Test that the {@link SensorFactory}
     * "functional" method of creation works generically.
     * </li>
     * <li>Test that a {@link Stream}
     * can be retrieved from the Sensor, and that that Stream can be "operated"
     * upon. 
     * </li>
     * <li>Test that the Stream operation results are consistent.
     * </li>
     * </ul>
     */
    @Test
    public void testObservableFromFile() {
        Object[] n = { "some name", makeFileObservable() };
        SensorParams parms = SensorParams.create(Keys::obs, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        long count = sensor.getInputStream().count();
        assertEquals(4391, count);
    }
    
    /**
     * Test input is "sequenced" and is processed by underlying HTMSensor
     */
    @Test
    public void testOpenObservableWithExplicitEntry() {
        Publisher manual = Publisher.builder()
            .addHeader("timestamp,consumption")
            .addHeader("datetime,float")
            .addHeader("B")
            .build();
        
        Object[] n = { "some name", manual };
        SensorParams parms = SensorParams.create(Keys::obs, n);
        final Sensor<ObservableSensor<String>> sensor = Sensor.create(ObservableSensor::create, parms);
        
        // Test input is "sequenced" and is processed by underlying HTMSensor
        final String[] expected = new String[] {
            "[0, 7/2/10 0:00, 21.2]",
            "[1, 7/2/10 1:00, 34.0]",
            "[2, 7/2/10 2:00, 40.4]",
            "[3, 7/2/10 3:00, 123.4]"           
        };
        
        (new Thread() {
            int i = 0;
            public void run() {
                sensor.getInputStream().forEach(l -> { 
                    System.out.println(Arrays.toString((String[])l));
                    assertEquals(Arrays.toString((String[])l), expected[i++]);
                });
            }
        }).start();
        
        
        String[] entries = { 
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
            "7/2/10 3:00,123.4",
        };
        manual.onNext(entries[0]);
        manual.onNext(entries[1]);
        manual.onNext(entries[2]);
        manual.onNext(entries[3]);
    }
    
    @Test
    public void testReadIntegerArray() {
        try {
            Stream<String> s = Files.lines(Paths.get(ResourceLocator.path("1_100.csv")));
            @SuppressWarnings("resource")
            int[][] ia = s.skip(3).map(l -> l.split("[\\s]*\\,[\\s]*")).map(i -> {
                return Arrays.stream(i).mapToInt(Integer::parseInt).toArray();
            }).toArray(int[][]::new);  
            
            s.close();
            
            assertEquals(100, ia.length);
            assertTrue(Arrays.toString(ia[0]).equals(
                "[0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0, 0, 0, 0, "
                + "1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, "
                + "0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, "
                + "1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, "
                + "1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, "
                + "0, 0, 0, 0, 0, 0, 0, 0, 0, 0]"));
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    @SuppressWarnings("unused")
    @Test
    public void testInputIntegerArray() {
        Publisher manual = Publisher.builder()
            .addHeader("sdr_in")
            .addHeader("sarr")
            .addHeader("B")
            .build();
        
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
            ObservableSensor::create, 
                SensorParams.create(
                    Keys::obs, new Object[] {"name", manual}));
        
        Thread t = null;
        (t = new Thread() {
            int i = 0;
            public void run() {
                assertEquals(2, ((String[])sensor.getInputStream().findFirst().get()).length);
            }
        }).start();
        
        int[][] ia = getArrayFromFile(ResourceLocator.path("1_100.csv"));
        manual.onNext(Arrays.toString(ia[0]).trim());
        try { t.join(); }catch(Exception e) { e.printStackTrace(); }
    }

    private int[][] getArrayFromFile(String path) {
        int[][] retVal = null;
        
        try {
            Stream<String> s = Files.lines(Paths.get(path));
            
            @SuppressWarnings("resource")
            int[][] ia = s.skip(3).map(l -> l.split("[\\s]*\\,[\\s]*")).map(i -> {
                return Arrays.stream(i).mapToInt(Integer::parseInt).toArray();
            }).toArray(int[][]::new);  
            
            retVal = ia;
            
            s.close();
        }catch(Exception e) {
            e.printStackTrace();
        }
        return retVal;
    }
}
