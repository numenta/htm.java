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
