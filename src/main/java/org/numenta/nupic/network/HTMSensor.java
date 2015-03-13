package org.numenta.nupic.network;

import java.util.List;
import java.util.Map;

import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.ValueList;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;
import org.numenta.nupic.util.Tuple;


/**
 * Decorator for {@link Sensor} types adding HTM
 * specific functionality to sensors and streams.
 * 
 * @author metaware
 *
 * @param <T>
 */
public class HTMSensor<T> implements Sensor<T> {
    private Sensor<T> delegate;
    private SensorInputMeta meta;
    private Parameters localParameters;
    private Encoder<?> encoder;
    
    public HTMSensor(Sensor<T> sensor) {
        this.delegate = sensor;
        meta = new SensorInputMeta(sensor.getStream().getMeta());
        createEncoder();
    }
    
    /**
     * Called internally during construction to build the encoders
     * needed to process the configured field types.
     */
    @SuppressWarnings("unchecked")
    private void createEncoder() {
        if(meta.getFieldTypes().size() > 1) {
            encoder = MultiEncoder.builder().name("MultiEncoder").build();
        }else{
            encoder = meta.getFieldTypes().get(0).newEncoder();
        }
        
        Map<String, Map<String, Object>> encoderSettings;
        if(localParameters != null && 
            (encoderSettings = (Map<String, Map<String, Object>>)localParameters.getParameterByKey(KEY.FIELD_ENCODING_MAP)) != null &&
                !encoderSettings.isEmpty()) {
            
            initEncoders(encoderSettings);
        }
    }

    @Override
    public SensorParams getParams() {
        return delegate.getParams();
    }

    @Override
    public <K> MetaStream<K> getStream() {
        return delegate.getStream();
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
            
            DateEncoder.Builder dateBuilder = doDateEncoderConfig(encoderSettings);
            if(dateBuilder != null) {
                ((MultiEncoder)encoder).addEncoder("DateEncoder", dateBuilder.build());
            }
            ((MultiEncoder)encoder).addMultipleEncoders(encoderSettings);
        }
    }
    
    /**
     * Do special configuration for DateEncoder
     * @param encoderSettings
     */
    private DateEncoder.Builder doDateEncoderConfig(Map<String, Map<String, Object>> encoderSettings) {
        DateEncoder.Builder retVal = null;
        Map<String, Object> dateEncoderSettings = extractAndConfigureDateTimeSettings(encoderSettings);
        if(dateEncoderSettings != null) {
            DateEncoder.Builder b = DateEncoder.builder();
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
            
            retVal = b;
        }
        
        return retVal;
    }
    
    /**
     * Extract the date encoder settings out of the main map so that we can do
     * special initialization on any {@link DateEncoder} which may exist.
     * @param encoderSettings
     * @return
     */
    private Map<String, Object> extractAndConfigureDateTimeSettings(Map<String, Map<String, Object>> encoderSettings) {
        for(String key : encoderSettings.keySet()) {
            String keyType = null;
            if((keyType = (String)encoderSettings.get(key).get("encoderType")) != null && 
                keyType.equals("DateEncoder")) {
                // Remove the key from the specified map (extraction)
                return (Map<String, Object>)encoderSettings.remove(key);
            }
        }
        return null;
    }
    
    /**
     * Initializes the {@link DateEncoder.Builder} specified
     * @param b
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
