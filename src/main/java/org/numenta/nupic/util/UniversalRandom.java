package org.numenta.nupic.util;

import java.util.Random;

public class UniversalRandom extends Random {
    /** serial version */
    private static final long serialVersionUID = 1L;

    long seed;
    
    static final String BadBound = "bound must be positive";
    
    public UniversalRandom(long seed) {
        this.seed = seed;
    }
    
    /**
     * Sets the long value used as the initial seed
     * 
     * @param   seed    the value with which to be initialized
     */
    public void setSeed(long seed) {
        this.seed = seed;
    }
    
    /**
     * Returns the long value used as the initial seed
     * 
     * @return  the initial seed value
     */
    public long getSeed() {
        return seed;
    }
    
    public int nextInt() {
        return next(32);
    }
    
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);

        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int)((bound * (long)r) >> 31);
        else {
            for (int u = r;
                 u - (r = u % bound) + m < 0;
                 u = next(31))
                ;
        }
        return r;
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
    
    public static void main(String[] args) {
        UniversalRandom random = new UniversalRandom(42);
        
        long s = 2858730232218250L;
        long e = (s >>> 35);
        System.out.println("e = " + e);
        
        int x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        for(int i = 0;i < 10;i++) {
            int o = random.nextInt(50);
            System.out.println("x = " + o);
        }
        
        ///////////////////////////////////
        //      Values Seen in Python    //
        ///////////////////////////////////
        /*
         *  e = 83200
            x = 0
            x = 26
            x = 14
            x = 15
            x = 38
            x = 47
            x = 13
            x = 9
            x = 15
            x = 31
            x = 6
            x = 3
            x = 0
            x = 21
            x = 45
         */
    }

}
