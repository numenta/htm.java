package org.numenta.nupic.util;

import org.numenta.nupic.model.Persistable;


/**
 * Implementation of George Marsaglia's elegant Xorshift random generator
 * 30% faster and better quality than the built-in java.util.random see also
 * see http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
 */
public strictfp class FastRandom extends java.util.Random implements Persistable, Cloneable {
    private static final long serialVersionUID = 1L;
    
    protected long seed;

    /**
     * Creates a new pseudo random number generator. The seed is initialized to
     * the current time, as if by
     * <code>setSeed(System.currentTimeMillis());</code>.
     */
    public FastRandom() {
        this(System.nanoTime());
    }

    /**
     * Creates a new pseudo random number generator, starting with the specified
     * seed, using <code>setSeed(seed);</code>.
     *
     * @param seed
     *            the initial seed
     */
    public FastRandom(long seed) {
        this.seed = seed;
    }
    
    /**
     * Returns the current state of the seed, can be used to clone the object
     *
     * @returns the current seed
     */
    public synchronized long getSeed() {
        return seed;
    }

    /**
     * Sets the seed for this pseudo random number generator. As described
     * above, two instances of the same random class, starting with the same
     * seed, produce the same results, if the same methods are called.
     *
     * @param s
     *            the new seed
     */
    public synchronized void setSeed(long seed) {
        this.seed = seed;
        super.setSeed(seed);
    }

    /**
     * Returns an XSRandom object with the same state as the original
     */
    public FastRandom clone() {
        return new FastRandom(getSeed());
    }

    /**
     * Implementation of George Marsaglia's elegant Xorshift random generator
     * 30% faster and better quality than the built-in java.util.random see also
     * see http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
     */
    protected int next(int nbits) {
        long x = seed;
        x ^= (x << 21);
        x ^= (x >>> 35);
        x ^= (x << 4);
        seed = x;
        x &= ((1L << nbits) - 1);
        
        return (int) x;
    }
    
    /**
     * Sets the specified seed value from the specified int[]
     * @param array
     */
    synchronized public void setSeed(int[] array) {
        if (array.length == 0)
            throw new IllegalArgumentException("Array length must be greater than zero");
        setSeed(array.hashCode());
    }
}