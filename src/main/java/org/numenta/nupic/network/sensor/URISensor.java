package org.numenta.nupic.network.sensor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.numenta.nupic.ValueList;

/**
 * Sensor which creates its source stream from a {@link URI}
 * Instances of this class should be obtained via the factory
 * {@link #create(SensorParams)} method.
 * 
 * @author David Ray
 * @see SensorFactory
 * @see Sensor#create(SensorFactory, SensorParams)
 */
public class URISensor implements Sensor<URI>  {
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    private static final boolean DEFAULT_PARALLEL_MODE = false;
    
    private BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    
    /**
     * Private constructor. Instances of this class should be obtained 
     * through the {@link #create(SensorParams)} factory method.
     * 
     * @param params
     */
    private URISensor(SensorParams params) {
        if(!params.hasKey("URI")) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: no key for \"URI\"");
        }
        
        this.params = params;
        
        BufferedReader br = null;
        try {
            InputStream is = new URL((String)params.get("URI")).openStream();
            br = new BufferedReader(new InputStreamReader(is));
        }catch(Exception e) {
            e.printStackTrace();
        }
        
        this.stream = BatchedCsvStream.batch(br.lines(), BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
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
    public static Sensor<URI> create(SensorParams p) {
        URISensor sensor = new URISensor(p);
        return sensor;
    }
    
    /**
     * Returns the {@link SensorParams} used to configure this {@code URISensor}
     */
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
