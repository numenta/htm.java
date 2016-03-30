package org.numenta.nupic.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.numenta.nupic.Persistable;
import org.numenta.nupic.network.sensor.Header;
import org.numenta.nupic.network.sensor.Publisher;

import rx.subjects.PublishSubject;


public class PublisherSupplier implements Persistable, Supplier<Publisher> {
    private static final long serialVersionUID = 1L;
    
    private Network network;
    
    private List<String> headers = new ArrayList<>();
    
    private volatile transient Publisher suppliedInstance;
    
    /**
     * Package private constructor for use by the {@link Network} class only.
     * @param network   the network for which a Publisher is to be supplied.
     */
    private PublisherSupplier(Network network) {
        this.network = network;
    }

    /**
     * Implementation of the {@link Supplier} interface
     * that returns a newly created {@link Publisher}
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
