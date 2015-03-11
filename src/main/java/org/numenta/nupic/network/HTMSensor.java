package org.numenta.nupic.network;

import java.util.Map;

import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.ValueList;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.MultiEncoder;


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
            
            ((MultiEncoder)encoder).addMultipleEncoders(encoderSettings);
        }
    }
    
    public Encoder<?> getEncoder() {
        return encoder;
    }
    
}
