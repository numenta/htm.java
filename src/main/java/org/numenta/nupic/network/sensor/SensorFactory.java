package org.numenta.nupic.network.sensor;

import java.io.File;
import java.net.URI;

import rx.Observable;

/**
 * <p>
 * Allows a level of indirection which makes calling {@link Sensor#create(SensorFactory, SensorParams)}
 * a lot more concise (and usable in "fluent" style): Use...
 * </p>
 * <p>
 * <pre>
 * Sensor.create(FileSensor::create, SensorParams); //Can be URISensor::create, or ObservableSensor::create
 * </pre>
 * <p>
 * 
 * @author David Ray
 * @see Sensor
 * @param <T>   The resource type (i.e. {@link File}, {@link URI}, {@link Observable})
 */
@FunctionalInterface
public interface SensorFactory<T> {
    /**
     * Returns the implemented type of {@link Sensor} configured
     * using the specified {@link SensorParams}
     * 
     * @param params    the {@link SensorParams} to use for configuration.
     * @return
     */
    public Sensor<T> create(SensorParams params);
}
