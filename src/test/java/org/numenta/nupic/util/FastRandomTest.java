package org.numenta.nupic.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import gnu.trove.set.hash.TIntHashSet;

import java.util.Random;

import org.junit.Test;


public class FastRandomTest {

    @Test
    public void testSeed() {
        assertEquals(42, new FastRandom(42).getSeed());
    }
    
    @Test
    public void testUniquenessAndDeterminance() {
        Random r = new FastRandom(42);
        TIntHashSet set = new TIntHashSet();
        
        TIntHashSet knownExpectedRepeats = new TIntHashSet();
        knownExpectedRepeats.addAll(new int[] { 9368, 149368, 193310, 194072, 202906, 241908, 249466, 266101, 276853, 289339, 293737 } );
        
        for(int i = 0;i < 300000;i++) {
            int rndInt = r.nextInt();
            if(set.contains(rndInt) && !knownExpectedRepeats.contains(i)) {
                fail();
            }
            set.add(rndInt);
        }
    }

}
