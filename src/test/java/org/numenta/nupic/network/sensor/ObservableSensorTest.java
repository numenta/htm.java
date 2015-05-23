package org.numenta.nupic.network.sensor;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;

import org.junit.Test;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Sensor;
import org.numenta.nupic.network.sensor.SensorParams;
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
    private Observable<String> makeObservable() {
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
        Object[] n = { "some name", makeObservable() };
        SensorParams parms = SensorParams.create(Keys::path, n);
        Sensor<ObservableSensor<String[]>> sensor = Sensor.create(ObservableSensor::create, parms);
        long count = sensor.getInputStream().count();
        assertEquals(4391, count);
    }

}
