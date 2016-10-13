package org.numenta.nupic.model;

import java.io.Serializable;

/**
 * Extends {@link Serializable} to add preparation tasks prior to
 * serialization and repair tasks following deserialization.
 * 
 * @author cogmission
 */
public interface Persistable extends Serializable {
    
    /**
     * <em>FOR INTERNAL USE ONLY</em><p>
     * Called prior to this object being serialized. Any
     * preparation required for serialization should be done
     * in this method.
     */
    @SuppressWarnings("unchecked")
    public default <T> T preSerialize() { return (T)this; }
    /**
     * <em>FOR INTERNAL USE ONLY</em><p>
     * Called following deserialization to execute logic required
     * to "fix up" any inconsistencies within the object being
     * reified.
     */
    @SuppressWarnings("unchecked")
    public default <T> T postDeSerialize() { return postDeSerialize((T)this); }
    /**
     * <em>FOR INTERNAL USE ONLY</em><p>
     * Called to implement a full or partial copy of an object 
     * upon de-serialization.
     * 
     * @param t     the instance of type &lt;T&gt;
     * @return  a post serialized custom form of T
     */
    public default <T> T postDeSerialize(T t) { return t; }
    
}
