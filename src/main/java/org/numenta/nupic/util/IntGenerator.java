package org.numenta.nupic.util;

/**
 * Generates a range of integers while exhibiting the specific behaviors
 * of a Python generator.
 * 
 * @author cogmission
 * @see AbstractGenerator
 */
public interface IntGenerator {
    /**
     * Returns an {@link AbstractGenerator} capable of returning a range of integers
     * specified by the lower and upper bound arguments.
     * 
     * @param lower     the lower bound <b><em>(inclusive)</em></b>
     * @param upper     the upper bound <b><em>(exclusive)</em></b>
     * @return  an {@link AbstractGenerator} capable of returning a range of integers
     */
    public static Generator<Integer> of(int lower, int upper) {
        /**
         *  Inner implementation of an {@code AbstractGenerator} for {@code Integer}s 
         */
        class TerminableGenerator extends AbstractGenerator<Integer> {
            private static final long serialVersionUID = 1L;
            
            int i = lower;
            
            @Override
            public void exec() {
                while(i < upper) {
                    yield(Integer.valueOf(i++));
                }
            }

            @Override
            public boolean isConsumed() { 
                return i > upper - 1;
            }
        }

        return new TerminableGenerator();
    }
}
