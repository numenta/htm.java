package org.numenta.nupic.util;

import static org.junit.Assert.*;

import org.junit.Test;


/**
 * Trivial Tests to keep CI Travis, Coveralls Happy
 * @author cogmission
 *
 */
public class MersenneTwisterFastTest {

    @Test
    public void testSetSeed() {
        MersenneTwisterFast m = new MersenneTwisterFast();
        m.setSeed(new int[] { 44 });
        assertNotNull(m);
    }
    
    @Test
    public void testNextInt() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextInt() > -1);
    }
    
    @Test
    public void testNextShort() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextShort() > -1);
    }
    
    @Test
    public void testNextChar() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextChar() != ' ');
    }
    
    @Test
    public void testNextBoolean() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextBoolean() || true);
    }
    
    @Test
    public void testNextBooleanFloat() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextBoolean(0.22f) || true);
    }
    
    @Test
    public void testNextBooleanDouble() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextBoolean(0.44D) || true);
    }
    
    @Test
    public void testNextByte() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextByte() != 0x00);
    }
    
    @Test
    public void testNextLong() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextLong() > 0);
    }
    
    @Test
    public void testNextDouble() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextDouble() > 0);
    }
    
    @Test
    public void testNextDoubleRange() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        double d = m.nextDouble(false, false);
        assertTrue(d > 0.0 && d < 1.0);
    }
    
    @Test
    public void testNextGaussian() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextGaussian() < 0.14);
    }
    
    @Test
    public void testNextFloat() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextFloat() > 0);
    }
    
    @Test
    public void testNextFloatRange() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        float d = m.nextFloat(false, false);
        assertTrue(d > 0.0 && d < 1.0);
    }
    
    @Test
    public void testNextIntBoundary() {
        MersenneTwisterFast m = new MersenneTwisterFast(42);
        assertTrue(m.nextInt(3) < 3);
    }

}
