package org.numenta.nupic.datagen;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;


public class SequenceMachineTest {

    @Test
    public void testGenerateNumbers() {
        int[] expected = { 4, 6, 2, 1, 7, 20, 21, 22, 23, 24, -1, 14, 16, 12, 11, 17, 20, 21, 22, 23, 24, -1 };
        
        SequenceMachine sm = new SequenceMachine(null);
        List<Integer> result = sm.generateNumbers(2, 10, new Tuple(5, 10));
        
        assertTrue(Arrays.equals(expected, ArrayUtils.toPrimitive(result.toArray(new Integer[0]))));
    }

}
