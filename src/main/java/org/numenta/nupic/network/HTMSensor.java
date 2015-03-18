package org.numenta.nupic.network;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.ValueList;
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
 * 
 * @author metaware
 *
 * @param <T>
 */
public class HTMSensor<T> implements Sensor<T> {
    private Sensor<T> delegate;
    private SensorInputMeta meta;
    private Parameters localParameters;
    private MultiEncoder encoder;
    private Stream<int[]> outputStream;
    private List<int[]> output;
    private Map<String, Object> inputMap;
    
    private TIntObjectMap<Encoder<?>> indexToEncoderMap;
    
    
    public HTMSensor(Sensor<T> sensor) {
        this.delegate = sensor;
        meta = new SensorInputMeta(sensor.getInputStream().getMeta());
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
    
    private void makeIndexEncoderMap() {
        indexToEncoderMap = new TIntObjectHashMap<Encoder<?>>();
        
        final FieldMetaType[] fieldTypes = meta.getFieldTypes().toArray(new FieldMetaType[meta.getFieldTypes().size()]);
        
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

    @Override
    public SensorParams getParams() {
        return delegate.getParams();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <K> MetaStream<K> getInputStream() {
        return (MetaStream<K>)delegate.getInputStream();
    }
    
    /**
     * Returns the encoded output stream of the underlying {@link Stream}'s encoder.
     * 
     * @return      the encoded output stream.
     */
    public Stream<int[]> getOutputStream() {
        final MultiEncoder encoder = (MultiEncoder)getEncoder();
        if(encoder == null) {
            throw new IllegalStateException(
                "setLocalParameters(Parameters) must be called before calling this method.");
        }
        
        if(outputStream == null) {
            inputMap = new HashMap<>();
            final String[] fieldNames = getFieldNames();
            
            final FieldMetaType[] fieldTypes = getFieldTypes();
            
            final boolean isParallel = delegate.getInputStream().isParallel();
            
            output = isParallel ? new LinkedList<>() : new ArrayList<>(); // if parallel we must sort and LinkedList has fastest insertion
            
            getInputStream().forEach(l -> {
                
                String[] arr = (String[])l;
                input(arr, fieldNames, fieldTypes, output, isParallel);
            });
        }
        
        return outputStream = output.stream();
    }
    
    public String[] getFieldNames() {
        return (String[])meta.getFieldNames().toArray(new String[meta.getFieldNames().size()]);
    }
    
    public FieldMetaType[] getFieldTypes() {
        return meta.getFieldTypes().toArray(new FieldMetaType[meta.getFieldTypes().size()]);
    }
    
    /**
     * <p>
     * Populates the specified outputStreamSource (List&lt;int[]&gt;) with an encoded
     * array of integers - using the specified field names and field types indicated.
     * </p><p>
     * This method process one single record, and is called iteratively to process an
     * input stream (internally by the {@link #getOutputStream()} method which will process
     * </p><p>
     * <b>WARNING:<b>  <em>When inserting data one by one, you must remember that the first index
     * must be a sequence number, which means you may have to insert that by hand. Typically
     * this method is called internally where the underlying sensor does the sequencing automatically.</em>
     *  
     * @param arr                       The string array of field values           
     * @param fieldNames                The field names
     * @param fieldTypes                The field types
     * @param outputStreamSource        A list object to hold the encoded int[]
     * @param isParallel                Whether the underlying stream is parallel, if so this method
     *                                  executes a binary search for the proper insertion index. The {@link List}
     *                                  handed in should thus be a {@link LinkedList} for faster insertion.
     */
    public void input(String[] arr, String[] fieldNames, FieldMetaType[] fieldTypes, List<int[]> outputStreamSource, boolean isParallel) {
        if(inputMap == null) inputMap = new HashMap<>();
        
        for(int i = 0;i < fieldNames.length;i++) {
            inputMap.put(fieldNames[i], fieldTypes[i].decodeType(arr[i + 1], indexToEncoderMap.get(i)));
        }
        
        int[] encoding = encoder.encode(inputMap);
        
        // If using parallel batch streaming, we must reassemble inputs
        // in the correct order so use binary search for insertion.
        if(isParallel) {
            int index = Collections.binarySearch( outputStreamSource, encoding, 
                (int[] i,int[] j) -> i[0] < j[0] ? -1 : i[0] == j[0] ? 0 : 1);
            
            if (index < 0) index = ~index;
            
            outputStreamSource.add(index, encoding);
        }else{
            outputStreamSource.add(encoding);
        }
    }
    
    private Optional<Encoder<?>> getCoordinateEncoder(MultiEncoder enc) {
        for(EncoderTuple t : enc.getEncoders(enc)) {
            if((t.getEncoder() instanceof CoordinateEncoder) ||
                (t.getEncoder() instanceof GeospatialCoordinateEncoder)) {
                return Optional.of(t.getEncoder());
            }
        }
        
        return null;
    }
    
    private Optional<Encoder<?>> getCategoryEncoder(MultiEncoder enc) {
        for(EncoderTuple t : enc.getEncoders(enc)) {
            if((t.getEncoder() instanceof CategoryEncoder) ||
                (t.getEncoder() instanceof SDRCategoryEncoder)) {
                return Optional.of(t.getEncoder());
            }
        }
        
        return null;
    }
    
    private Optional<DateEncoder> getDateEncoder(MultiEncoder enc) {
       for(EncoderTuple t : enc.getEncoders(enc)) {
           if(t.getEncoder() instanceof DateEncoder) {
               return Optional.of((DateEncoder)t.getEncoder());
           }
       }
       
       return Optional.of(null);
    }
    
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
     * Returns the {@link SensorInputMeta} container for Sensor meta
     * information associated with the input characteristics and configured
     * behavior.
     * @return
     */
    @Override
    public ValueList getMeta() {
        return meta;
    }

    /**
     * Sets the global parameters used to configure the major
     * algorithmic components.
     */
    @SuppressWarnings("unchecked")
    public void setLocalParameters(Parameters p) {
        this.localParameters = p;
        
        Map<String, Map<String, Object>> encoderSettings;
        if((encoderSettings = (Map<String, Map<String, Object>>)p.getParameterByKey(KEY.FIELD_ENCODING_MAP)) != null) {
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
     * Called internally to initialize this sensor's encoders
     * @param encoderSettings
     */
    private void initEncoders(Map<String, Map<String, Object>> encoderSettings) {
        if(encoder instanceof MultiEncoder) {
            if(encoderSettings == null) {
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
                
                if(encoderType.equals("DateEncoder")) {
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
    
    @SuppressWarnings("unchecked")
    public <K> Encoder<K> getEncoder() {
        return (Encoder<K>)encoder;
    }
    
}
