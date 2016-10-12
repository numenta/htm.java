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

import java.io.File;
import java.net.URI;
import java.util.stream.Stream;

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.network.Network;

import rx.Observable;

/**
 * Parent type for all {@link Sensor}s. This type describes strategies
 * used to connect to a data source on behalf of an HTM network
 * (see {@link Network}). Subtypes of this type use {@link SensorParams}
 * to configure the connection and location details. In this way, Sensors
 * may be used to connect {@link Stream}s of data in multiple ways; either
 * from a file or URL, or in a functional/reactive way via an {@link rx.Observable}
 * or {@link Publisher} from this library.
 * 
 * @author David Ray
 * @see SensorParams
 * @see SensorFactory
 * @see Publisher
 * @see Network
 * @param <T>       the resource type to retrieve (i.e. {@link File}, {@link URI}, {@link Observable}
 */
public interface Sensor<T> extends Persistable {
    
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
     * Returns an instance of {@link SensorParams} used 
     * to initialize the different types of Sensors with
     * their resource location or source object.
     * 
     * @return a {@link SensorParams} object.
     */
    public SensorParams getSensorParams();
    
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
    public ValueList getMetaInfo();
    
    
}
