package org.numenta.nupic.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;

public interface Generator<T> extends Iterator<T>, Iterable<T>, Serializable {
    /**
     * Returns the value returned by the last call to {@link #next()}
     * or the initial value if no previous call to {@code #next()} was made.
     * @return
     */
    default int get() { return -1; }
    
    /**
     * Returns the configured size or distance between the initialized
     * upper and lower bounds.
     * @return
     */
    default int size() { return -1; }
    
    /**
     * Returns the state of this generator to its initial state so 
     * that it can be reused.
     */
    default void reset() {}

    /**
     * Returns a flag indicating whether another iteration
     * of processing may occur.
     * 
     * @return  true if so, false if not
     */
    boolean hasNext();

    /**
     * Returns the object of type &lt;T&gt; which is the
     * result of one iteration of processing.
     * 
     * @return   the object of type &lt;T&gt; to return
     */
    T next();
    
    /**
     * {@inheritDoc}
     */
    default Iterator<T> iterator() { return this; }
    
    /**
     * Returns a {@code Generator} of type &lt;T&gt; which can be used
     * as both an {@link Iterator} and an {@link Iterable}
     * 
     * @param l     the list of items to generate
     * @param i     the generator of indexes to return
     * @return      the composed generator
     */
    static <T> Generator<T> of(List<T> l, Generator<Integer> i) {
        /**
         *  Inner implementation of an {@code Generator}
         */
        return new Generator<T>() {
            private static final long serialVersionUID = 1L;
            
            @Override
            public T next() {
                return l.get(i.next());
            }

            @Override
            public boolean hasNext() { 
                return i.hasNext();
            }
        };
    }
}
