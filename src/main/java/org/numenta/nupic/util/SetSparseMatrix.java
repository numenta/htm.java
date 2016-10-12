package org.numenta.nupic.util;

import java.util.Set;
import java.util.TreeSet;

import org.numenta.nupic.model.Persistable;

/**
 * {@link SparseMatrix} implementation that use a {@link Set} to store indexes.
 * 
 * @author Jose Luis Martin
 */
public class SetSparseMatrix extends AbstractSparseMatrix<Integer> implements Persistable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private TreeSet<Integer> indexes = new TreeSet<>();

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
        return this.indexes.contains(index) ? 1 : 0;
    }

    @Override
    public SetSparseMatrix set(int index, Integer value) {
        if (value > 0) 
            this.indexes.add(index);

        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((indexes == null) ? 0 : indexes.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(!super.equals(obj))
            return false;
        if(getClass() != obj.getClass())
            return false;
        SetSparseMatrix other = (SetSparseMatrix)obj;
        if(indexes == null) {
            if(other.indexes != null)
                return false;
        } else if(!indexes.equals(other.indexes))
            return false;
        return true;
    }	
}
