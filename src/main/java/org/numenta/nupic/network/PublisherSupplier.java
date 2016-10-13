/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.network.sensor.Header;
import org.numenta.nupic.network.sensor.Publisher;

import rx.subjects.PublishSubject;

/**
 * <p>
 * Ascribes to the {@link Supplier} interface to provide a {@link Publisher} upon request.
 * This supplier is expressed as a lambda which acts as a "lazy factory" to create Publishers
 * and set references on the Network when {@link PublisherSupplier#get()} is called.
 * </p><p>
 * The old way of creating Publishers has now changed when the {@link PAPI} is used.
 * Instead, a {@link PublisherSupplier} is used, which ascribes to the {@link Supplier} 
 * interface which acts like a "lazy factory" and kicks off other needed settings when a new
 * Publisher is created... 
 * </p><p>
 * The basic usage is:
 * </p><p>
 * <pre>
 *  Supplier<Publisher> supplier = PublisherSupplier.builder()
 *      .addHeader("dayOfWeek, timestamp")
 *      .addHeader("number, date")
 *      .addHeader("B, T")
 *      .build();
 *  
 *  // Since Suppliers are always added to Sensors we do...
 *  
 *  Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
 *      ObservableSensor::create, SensorParams.create(
 *          Keys::obs, new Object[] {"name", supplier})); // <-- supplier created above
 *          
 *  <b>--- OR (all inline in "Fluent" fashion) ---</b>
 *  
 *  Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
 *      ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
 *          PublisherSupplier.builder()                                                // <-- supplier created fluently
 *              .addHeader("dayOfWeek, timestamp")
 *              .addHeader("number")
 *              .addHeader("B, T").build() }));
 * </pre>
 * @author cogmission
 *
 */
public class PublisherSupplier implements Persistable, Supplier<Publisher> {
    private static final long serialVersionUID = 1L;
    
    /** The parent network this supplier services */
    private Network network;
    
    /** 3 Header lines used during csv parsing of input and type determination */
    private List<String> headers = new ArrayList<>();
    
    /** last created Publisher instance */
    private volatile transient Publisher suppliedInstance;
    
    /**
     * Package private constructor for use by the {@link Network} class only.
     * @param network   the network for which a Publisher is to be supplied.
     */
    private PublisherSupplier(Network network) {
        this.network = network;
    }

    /**
     * <p>
     * Implementation of the {@link Supplier} interface that returns a newly created 
     * {@link Publisher}. 
     * </p><p>
     * The {@link Publisher.Builder} is passed a {@link Consumer} in its constructor which 
     * basically triggers a call to {@link Network#setPublisher(Publisher)} with the newly
     * created {@link Publisher} - which must be available so that users can get a reference 
     * to the new Publisher that is created when {@link Network#load()} is called.
     * 
     * @return a new Publisher
     */
    @Override
    public Publisher get() {
        if(suppliedInstance == null) {
            Publisher.Builder<PublishSubject<String>> builder = 
                Publisher.builder(network == null ? null : p -> network.setPublisher(p));
            
            headers.stream().forEach(line -> builder.addHeader(line));
            
            suppliedInstance = builder.build();
            suppliedInstance.setNetwork(network);
        }
        
        return suppliedInstance;
    }
    
    public void clearSuppliedInstance() {
        this.suppliedInstance = null;
    }
    
    /**
     * Sets the {@link Network} for which this supplier supplies a publisher.
     * @param n the Network acting as consumer.
     */
    public void setNetwork(Network n) {
        this.network = n;
        this.suppliedInstance.setNetwork(n);
    }
    
    /**
     * <p>
     * Returns a {@link PublisherSupplier.Builder} which is used to build up 
     * a {@link Header} and then create a supplier. An example is:
     * </p>
     * <pre>
     *  Supplier<Publisher> supplier = PublisherSupplier.builder()
     *      .addHeader("dayOfWeek, timestamp")
     *      .addHeader("number, date")
     *      .addHeader("B, T")
     *      .build();
     *  
     *  // Since Suppliers are always added to Sensors we do...
     *  
     *  Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
     *      ObservableSensor::create, SensorParams.create(
     *          Keys::obs, new Object[] {"name", supplier})); // <-- supplier created above
     *          
     *  <b>--- OR (all inline in "Fluent" fashion) ---</b>
     *  
     *  Sensor<ObservableSensor<String[]>> sensor = Sensor.create(
     *      ObservableSensor::create, SensorParams.create(Keys::obs, new Object[] {"name", 
     *          PublisherSupplier.builder()                                                // <-- supplier created fluently
     *              .addHeader("dayOfWeek, timestamp")
     *              .addHeader("number")
     *              .addHeader("B, T").build() }));
     * </pre>
     * 
     * @return  a builder with which to create a {@link PublisherSupplier}
     */
    public static Builder builder() {
        return new Builder();
    }

    
    /**
     * Follows "builder pattern" for building new {@link PublisherSupplier}s.
     * 
     * @see Supplier
     * @see PublisherSupplier
     * @see Publisher
     * @see Publisher.Builder
     */
    public static class Builder {
        private Network network;
        
        private List<String> headers = new ArrayList<>();
        
        
        /**
         * Constructs a new {@code PublisherSupplier.Builder}
         */
        private Builder() {
            
        }
        
        /**
         * Adds a header line which in the case of a multi column input 
         * is a comma separated string.
         * 
         * @param s     string representing one line of a header
         * @return  this Builder
         */
        public Builder addHeader(String headerLine) {
            headers.add(headerLine);
            return this;
        }
        
        /**
         * Signals the builder to instantiate and return the new
         * {@code PublisherSupplier}
         * 
         * @return  a new PublisherSupplier
         */
        public PublisherSupplier build() {
            PublisherSupplier retVal = new PublisherSupplier(network);
            retVal.headers = new ArrayList<>(this.headers);
            return retVal;
        }
    }
    
}
