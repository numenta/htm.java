package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.SensorParams.Keys;

import rx.Observable;
import rx.subjects.ReplaySubject;


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
    
    @Test
    public void testOpenObservableWithExplicitEntry() {
        final ReplaySubject<String> manual = ReplaySubject.create();
        manual.onNext("timestamp,consumption");
        manual.onNext("datetime,float");
        manual.onNext("B");
        
        Object[] n = { "some name", manual };
        SensorParams parms = SensorParams.create(Keys::obs, n);
        final Sensor<ObservableSensor<String>> sensor = Sensor.create(ObservableSensor::create, parms);
        
        (new Thread() {
            public void run() {
                sensor.getInputStream().forEach(l -> { System.out.println(Arrays.toString((String[])l)); });
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

}
