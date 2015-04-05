package org.numenta.nupic.network.sensor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.stream.Stream;

import org.numenta.nupic.ValueList;
import org.numenta.nupic.network.Network;

/**
 * Default implementation of a {@link Sensor} for inputting data from
 * a file.
 * 
 * All {@link Sensor}s represent the bottom-most level of any given {@link Network}. 
 * Sensors are used to connect to a data source and feed data into the Network, therefore
 * there are no nodes beneath them or which precede them within the Network hierarchy, in
 * terms of data flow. In fact, a Sensor will throw an {@link Exception} if an attempt to 
 * connect another {@link Node} to the input of a Node containing a Sensor is made.
 *  
 * @author David Ray
 * @see SensorFactory
 * @see Sensor#create(SensorFactory, SensorParams)
 */
public class FileSensor implements Sensor<File> {
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    // This is OFF until Encoders are made concurrency safe
    private static final boolean DEFAULT_PARALLEL_MODE = false;
    
    private BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    /**
     * Private constructor. Instances of this class should be obtained 
     * through the {@link #create(SensorParams)} factory method.
     * 
     * @param params
     */
    private FileSensor(SensorParams params) {
        this.params = params;
        
        if(!params.hasKey("PATH")) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: no key for \"PATH\"");
        }
        
        File f = new File((String)params.get("PATH"));
        if(!f.exists()) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: invalid PATH: " + params.get("PATH"));
        }
        
        try {
            Stream<String> stream = Files.lines(f.toPath(), Charset.forName("UTF-8"));
            this.stream = BatchedCsvStream.batch(
                stream, BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Factory method to allow creation through the {@link SensorFactory} in
     * the {@link Sensor#create(SensorFactory, SensorParams)} method of the 
     * parent {@link Sensor} class. This indirection allows the decoration of 
     * the returned {@code Sensor} type by wrapping it in an {@link HTMSensor}
     * (which is the current implementation but could be any wrapper).
     * 
     * @param p     the {@link SensorParams} which describe connection or source
     *              data details.
     * @return      the Sensor.
     */
    public static Sensor<File> create(SensorParams p) {
        Sensor<File> fs = new FileSensor(p);
        return fs;
    }
    
    @Override
    public SensorParams getParams() {
        return params;
    }
    
    /**
     * Returns the configured {@link MetaStream} if this is of
     * Type Stream, otherwise it throws an {@link UnsupportedOperationException}
     * 
     * @return  the MetaStream
     */
    @SuppressWarnings("unchecked")
    @Override
    public <K> MetaStream<K> getInputStream() {
        return (MetaStream<K>)stream;
    }
    
    /**
     * Returns the values specifying meta information about the 
     * underlying stream.
     */
    public ValueList getMeta() {
        return stream.getMeta();
    }

}
