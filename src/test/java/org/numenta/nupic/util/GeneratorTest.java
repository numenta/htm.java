package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class GeneratorTest {
    /////////////////////////////////
    //       Utility Methods       //
    /////////////////////////////////
    /**
     * Returns an {@link AbstractGenerator} that runs for 30 iterations
     * 
     * @return  an {@link AbstractGenerator} that runs for 30 iterations
     */
    private Generator<Integer> getGenerator() {
         return new Generator<Integer>() {
             /** serial version */
            private static final long serialVersionUID = 1L;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Integer next() {
                return new Integer(42);
            }
             
         };
    }
    
    @Test
    public void testInterface() {
        Generator<Integer> bg = getGenerator();
        assertEquals(bg, bg.iterator());
        assertTrue(bg.hasNext());
        assertEquals((Integer)42, (Integer)bg.next());
        
        // Test other interface methods
        assertEquals(-1, bg.get());
        assertEquals(-1, bg.size());
        try {
            bg.reset();
        }catch(Exception e) { fail(); }
    }
    
    @Test
    public void testOf() {
        List<Integer> l = new ArrayList<>();
        l.add(42);
        Generator<Integer> g = Generator.of(l, IntGenerator.of(0, 1));
        assertEquals(42, (int)g.next());
        assertFalse(g.hasNext());
    }
}
