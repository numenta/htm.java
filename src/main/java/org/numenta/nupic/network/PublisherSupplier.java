package org.numenta.nupic.network;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.numenta.nupic.network.sensor.Publisher;

import rx.subjects.PublishSubject;


public class PublisherSupplier implements Serializable, Supplier<Publisher> {
    private static final long serialVersionUID = 1L;
    
    private Network network;
    
    private List<String> headers = new ArrayList<>();
    
    
    
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
        Publisher.Builder<PublishSubject<String>> builder = 
            Publisher.builder(network == null ? null : p -> network.setPublisher(p));
        
        headers.stream().forEach(line -> builder.addHeader(line));
        
        return builder.build();
    }
    
    /**
     * Returns a {@code Builder} capable of building a {@link PublisherSupplier}
     * 
     * @param network   the network for which the {@link Supplier} is created.
     * @return  a new {@code PublisherSupplier.Builder}
     */
    public static Builder builder(Network network) {
        return new Builder(network);
    }

    
    /**
     * Follows "builder patter" for building new {@link PublisherSupplier}s.
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
         * @param network   the {@link Network} for which a new {@link Publisher}
         *                  will be created.
         */
        private Builder(Network network) {
            this.network = network;
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
        
        public PublisherSupplier build() {
            PublisherSupplier retVal = new PublisherSupplier(network);
            retVal.headers = new ArrayList<>(this.headers);
            return retVal;
        }
    }
    
}
