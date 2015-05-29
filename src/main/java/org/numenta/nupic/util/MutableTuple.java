package org.numenta.nupic.util;


/**
 * Mutable and reusable version of a {@link Tuple}
 * 
 * @author David Ray
 * @see Tuple
 * @see NamedTuple
 */
public class MutableTuple extends Tuple {
    /**
     * Constructs a new {@code MutableTuple} with the contents
     * specified. Warning, all Tuples cannot be resized.
     * 
     * @param objects
     */
    public MutableTuple(Object... objects) {
        super(objects);
    }
    
    /**
     * Convenience constructor to initialize the size of this {@code MutableTuple}
     * given that it cannot be resized.
     * 
     * @param maxFields
     */
    public MutableTuple(int maxFields) {
        this(new Object[maxFields]);
    }
    
    /**
     * Sets the value at the specified index to be the
     * indicated object.
     * 
     * @param index     the index at which to set the specified object
     * @param o         the new object to store
     */
    public void set(int index, Object o) {
        container[index] = o;
    }
    
    /**
     * Clears the contained data from this {@code MutableTuple}
     */
    public void clear() {
        for(int i = 0;i < container.length;i++) container[i] = null;
    }
}
