package org.numenta.nupic.network;

import java.io.File;
import java.net.URI;
import java.util.stream.Stream;

import org.numenta.nupic.ValueList;

import rx.Observable;

/**
 * Parent type for all {@link Sensor}s. This type describes strategies
 * used to connect to a data source on behalf of an HTM network
 * (see {@link Network}). Subtypes of this type use {@link SensorParams}
 * to configure the connection and location details.
 * 
 * @author David Ray
 * @see SensorParams
 * @see SensorFactory
 * @see Network
 * @param <T>       the resource type to retrieve (i.e. {@link File}, {@link URI}, {@link Observable}
 */
public interface Sensor<T> {
    
    /**
     * <p>
     * Creates and returns the {@link Sensor} subtype indicated by the 
     * method reference passed in for the SensorFactory {@link FunctionalInterface}
     * argument. <br><br><b>Typical usage is as follows:</b>
     * </p>
     * <p>
     * <pre>
     * Sensor.create(FileSensor::create, SensorParams); //Can be URISensor, or ObservableSensor
     * </pre>
     * <p>
     * 
     * @param sf    the {@link SensorFactory} or method reference. SensorFactory is a {@link FunctionalInterface}
     * @param t     the {@link SensorParams} which hold the configuration and data source details.
     * @return
     */
    public static <T> Sensor<T> create(SensorFactory<T> sf, SensorParams t) {
        if(sf == null) {
            throw new IllegalArgumentException("Factory cannot be null");
        }
        if(t == null) {
            throw new IllegalArgumentException("Properties (i.e. \"SensorParams\") cannot be null");
        }
        
        return new HTMSensor<T>(sf.create(t));
    }
    
    /**
     * Returns the {@link SensorParams} containing source input configuration details.
     * @return
     */
    public SensorParams getParams();
    
    /**
     * Returns the configured {@link Stream} if this is of
     * Type Stream, otherwise it throws an {@link UnsupportedOperationException}
     * 
     * @return the constructed Stream
     */
    public <K> MetaStream<K> getInputStream();
    
    /**
     * Returns the inner Stream's meta information.
     * @return
     */
    public ValueList getMeta();
    
    
}
