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

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import org.numenta.nupic.network.Layer;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.Region;

import rx.Observable;

/**
 * Wraps an {@link rx.Observable} or {@link Publisher} event emitter which can be used to input CSV
 * strings into a given {@link Layer} of a {@link Region} and {@link Network} by
 * either manually calling {@link Publisher#onNext(String)} or by connecting an Observable
 * to an existing chain of Observables (operations/transformations) which eventually yield an appropriate CSV
 * String input.
 * 
 * @author metaware
 *
 * @param <T>
 */
public class ObservableSensor<T> implements Sensor<Observable<T>> {
    private static final long serialVersionUID = 1L;
    
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    private static final boolean DEFAULT_PARALLEL_MODE = false;
    
    private transient BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    
    /**
     * Creates a new {@code ObservableSensor} using the specified 
     * {@link SensorParams}
     * 
     * @param params
     */
    @SuppressWarnings("unchecked")
    public ObservableSensor(SensorParams params) {
        if(!params.hasKey("ONSUB")) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: no key for \"ONSUB\"");
        }
        
        this.params = params;
        
        Observable<String> obs = null;
        Object publisher = params.get("ONSUB");
        if(publisher instanceof Publisher) {
            obs = ((Publisher)publisher).observable();
        } else if(publisher instanceof Supplier<?>) {
            obs = ((Supplier<Publisher>)publisher).get().observable();
        } else {
            obs = (Observable<String>)publisher; 
        }
        
        Iterator<String> observerator = obs.toBlocking().getIterator();
        
        Iterator<String> iterator = new Iterator<String>() {
            @Override public boolean hasNext() { return observerator.hasNext(); }
            @Override public String next() {
                return observerator.next();
            }
        };
                
        int characteristics = Spliterator.SORTED | Spliterator.ORDERED;
        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
      
        this.stream = BatchedCsvStream.batch(
            StreamSupport.stream(spliterator, false), BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Sensor<T> create(SensorParams p) {
        ObservableSensor<String[]> sensor = 
            (ObservableSensor<String[]>)new ObservableSensor<String[]>(p);
        
        return (Sensor<T>)sensor;
    }
    
    /**
     * Returns the {@link SensorParams} object used to configure this
     * {@code ObservableSensor}
     * 
     * @return the SensorParams
     */
    @Override
    public SensorParams getSensorParams() {
        return params;
    }
    
    /**
     * Returns the configured {@link MetaStream}.
     * 
     * @return  the MetaStream
     */
    @SuppressWarnings("unchecked")
    public <K> MetaStream<K> getInputStream() {
        return (MetaStream<K>)stream;
    }
    
    /**
     * Returns the values specifying meta information about the 
     * underlying stream.
     */
    public ValueList getMetaInfo() {
        return stream.getMeta();
    }

}
