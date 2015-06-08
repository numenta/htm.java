/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.algorithms.CLAClassifier;
import org.numenta.nupic.encoders.AdaptiveScalarEncoder;
import org.numenta.nupic.encoders.CategoryEncoder;
import org.numenta.nupic.encoders.CoordinateEncoder;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.DeltaEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.Encoder.Builder;
import org.numenta.nupic.encoders.EncoderTuple;
import org.numenta.nupic.encoders.GeospatialCoordinateEncoder;
import org.numenta.nupic.encoders.LogEncoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.encoders.RandomDistributedScalarEncoder;
import org.numenta.nupic.encoders.SDRCategoryEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;
import org.numenta.nupic.util.Tuple;


/**
 * <p>
 * Decorator for {@link Sensor} types adding HTM
 * specific functionality to sensors and streams.
 * </p><p>
 * The {@code HTMSensor} decorates the sensor with the expected
 * meta data containing field name and field type information 
 * together with the information needed in order to auto-create
 * {@link Encoder}s necessary to output a bit vector specifically
 * tailored for HTM (Hierarchical Temporal Memory) input.
 * </p><p>
 * This class also has very specific date handling capability for
 * the "timestamp" data field type.
 * </p><p>
 * Output is attained by calling {@link #getOutputStream()}. This class
 * extends the Stream API to be able to "fork" streams, so that
 * a single stream can supply multiple fanouts.
 * </p><p>
 * <b>Warning:</b> if {@link #getOutputStream()} is called multiple times,
 * all calls must precede any operations on any of the supplied streams. 
 * </p><p>
 * @author David Ray
 *
 * @param <T>   the input type (i.e. File, URL, etc.)
 */
public class HTMSensor<T> implements Sensor<T> {
    private Sensor<T> delegate;
    private Header header;
    private Parameters localParameters;
    private MultiEncoder encoder;
    private Stream<int[]> outputStream;
    private List<int[]> output;
    private InputMap inputMap;
    
    private TIntObjectMap<Encoder<?>> indexToEncoderMap;
    private TObjectIntHashMap<String> indexFieldMap = new TObjectIntHashMap<String>();
    
    
    private Iterator<int[]> mainIterator;
    private List<LinkedList<int[]>> fanOuts = new ArrayList<>();
    
    /** Protects {@ #mainIterator} formation and the next() call */
    private Lock criticalAccessLock = new ReentrantLock();
    
    
    
    /**
     * Decorator pattern to construct a new HTMSensor wrapping the specified {@link Sensor}.
     * 
     * @param sensor
     */
    public HTMSensor(Sensor<T> sensor) {
        this.delegate = sensor;
        header = new Header(sensor.getInputStream().getMeta());
        if(header == null || header.size() < 3) {
            throw new IllegalStateException("Header must always be present; and have 3 lines.");
        }
        createEncoder();
    }
    
    /**
     * Called internally during construction to build the encoders
     * needed to process the configured field types.
     */
    @SuppressWarnings("unchecked")
    private void createEncoder() {
        encoder = MultiEncoder.builder().name("MultiEncoder").build();
        
        Map<String, Map<String, Object>> encoderSettings;
        if(localParameters != null && 
            (encoderSettings = (Map<String, Map<String, Object>>)localParameters.getParameterByKey(KEY.FIELD_ENCODING_MAP)) != null &&
                !encoderSettings.isEmpty()) {
            
            initEncoders(encoderSettings);
            makeIndexEncoderMap();
        }
    }
    
    /**
     * Sets up a mapping which describes the order of occurrence of comma
     * separated fields - mapping their ordinal position to the {@link Encoder}
     * which services the encoding of the field occurring in that position. This
     * sequence of types is contained by an instance of {@link Header} which
     * makes available an array of {@link FieldMetaType}s.
     *    
     */
    private void makeIndexEncoderMap() {
        indexToEncoderMap = new TIntObjectHashMap<Encoder<?>>();
        
        final FieldMetaType[] fieldTypes = header.getFieldTypes().toArray(new FieldMetaType[header.getFieldTypes().size()]);
        
        for(int i = 0;i < fieldTypes.length;i++) {
            switch(fieldTypes[i]) {
                case DATETIME:
                    Optional<DateEncoder> de = getDateEncoder(encoder);
                    if(de.isPresent()) {
                        indexToEncoderMap.put(i, de.get());
                    }else{
                        throw new IllegalArgumentException("DateEncoder never initialized.");
                    }
                    break;
                case BOOLEAN:
                case FLOAT:
                case INTEGER:
                    Optional<Encoder<?>> opt = getNumberEncoder(encoder);
                    if(opt.isPresent()) {
                        indexToEncoderMap.put(i, opt.get());
                    }else{
                        throw new IllegalArgumentException("Number (Boolean also) encoder never initialized.");
                    }
                    break;
                case LIST:
                case STRING:
                    opt = getCategoryEncoder(encoder);
                    if(opt.isPresent()) {
                        indexToEncoderMap.put(i, opt.get());
                    }else{
                        throw new IllegalArgumentException("Category encoder never initialized.");
                    }
                    break;
                case COORD:
                case GEO:
                    opt = getCoordinateEncoder(encoder);
                    if(opt.isPresent()) {
                        indexToEncoderMap.put(i, opt.get());
                    }else{
                        throw new IllegalArgumentException("Coordinate encoder never initialized.");
                    }
                    break;
                default:
                    break;
            }
        }
        
    }

    /**
     * Returns an instance of {@link SensorParams} used 
     * to initialize the different types of Sensors with
     * their resource location or source object.
     * 
     * @return a {@link SensorParams} object.
     */
    @Override
    public SensorParams getParams() {
        return delegate.getParams();
    }

    /**
     * <p>
     * Main method by which this Sensor's information is retrieved.
     * </p><p>
     * This method returns a subclass of Stream ({@link MetaStream})
     * capable of returning a flag indicating whether a terminal operation
     * has been performed on the stream (i.e. see {@link MetaStream#isTerminal()});
     * in addition the MetaStream returned can return meta information (see
     * {@link MetaStream#getMeta()}.
     * </p>
     * @return  a {@link MetaStream} instance.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <K> MetaStream<K> getInputStream() {
        return (MetaStream<K>)delegate.getInputStream();
    }
    
    /**
     * Customized Iterator which allows "forking" of a Stream
     * into multiple fanouts.
     */
    private class Copy implements Iterator<int[]> {
        private LinkedList<int[]> list;
        Copy(LinkedList<int[]> l) { this.list = l; }
        public boolean hasNext() { return !list.isEmpty() || mainIterator.hasNext(); }
        public int[] next() {
            if(list.isEmpty()) {
                // We want to make sure only one thread calls next at a time
                criticalAccessLock.lock();
                int[] next = mainIterator.next();
                for(List<int[]> l : fanOuts) { l.add(next); }
                criticalAccessLock.unlock();
            }
            return list.remove(0);
        }
    }
    
    /**
     * Specialized {@link Map} for the avoidance of key hashing. This
     * optimization overrides {@link Map#get(Object)directly accesses the input arrays providing input
     * and should be extremely faster.
     */
    class InputMap extends HashMap<String, Object> {
        private static final long serialVersionUID = 1L;
        
        private FieldMetaType[] fTypes;
        private String[] arr;
        
        @Override public Object get(Object key) {
            int idx = indexFieldMap.get(key);
            return fTypes[idx].decodeType(arr[idx + 1], indexToEncoderMap.get(idx));
        }
        @Override public boolean containsKey(Object key) {
            return indexFieldMap.get(key) != -1;
        }
    }
    
    /**
     * Returns the encoded output stream of the underlying {@link Stream}'s encoder.
     * 
     * @return      the encoded output stream.
     */
    public Stream<int[]> getOutputStream() {
        if(isTerminal()) {
            throw new IllegalStateException("Stream is already \"terminal\" (operated upon or empty)");
        }
        
        final MultiEncoder encoder = (MultiEncoder)getEncoder();
        if(encoder == null) {
            throw new IllegalStateException(
                "setLocalParameters(Parameters) must be called before calling this method.");
        }
        
        // Protect outputStream formation and creation of "fan out" also make sure
        // that no other thread is trying to update the fan out lists
        Stream<int[]> retVal = null;
        try {
            criticalAccessLock.lock();
            
            final String[] fieldNames = getFieldNames();
            final FieldMetaType[] fieldTypes = getFieldTypes();
            
            if(outputStream == null) {
                if(indexFieldMap.isEmpty()) {
                    for(int i = 0;i < fieldNames.length;i++) {
                        indexFieldMap.put(fieldNames[i], i);
                    }
                }
              
                if(inputMap == null) {
                    inputMap = new InputMap();
                    inputMap.fTypes = fieldTypes;
                }
                
                
                final boolean isParallel = delegate.getInputStream().isParallel();
                
                output = new ArrayList<>();
                
                outputStream = delegate.getInputStream().map(l -> {
                    String[] arr = (String[])l;
                    inputMap.arr = arr;
                    return input(arr, fieldNames, fieldTypes, output, isParallel);
                });
                
                mainIterator = outputStream.iterator();
            }
            
            LinkedList<int[]> l = new LinkedList<int[]>();
            fanOuts.add(l);
            Copy copy = new Copy(l);
            
            retVal = StreamSupport.stream(Spliterators.spliteratorUnknownSize(copy,
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), false);
            
        }catch(Exception e) {
            e.printStackTrace();
        }finally {
            criticalAccessLock.unlock();
        }
        
        return retVal;
    }
    
    public boolean hasNext() {
        return mainIterator.hasNext();
    }
    
    /**
     * Returns an array of field names in the order of column head occurrence.
     * 
     * @return
     */
    private String[] getFieldNames() {
        return (String[])header.getFieldNames().toArray(new String[header.getFieldNames().size()]);
    }
    
    /**
     * Returns an array of {@link FieldMetaType}s in the order of field occurrence.
     * @return
     */
    private FieldMetaType[] getFieldTypes() {
        return header.getFieldTypes().toArray(new FieldMetaType[header.getFieldTypes().size()]);
    }
    
    /**
     * <p>
     * Populates the specified outputStreamSource (List&lt;int[]&gt;) with an encoded
     * array of integers - using the specified field names and field types indicated.
     * </p><p>
     * This method process one single record, and is called iteratively to process an
     * input stream (internally by the {@link #getOutputStream()} method which will process
     * </p><p>
     * <b>WARNING:</b>  <em>When inserting data <b><em>MANUALLY</em></b>, you must remember that the first index
     * must be a sequence number, which means you may have to insert that by hand. Typically
     * this method is called internally where the underlying sensor does the sequencing automatically.</em>
     * </p>
     *  
     * @param arr                       The string array of field values           
     * @param fieldNames                The field names
     * @param fieldTypes                The field types
     * @param outputStreamSource        A list object to hold the encoded int[]
     * @param isParallel                Whether the underlying stream is parallel, if so this method
     *                                  executes a binary search for the proper insertion index. The {@link List}
     *                                  handed in should thus be a {@link LinkedList} for faster insertion.
     */
    private int[] input(String[] arr, String[] fieldNames, FieldMetaType[] fieldTypes, List<int[]> outputStreamSource, boolean isParallel) {
        processHeader(arr);
        
        int[] encoding = encoder.encode(inputMap);
        
        if(isParallel) {
            outputStreamSource.set(padTo(Integer.parseInt(arr[0]), outputStreamSource), encoding);
        }
        
        return encoding;
    }
    
    /**
     * Return the input mapping of field names to the last input
     * value for that field name. 
     * 
     * This method is typically used by client code which needs the 
     * input value for use with the {@link CLAClassifier}.
     * @return
     */
    public Map<String, Object> getInputMap() {
        return inputMap;
    }
    
    /**
     * Avoids the {@link IndexOutOfBoundsException} that can happen if inserting
     * into indexes which have gaps between insertion points.
     * 
     * @param i     the index whose lesser values are to have null inserted
     * @param l     the list to operate on.
     * @return      the index passed in (for fluent convenience at call site).
     */
    static int padTo(int i, List<?> l) {
        for(int x = l.size();x < i + 1;x++) {
            l.add(null);
        }
        return i;
    }
    
    /**
     * Searches through the specified {@link MultiEncoder}'s previously configured 
     * encoders to find and return one that is of type {@link CoordinateEncoder} or
     * {@link GeospatialCoordinateEncoder}
     * 
     * @param enc   the containing {@code MultiEncoder}
     * @return
     */
    private Optional<Encoder<?>> getCoordinateEncoder(MultiEncoder enc) {
        for(EncoderTuple t : enc.getEncoders(enc)) {
            if((t.getEncoder() instanceof CoordinateEncoder) ||
                (t.getEncoder() instanceof GeospatialCoordinateEncoder)) {
                return Optional.of(t.getEncoder());
            }
        }
        
        return null;
    }
    
    /**
     * Searches through the specified {@link MultiEncoder}'s previously configured 
     * encoders to find and return one that is of type {@link CategoryEncoder} or
     * {@link SDRCategoryEncoder}
     * 
     * @param enc   the containing {@code MultiEncoder}
     * @return
     */
    private Optional<Encoder<?>> getCategoryEncoder(MultiEncoder enc) {
        for(EncoderTuple t : enc.getEncoders(enc)) {
            if((t.getEncoder() instanceof CategoryEncoder) ||
                (t.getEncoder() instanceof SDRCategoryEncoder)) {
                return Optional.of(t.getEncoder());
            }
        }
        
        return null;
    }
    
    /**
     * Searches through the specified {@link MultiEncoder}'s previously configured 
     * encoders to find and return one that is of type {@link DateEncoder}
     * 
     * @param enc   the containing {@code MultiEncoder}
     * @return
     */
    private Optional<DateEncoder> getDateEncoder(MultiEncoder enc) {
       for(EncoderTuple t : enc.getEncoders(enc)) {
           if(t.getEncoder() instanceof DateEncoder) {
               return Optional.of((DateEncoder)t.getEncoder());
           }
       }
       
       return Optional.of(null);
    }
    
    /**
     * Searches through the specified {@link MultiEncoder}'s previously configured 
     * encoders to find and return one that is of type {@link ScalarEncoder},
     * {@link RandomDistributedScalarEncoder}, {@link AdaptiveScalarEncoder},
     * {@link LogEncoder} or {@link DeltaEncoder}.
     * 
     * @param enc   the containing {@code MultiEncoder}
     * @return
     */
    private Optional<Encoder<?>> getNumberEncoder(MultiEncoder enc) {
        for(EncoderTuple t : enc.getEncoders(enc)) {
            if((t.getEncoder() instanceof RandomDistributedScalarEncoder) ||
                (t.getEncoder() instanceof ScalarEncoder) ||
                (t.getEncoder() instanceof AdaptiveScalarEncoder) ||
                (t.getEncoder() instanceof LogEncoder) ||
                (t.getEncoder() instanceof DeltaEncoder)) {
                
                return Optional.of(t.getEncoder());
            }
        }
        
        return Optional.of(null);
     }
    
    /**
     * <p>
     * Returns a flag indicating whether the underlying stream has had
     * a terminal operation called on it, indicating that it can no longer
     * have operations built up on it.
     * </p><p>
     * The "terminal" flag if true does not indicate that the stream has reached
     * the end of its data, it just means that a terminating operation has been
     * invoked and that it can no longer support intermediate operation creation.
     * 
     * @return  true if terminal, false if not.
     */
    public boolean isTerminal() {
        return delegate.getInputStream().isTerminal();
    }
    
    /**
     * Returns the {@link Header} container for Sensor meta
     * information associated with the input characteristics and configured
     * behavior.
     * @return
     */
    @Override
    public Header getMetaInfo() {
        return header;
    }

    /**
     * Sets the local parameters used to configure the major
     * algorithmic components.
     */
    @SuppressWarnings("unchecked")
    public void initEncoder(Parameters p) {
        this.localParameters = p;
        
        Map<String, Map<String, Object>> encoderSettings;
        if((encoderSettings = (Map<String, Map<String, Object>>)p.getParameterByKey(KEY.FIELD_ENCODING_MAP)) != null &&
            encoder.getEncoders().isEmpty() && 
                indexToEncoderMap == null) {
            
            initEncoders(encoderSettings);
            makeIndexEncoderMap();
        }
    }
    
    /**
     * Returns the global Parameters object
     */
    public Parameters getLocalParameters() {
        return localParameters;
    }
    
    /**
     * For each entry, the header runs its processing to calculate
     * meta state of the current input (i.e. is learning, should reset etc.)
     * 
     * @param entry     an array containing the current input entry.
     */
    private void processHeader(String[] entry) {
        header.process(entry);
    }
    
    /**
     * Called internally to initialize this sensor's encoders
     * @param encoderSettings
     */
    private void initEncoders(Map<String, Map<String, Object>> encoderSettings) {
        if(encoder instanceof MultiEncoder) {
            if(encoderSettings == null || encoderSettings.isEmpty()) {
                throw new IllegalArgumentException(
                    "Cannot initialize this Sensor's MultiEncoder with a null settings");
            }
            
            // Sort the encoders so that they end up in a controlled order
            List<String> sortedFields = new ArrayList<String>(encoderSettings.keySet());
            Collections.sort(sortedFields);

            for (String field : sortedFields) {
                Map<String, Object> params = encoderSettings.get(field);

                if (!params.containsKey("fieldName")) {
                    throw new IllegalArgumentException("Missing fieldname for encoder " + field);
                }
                String fieldName = (String) params.get("fieldName");

                if (!params.containsKey("encoderType")) {
                    throw new IllegalArgumentException("Missing type for encoder " + field);
                }
                
                String encoderType = (String) params.get("encoderType");
                Builder<?, ?> builder = ((MultiEncoder)encoder).getBuilder(encoderType);
                
                if(encoderType.equals("SDRCategoryEncoder")) {
                    ((MultiEncoder)encoder).setValue(builder, "n", params.get("n"));
                    ((MultiEncoder)encoder).setValue(builder, "w", params.get("w"));
                    ((MultiEncoder)encoder).setValue(builder, "forced", params.get("forced"));
                    ((MultiEncoder)encoder).setValue(builder, "categoryList", params.get("categoryList"));
                }else if(encoderType.equals("DateEncoder")) {
                    // Extract date specific mappings out of the map so that we can
                    // pre-configure the DateEncoder with its needed directives.
                    configureDateBuilder(encoderSettings, (DateEncoder.Builder)builder);
                }else{
                    for (String param : params.keySet()) {
                        if (!param.equals("fieldName") && !param.equals("encoderType") &&
                            !param.equals("fieldType") && !param.equals("fieldEncodings")) {
                            
                            ((MultiEncoder)encoder).setValue(builder, param, params.get(param));
                        }
                    }
                }

                ((MultiEncoder)encoder).addEncoder(fieldName, (Encoder<?>)builder.build());
            }
        }
    }
    
    /**
     * Do special configuration for DateEncoder
     * @param encoderSettings
     */
    private void configureDateBuilder(Map<String, Map<String, Object>> encoderSettings, DateEncoder.Builder b) {
        Map<String, Object> dateEncoderSettings = getDateEncoderMap(encoderSettings);
        if(dateEncoderSettings == null) {
            throw new IllegalStateException("Input requires missing DateEncoder settings mapping.");
        }
        
        for(String key : dateEncoderSettings.keySet()) {
            if(!key.equals("fieldName") && !key.equals("encoderType") &&
                !key.equals("fieldType") && !key.equals("fieldEncodings")) {
                
                if(!key.equals("season") && !key.equals("dayOfWeek") &&
                    !key.equals("weekend") && !key.equals("holiday") &&
                    !key.equals("timeOfDay") && !key.equals("customDays") && 
                    !key.equals("formatPattern") && !key.equals("dateFormatter")) {
                
                    ((MultiEncoder)encoder).setValue(b, key, dateEncoderSettings.get(key));
                }else{
                    if(key.equals("formatPattern")) {
                        b.formatPattern((String)dateEncoderSettings.get(key));
                    }else if(key.equals("dateFormatter")) {
                        b.formatter((DateTimeFormatter)dateEncoderSettings.get(key));
                    }else{
                        setDateFieldBits(b, dateEncoderSettings, key);
                    }
                }
            }
        }
    }
    
    /**
     * Extract the date encoder settings out of the main map so that we can do
     * special initialization on any {@link DateEncoder} which may exist.
     * @param encoderSettings
     * @return the {@link DateEncoder} settings map
     */
    private Map<String, Object> getDateEncoderMap(Map<String, Map<String, Object>> encoderSettings) {
        for(String key : encoderSettings.keySet()) {
            String keyType = null;
            if((keyType = (String)encoderSettings.get(key).get("encoderType")) != null && 
                keyType.equals("DateEncoder")) {
                // Remove the key from the specified map (extraction)
                return (Map<String, Object>)encoderSettings.get(key);
            }
        }
        return null;
    }
    
    /**
     * Initializes the {@link DateEncoder.Builder} specified
     * @param b         the builder on which to set the mapping.
     * @param m         the map containing the values
     * @param key       the key to be set.
     */
    @SuppressWarnings("unchecked")
    private void setDateFieldBits(DateEncoder.Builder b, Map<String, Object> m, String key) {
        Tuple t = (Tuple)m.get(key);
        switch(key) {
            case "season" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.season((int)t.get(0), (double)t.get(1));
                }else{
                    b.season((int)t.get(0));
                }
                break;
            }
            case "dayOfWeek" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.dayOfWeek((int)t.get(0), (double)t.get(1));
                }else{
                    b.dayOfWeek((int)t.get(0));
                }
                break;
            }
            case "weekend" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.weekend((int)t.get(0), (double)t.get(1));
                }else{
                    b.weekend((int)t.get(0));
                }
                break;
            }
            case "holiday" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.holiday((int)t.get(0), (double)t.get(1));
                }else{
                    b.holiday((int)t.get(0));
                }
                break;
            }
            case "timeOfDay" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.timeOfDay((int)t.get(0), (double)t.get(1));
                }else{
                    b.timeOfDay((int)t.get(0));
                }
                break;
            }
            case "customDays" : {
                if(t.size() > 1 && ((double)t.get(1)) > 0.0) {
                    b.customDays((int)t.get(0), (List<String>)t.get(1));
                }else{
                    b.customDays((int)t.get(0));
                }
                break;
            }
            
            default: break;
        }
    }
    
    /**
     * Returns this {@code HTMSensor}'s {@link MultiEncoder}
     * @return
     */
    public <K> MultiEncoder getEncoder() {
        return (MultiEncoder)encoder;
    }
    
    public static void main(String[] args) {
        ArrayList<String> ll = new ArrayList<>(5);
        ll.set(padTo(0, ll), "My");
        ll.set(padTo(2, ll), "array");
        ll.set(padTo(1, ll), "ordered");
        
        System.out.println(ll + ", size = " + ll.size());
    }
}
