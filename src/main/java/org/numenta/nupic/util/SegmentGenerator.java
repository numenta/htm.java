package org.numenta.nupic.util;

import java.util.List;

import org.numenta.nupic.algorithms.OldTemporalMemory;
import org.numenta.nupic.model.DistalDendrite;

/**
 * Used by the {@link OldTemporalMemory}'s column generator to specify a range
 * of {@link DistalDendrite} segments.
 * 
 * @author cogmission
 */
public interface SegmentGenerator {
    /**
     * Returns a generator of {@link DistalDendrite}s whose indexes are in the range
     * specified by the {@link AbstractStuff} argument, and which are contained
     * within the specified segment list.
     * 
     * @param segments  the list of segments from which to retrieve the segments based on the generator's 
     *                  returned index.
     * @param gen       the generator specifying the index of the segments to return.
     * @return  a generator capable of generating a specified range of segments
     */
    public static Generator<DistalDendrite> of(List<DistalDendrite> segments, Generator<Integer> gen) {
        return Generator.of(segments, gen);
    }
}
