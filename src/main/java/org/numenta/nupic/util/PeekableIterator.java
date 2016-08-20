/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014-2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.util;

import java.util.Iterator;

/**
 * Implementors add the ability to return the "next" item which 
 * would be returned by a call to {@link Iterator#next()} - without
 * altering the iterator's pointer to the indexed item.
 * 
 * @author cogmission
 *
 * @param <T>   the type of the return value.
 */
public interface PeekableIterator<T> extends Iterator<T> {
    /**
     * Returns the item that would be returned by {@link #next()},
     * without forwarding the iterator index.
     * 
     * @return  the "next" item to be returned by a call to {@link Iterator#next()}
     */
    public T peek();
}
