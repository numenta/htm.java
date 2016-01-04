package org.numenta.nupic.util;

import java.io.Serializable;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * {@link SparseMatrix} implementation that use a {@link Set} to store indexes.
 * 
 * @author Jose Luis Martin
 */
public class SetSparseMatrix extends AbstractSparseMatrix<Integer> implements Serializable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private SortedSet<Integer> indexes = new TreeSet<>();

    public SetSparseMatrix(int[] dimensions) {
        this(dimensions, false);
    }

    public SetSparseMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
        super(dimensions, useColumnMajorOrdering);
    }

    @Override
    public SetSparseMatrix set(int[] coordinates, Integer value) {
        return set(computeIndex(coordinates), value);

    }

    @Override
    public Integer get(int index) {
        return this.indexes.contains(index) ? 0 : 1;
    }

    @Override
    public SetSparseMatrix set(int index, Integer value) {
        if (value > 0) 
            this.indexes.add(index);

        return this;
    }	
}
