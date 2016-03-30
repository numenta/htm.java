package org.numenta.nupic;

import java.io.Serializable;

/**
 * Extends {@link Serializable} to add preparation tasks prior to
 * serialization and repair tasks following deserialization.
 * 
 * @author cogmission
 */
public interface Persistable extends Serializable {
    
    /**
     * Called prior to this object being serialized. Any
     * preparation required for serialization should be done
     * in this method.
     */
    @SuppressWarnings("unchecked")
    public default <T> T preSerialize() { return (T)this; }
    /**
     * Called following deserialization to execute logic required
     * to "fix up" any inconsistencies within the object being
     * reified.
     */
    @SuppressWarnings("unchecked")
    public default <T> T postSerialize() { return (T)this; }

}
