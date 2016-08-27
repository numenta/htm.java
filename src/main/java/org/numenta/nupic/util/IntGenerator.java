package org.numenta.nupic.util;

/**
 * Generates a range of integers.
 * 
 * @author cogmission
 */
public class IntGenerator {
    /**
     * Returns a {@link Generator} which returns integers between
     * the values specified (lower inclusive, upper exclusive)
     * @param lower     the lower bounds or start value
     * @param upper     the upper bounds (exclusive)
     * @return
     */
    public static Generator<Integer> of(int lower, int upper) {
        return new Generator<Integer>() {
            /** serial version */
            private static final long serialVersionUID = 1L;
            
            int i = lower;
            
            /**
             * {@inheritDoc}
             */
            @Override
            public Integer next() {
                return i++;
            }
            
            /**
             * {@inheritDoc}
             */
            @Override
            public boolean hasNext() { return i < upper; }
        };
    }
}
