package org.numenta.nupic.network;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;

import org.numenta.nupic.ValueList;


public class URISensor implements Sensor<URI>  {
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    private static final boolean DEFAULT_PARALLEL_MODE = true;
    
    private BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    public URISensor(SensorParams params) {
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
    
    public static Sensor<URI> create(SensorParams p) {
        URISensor sensor = new URISensor(p);
        
        return sensor;
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
    public <K> MetaStream<K> getStream() {
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
