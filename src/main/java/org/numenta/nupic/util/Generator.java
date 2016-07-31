/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.util;

import java.io.Serializable;
import java.util.Iterator;

public interface Generator<T> extends Iterable<T>, Iterator<T>, Serializable {
    /**
     * Called during the execution of the {@link #exec()}
     * method to signal the availability of the result from
     * one iteration of processing.
     *  
     * @param t     the object of type &lt;T&gt; to return
     */
    default void yield(T t) {}
    
    /**
     * Halts the main thread
     */
    default void halt() {}
    
    /**
     * Used by the main {@link #exec} loop to query if
     * the execution is to be stopped.
     * @return      true if halt requested, false if not
     */
    default boolean haltRequested() { return false; }
    
    /**
     * Overridden to identify when this generator has
     * concluded its processing. For infinite generators
     * simply return "false" here.
     * 
     * @return  a flag indicating whether the last iteration
     *          was the last processing cycle.  
     */
    boolean isConsumed();

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
    Iterator<T> iterator();

}