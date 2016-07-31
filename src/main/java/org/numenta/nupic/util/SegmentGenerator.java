package org.numenta.nupic.util;

import java.util.List;

import org.numenta.nupic.model.DistalDendrite;

/**
 * Used by the {@link TemporalMemory}'s column generator to specify a range
 * of {@link DistalDendrite} segments.
 * 
 * @author cogmission
 * @see AbstractGenerator
 */
public interface SegmentGenerator {
    /**
     * Returns a generator of {@link DistalDendrite}s whose indexes are in the range
     * specified by the {@link AbstractGenerator} argument, and which are contained
     * within the specified segment list.
     * 
     * @param segments  the list of segments from which to retrieve the segments based on the generator's 
     *                  returned index.
     * @param gen       the generator specifying the index of the segments to return.
     * @return  a generator capable of generating a specified range of segments
     */
    public static Generator<DistalDendrite> of(List<DistalDendrite> segments, Generator<Integer> gen) {
        /**
         *  Inner implementation of an {@code AbstractGenerator} for {@code DistalDendrite}s 
         */
        class DDGenerator extends AbstractGenerator<DistalDendrite> {
            private static final long serialVersionUID = 1L;
            
            @Override
            public void exec() {
                while(gen.hasNext()) {
                    yield(segments.get(gen.next()));
                }
            }

            @Override
            public boolean isConsumed() { 
                return !gen.hasNext();
            }
        }

        return new DDGenerator();
    }
}
