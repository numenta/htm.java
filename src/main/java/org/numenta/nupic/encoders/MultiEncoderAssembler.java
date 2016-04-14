package org.numenta.nupic.encoders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.encoders.Encoder.Builder;
import org.numenta.nupic.network.sensor.HTMSensor;
import org.numenta.nupic.util.Tuple;

/**
 * Provides a central configuration path for {@link MultiEncoder}s, for use
 * both by the MultiEncoder itself, and the Network configuration performed
 * by the {@link HTMSensor}
 * 
 * @author cogmission
 *
 */
public class MultiEncoderAssembler {
    
    /**
     * Uses the specified Map containing encoder settings to configure the
     * {@link MultiEncoder} passed in.
     * 
     * @param encoder           the {@link MultiEncoder} to configure.
     * @param encoderSettings   the Map containing MultiEncoder settings.
     */
    public static MultiEncoder assemble(MultiEncoder encoder, Map<String, Map<String, Object>> encoderSettings) {
        if(encoderSettings == null || encoderSettings.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot initialize this Sensor's MultiEncoder with a null or empty settings");
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
                // Add mappings for category list
                configureCategoryBuilder((MultiEncoder)encoder, params, builder);
            }else if(encoderType.equals("DateEncoder")) {
                // Extract date specific mappings out of the map so that we can
                // pre-configure the DateEncoder with its needed directives.
                configureDateBuilder(encoder, encoderSettings, (DateEncoder.Builder)builder);
            }else if(encoderType.equals("GeospatialCoordinateEncoder")) {
                // Extract Geo specific mappings out of the map so that we can
                // pre-configure the GeospatialCoordinateEncoder with its needed directives.
                configureGeoBuilder(encoder, encoderSettings, (GeospatialCoordinateEncoder.Builder) builder);
            }else{
                for (String param : params.keySet()) {
                    if (!param.equals("fieldName") && !param.equals("encoderType") &&
                        !param.equals("fieldType") && !param.equals("fieldEncodings")) {
                        
                        ((MultiEncoder)encoder).setValue(builder, param, params.get(param));
                    }
                }
            }

            encoder.addEncoder(fieldName, (Encoder<?>)builder.build());
        }
        
        return encoder;
    }
    
    private static void configureCategoryBuilder(MultiEncoder multiEncoder, 
        Map<String, Object> encoderSettings, Builder<?,?> builder) {
        
        multiEncoder.setValue(builder, "n", encoderSettings.get("n"));
        multiEncoder.setValue(builder, "w", encoderSettings.get("w"));
        multiEncoder.setValue(builder, "forced", encoderSettings.get("forced"));
        multiEncoder.setValue(builder, "categoryList", encoderSettings.get("categoryList"));
    }
    
    /**
     * Do special configuration for DateEncoder
     * @param encoderSettings
     */
    private static void configureDateBuilder(MultiEncoder multiEncoder, Map<String, Map<String, Object>> encoderSettings, DateEncoder.Builder b) {
        Map<String, Object> dateEncoderSettings = getEncoderMap(encoderSettings, "DateEncoder");
        if(dateEncoderSettings == null) {
            throw new IllegalStateException("Input requires missing DateEncoder settings mapping.");
        }
        
        for(String key : dateEncoderSettings.keySet()) {
            if(!key.equals("fieldName") && !key.equals("encoderType") &&
                !key.equals("fieldType") && !key.equals("fieldEncodings")) {
                
                if(!key.equals("season") && !key.equals("dayOfWeek") &&
                    !key.equals("weekend") && !key.equals("holiday") &&
                    !key.equals("timeOfDay") && !key.equals("customDays") &&
                    !key.equals("formatPattern")) {
                
                    multiEncoder.setValue(b, key, dateEncoderSettings.get(key));
                }else{
                    if(key.equals("formatPattern")) {
                        b.formatPattern((String)dateEncoderSettings.get(key));
                    }else{
                        setDateFieldBits(b, dateEncoderSettings, key);
                    }
                }
            }
        }
    }
    
    /**
     * Initializes the {@link DateEncoder.Builder} specified
     * @param b         the builder on which to set the mapping.
     * @param m         the map containing the values
     * @param key       the key to be set.
     */
    @SuppressWarnings("unchecked")
    private static void setDateFieldBits(DateEncoder.Builder b, Map<String, Object> m, String key) {
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
     * Specific configuration for GeospatialCoordinateEncoder builder
     * @param encoderSettings
     * @param builder
     */
    private static void configureGeoBuilder(MultiEncoder multiEncoder, Map<String, Map<String, Object>> encoderSettings, GeospatialCoordinateEncoder.Builder builder) {
        Map<String, Object> geoEncoderSettings = getEncoderMap(encoderSettings, "GeospatialCoordinateEncoder");
        if(geoEncoderSettings == null) {
            throw new IllegalStateException("Input requires missing GeospatialCoordinateEncoder settings mapping.");
        }

        for(String key : geoEncoderSettings.keySet()) {
            if(!key.equals("fieldName") && !key.equals("encoderType") &&
                    !key.equals("fieldType") && !key.equals("fieldEncodings")) {

                if(!key.equals("scale") && !key.equals("timestep")) {
                    multiEncoder.setValue(builder, key, geoEncoderSettings.get(key));
                } else {
                    setGeoFieldBits(builder, geoEncoderSettings, key);
                }
            }
        }
    }

    /**
     * Initializes the {@link GeospatialCoordinateEncoder.Builder} specified
     * @param b         the builder on which to set the mapping.
     * @param m         the map containing the values
     * @param key       the key to be set.
     */
    private static void setGeoFieldBits(GeospatialCoordinateEncoder.Builder b, Map<String, Object> m, String key) {
        Object obj = m.get(key);
        if(obj instanceof String) {
            String t = (String)m.get(key);
            switch(key) {
                case "scale" : {
                    b.scale(Integer.parseInt(t));
                    break;
                }
                case "timestep" : {
                    b.timestep(Integer.parseInt(t));
                    break;
                }
                default: break;
            }
        }else{
            int t = (int)obj;
            switch(key) {
                case "scale" : {
                    b.scale(t);
                    break;
                }
                case "timestep" : {
                    b.timestep(t);
                    break;
                }
                default: break;
            }
        }
    }
    
    /**
     * Extract the encoder settings out of the main map so that we can do
     * special initialization on it
     * @param encoderSettings
     * @return the settings map
     */
    private static Map<String, Object> getEncoderMap(Map<String, Map<String, Object>> encoderSettings, String encoderType) {
        for(String key : encoderSettings.keySet()) {
            String keyType = null;
            if((keyType = (String)encoderSettings.get(key).get("encoderType")) != null &&
                    keyType.equals(encoderType)) {
                // Remove the key from the specified map (extraction)
                return (Map<String, Object>)encoderSettings.get(key);
            }
        }
        return null;
    }
}
