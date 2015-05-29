package org.numenta.nupic.network.sensor;

import java.util.stream.Stream;

import org.numenta.nupic.ValueList;

/**
 * Adds meta information retrieval to a {@link Stream}
 * 
 * @author metaware
 *
 * @param <T>   the source type of the {@link Stream}
 */
public interface MetaStream<T> extends Stream<T> {
    /**
     * Returns a {@link ValueList} containing meta information (i.e. header information)
     * which can be used to infer the structure of the underlying stream.
     * 
     * @return  a {@link ValueList} describing meta features of this stream.
     */
    public ValueList getMeta();
    
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
    public boolean isTerminal();
}
