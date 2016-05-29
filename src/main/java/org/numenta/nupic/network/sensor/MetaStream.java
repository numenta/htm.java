/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.network.sensor;

import java.util.stream.Stream;

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
