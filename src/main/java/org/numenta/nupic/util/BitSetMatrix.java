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

import java.util.BitSet;

import org.numenta.nupic.model.Persistable;

/**
 * {@link FlatMatrix} implementation that store booleans in a {@link BitSet}.
 * 
 * @author Jose Luis Martin
 */
public class BitSetMatrix extends AbstractFlatMatrix<Boolean> implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private BitSet data;

    /**
     * @param dimensions
     */
    public BitSetMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    public BitSetMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
        this.data = new BitSet(getSize());
    }

    @Override
    public Boolean get(int index) {
        return this.data.get(index);
    }

    @Override
    public BitSetMatrix set(int index, Boolean value) {
        this.data.set(index, value);
        return this;
    }
}
