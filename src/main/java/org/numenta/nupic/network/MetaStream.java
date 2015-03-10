package org.numenta.nupic.network;

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
    public ValueList getMeta();
}
