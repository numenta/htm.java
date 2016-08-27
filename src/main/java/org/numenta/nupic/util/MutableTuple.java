package org.numenta.nupic.util;


/**
 * Mutable and reusable version of a {@link Tuple}
 * 
 * @author David Ray
 * @see Tuple
 * @see NamedTuple
 */
public class MutableTuple extends Tuple {
    
    private static final long serialVersionUID = 1L;
    
    public MutableTuple(int size) {
        container = new Object[size];
    }

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
        container = new Object[0];
    }
}
