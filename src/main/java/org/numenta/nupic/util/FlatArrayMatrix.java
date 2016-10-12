/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
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

import java.util.Arrays;

import org.numenta.nupic.model.Persistable;

/**
 * {@link FlatMatrix} implementation that store objects in a flat object array.
 * 
 * @author Jose Luis Martin
 */
public class FlatArrayMatrix<T> extends AbstractFlatMatrix<T> implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private T[] data;

    public FlatArrayMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    @SuppressWarnings("unchecked")
    public FlatArrayMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.data = (T[]) new Object[getSize()];
    }

    @Override
    public T get(int index) {
        return this.data[index];
    }

    @Override
    public FlatArrayMatrix<T> set(int index, T value) {
        this.data[index] = value;
        return this;
    }

    /**
     * Fill array with value
     * @param value
     */
    public void fill(T value) {
        Arrays.fill(this.data, value);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(data);
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!super.equals(obj))
            return false;
        if(getClass() != obj.getClass())
            return false;
        FlatArrayMatrix other = (FlatArrayMatrix)obj;
        if(!Arrays.equals(data, other.data))
            return false;
        return true;
    }

}
