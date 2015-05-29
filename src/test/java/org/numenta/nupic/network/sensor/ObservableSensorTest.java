package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Ignore;
import org.junit.Test;
import org.numenta.nupic.ValueList;
import org.numenta.nupic.datagen.NetworkInputKit;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.SensorParams.Keys;

import rx.Observable;
import rx.subjects.PublishSubject;


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
    public void testCanRetrieveStream() {
        Object[] n = { "some name", makeFileObservable() };
        SensorParams parms = SensorParams.create(Keys::obs, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        long count = sensor.getInputStream().count();
        assertEquals(4391, count);
    }
    
    @Test
    public void testProgrammaticStream() {
        NetworkInputKit kit = new NetworkInputKit();
        Object[] n = { "some name", kit.observe() };
        SensorParams parms = SensorParams.create(Keys::obs, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        long count = sensor.getInputStream().count();
        assertEquals(4391, count);
    }
    
    int headerIdx = 0;
    @Ignore
    public void testProgrammaticStreamX() {
        PublishSubject<String> manual = PublishSubject.create();
        final String[][] header = new String[][] {
            { "timestamp", "consumption"},
            { "datetime", "float"},
            { "B" }
        };
        Object[] n = { "some name", manual, Arrays.asList(header) };
        
        SensorParams parms = SensorParams.create(new String[] { "", "ONSUB", "HEADER" }, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        
        ValueList headerValues = sensor.getHeader();
        assertEquals(3, headerValues.size());
        
        String[][] expected = {
            { "0", "7/2/10 0:00", "21.2" },
            { "1", "7/2/10 1:00", "34.0" },
            { "2", "7/2/10 2:00", "40.4" },
        };
        
        final Stream<String[]> testStream = sensor.getInputStream();
        
        Thread testThread = null;
        (testThread = new Thread() {
            public void run() {
                testStream.map(i -> {
                    switch(headerIdx) {
                        case 0: assertTrue(Arrays.equals((String[])expected[0], (String[])i)); break;
                        case 1: assertTrue(Arrays.equals((String[])expected[1], (String[])i)); break;
                        case 2: assertTrue(Arrays.equals((String[])expected[2], (String[])i)); break;
                    }
                    ++headerIdx;
                    return (String[])i;
                });
                testStream.count();
            }
        }).start();
        
       
        String[] entries = { 
            "timestamp", "consumption",
            "datetime", "float",
            "B",
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,34.0",
            "7/2/10 2:00,40.4",
        };
        manual.onNext(entries[0]);
        manual.onNext(entries[1]);
        manual.onNext(entries[2]);
        testStream.close();
        
        try {
            testThread.join();
            assertEquals(2, headerIdx);
        }catch(Exception e) {
            e.printStackTrace();
        }
        
    }

}
