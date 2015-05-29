package org.numenta.nupic.network.sensor;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.StreamSupport;

import org.numenta.nupic.ValueList;

import rx.Observable;


public class ObservableSensor<T> implements Sensor<Observable<T>> {
    private static final int HEADER_SIZE = 3;
    private static final int BATCH_SIZE = 20;
    private static final boolean DEFAULT_PARALLEL_MODE = false;
    
    private BatchedCsvStream<String[]> stream;
    private SensorParams params;
    
    
    @SuppressWarnings("unchecked")
    public ObservableSensor(SensorParams params) {
        if(!params.hasKey("ONSUB")) {
            throw new IllegalArgumentException("Passed improperly formed Tuple: no key for \"ONSUB\"");
        }
        
        this.params = params;
        
        Observable<String> obs = (Observable<String>)params.get("ONSUB");
        Iterator<String> observerator = obs.toBlocking().getIterator();
        
        Iterator<String> iterator = new Iterator<String>() {
            @Override public boolean hasNext() { return observerator.hasNext(); }
            @Override public String next() {
                return observerator.next();
            }
        };
                
        int characteristics = Spliterator.SORTED | Spliterator.ORDERED;
        Spliterator<String> spliterator = Spliterators.spliteratorUnknownSize(iterator, characteristics);
      
        if(params.get("HEADER") != null) {
            this.stream = BatchedCsvStream.batch(
                StreamSupport.stream(spliterator, false), BATCH_SIZE, DEFAULT_PARALLEL_MODE, characteristics, 
                    HEADER_SIZE, (List<String[]>)params.get("HEADER"));
        }else{
            this.stream = BatchedCsvStream.batch(
                StreamSupport.stream(spliterator, false), BATCH_SIZE, DEFAULT_PARALLEL_MODE, HEADER_SIZE);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> Sensor<T> create(SensorParams p) {
        ObservableSensor<String[]> sensor = 
            (ObservableSensor<String[]>)new ObservableSensor<String[]>(p);
        
        return (Sensor<T>)sensor;
    }
    
    @Override
    public SensorParams getParams() {
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
    public ValueList getHeader() {
        return stream.getMeta();
    }

}
