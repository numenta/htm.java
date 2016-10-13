/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero Public License for more details.
 *
 * You should have received a copy of the GNU Affero Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */
package org.numenta.nupic.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

/**
 * <p>
 * This also has a Python version which is guaranteed to output the same random
 * numbers if given the same initial seed value.
 * </p><p>
 * Implementation of George Marsaglia's elegant Xorshift random generator
 * 30% faster and better quality than the built-in java.util.random. 
 * <p>
 * see http://www.javamex.com/tutorials/random_numbers/xorshift.shtml.
 * @author cogmission
 */
public class UniversalRandom extends Random {
    /** serial version */
    private static final long serialVersionUID = 1L;
    
    private static final MathContext MATH_CONTEXT = new MathContext(9);

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
    @Override
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
    
    /*
     * Internal method used for testing
     */
    private int[] sampleWithPrintout(TIntArrayList choices, int[] selectedIndices, List<Integer> collectedRandoms) {
        TIntArrayList choiceSupply = new TIntArrayList(choices);
        int upperBound = choices.size();
        for (int i = 0; i < selectedIndices.length; i++) {
            int randomIdx = nextInt(upperBound);
            //System.out.println("randomIdx: " + randomIdx);
            collectedRandoms.add(randomIdx);
            selectedIndices[i] = (choiceSupply.removeAt(randomIdx));
            upperBound--;
        }
        Arrays.sort(selectedIndices);
        return selectedIndices;
    }
    
    /**
     * Returns a random, sorted, and  unique list of the specified sample size of
     * selections from the specified list of choices.
     * 
     * @param choices
     * @param selectedIndices
     * @return an array containing a sampling of the specified choices
     */
    public int[] sample(TIntArrayList choices, int[] selectedIndices) {
        TIntArrayList choiceSupply = new TIntArrayList(choices);
        int upperBound = choices.size();
        for (int i = 0; i < selectedIndices.length; i++) {
            int randomIdx = nextInt(upperBound);
            selectedIndices[i] = (choiceSupply.removeAt(randomIdx));
            upperBound--;
        }
        Arrays.sort(selectedIndices);
        //System.out.println("sample: " + Arrays.toString(selectedIndices));
        return selectedIndices;
    }
    
    /**
     * Fisher-Yates implementation which shuffles the array contents.
     * 
     * @param array     the array of ints to shuffle.
     * @return shuffled array
     */
    public int[] shuffle(int[] array) {
        int index;
        for (int i = array.length - 1; i > 0; i--) {
            index = nextInt(i + 1);
            if (index != i) {
                array[index] ^= array[i];
                array[i] ^= array[index];
                array[index] ^= array[i];
            }
        }
        //System.out.println("shuffle: " + Arrays.toString(array));
        return array;
    }
    
    /**
     * Returns an array of floating point values of the specified shape
     * 
     * @param rows      the number of rows
     * @param cols      the number of cols
     * @return
     */
    public double[][] rand(int rows, int cols) {
        double[][] retval = new double[rows][cols];
        for(int i = 0;i < rows;i++) {
            for(int j = 0;j < cols;j++) {
                retval[i][j] = nextDouble();
            }
        }
        return retval;
    }
    
    /**
     * Returns an array of binary values of the specified shape whose
     * total number of "1's" will reflect the sparsity specified.
     * 
     * @param rows          the number of rows
     * @param cols          the number of cols
     * @param sparsity      number between 0 and 1, indicating percentage
     *                      of "on" bits
     * @return
     */
    public int[][] binDistrib(int rows, int cols, double sparsity) {
        double[][] rand = rand(rows, cols);
        
        for(int i = 0;i < rand.length;i++) {
            TIntArrayList sub = new TIntArrayList(
                ArrayUtils.where(rand[i], new Condition.Adapter<Double>() {
                    @Override public boolean eval(double d) {
                        return d >= sparsity;
                    }
                }));
            
            int sublen = sub.size();
            int target = (int)(sparsity * cols);
            
            if(sublen < target) {
                int[] full = IntStream.range(0, cols).toArray();
                TIntHashSet subSet = new TIntHashSet(sub);
                TIntArrayList toFill = new TIntArrayList(
                    Arrays.stream(full)
                        .filter(d -> !subSet.contains(d))
                        .toArray());
                int cnt = toFill.size();
                for(int x = 0;x < target - sublen;x++, cnt--) {
                    int ind = nextInt(cnt);
                    int item = toFill.removeAt(ind);
                    rand[i][item] = sparsity;
                }
            }else if(sublen > target) {
                int cnt = sublen;
                for(int x = 0;x < sublen - target;x++, cnt--) {
                    int ind = nextInt(cnt);
                    int item = sub.removeAt(ind);
                    rand[i][item] = 0.0;
                }
            }
        }
        
        int[][] retval = Arrays.stream(rand)
            .map(da -> Arrays.stream(da).mapToInt(d -> d >= sparsity ? 1 : 0).toArray())
            .toArray(int[][]::new);
        return retval;
    }
    
    @Override
    public double nextDouble() {
        int nd = nextInt(10000);
        double retVal = new BigDecimal(nd * .0001d, MATH_CONTEXT).doubleValue();
        //System.out.println("nextDouble: " + retVal);
        return retVal;
    }
    
    @Override
    public int nextInt() {
        int retVal = nextInt(Integer.MAX_VALUE);
        //System.out.println("nextIntNB: " + retVal);
        return retVal;
    }
    
    @Override
    public int nextInt(int bound) {
        if (bound <= 0)
            throw new IllegalArgumentException(BadBound);

        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0)  // i.e., bound is a power of 2
            r = (int)((bound * (long)r) >> 31);
        else {
            r = r % bound;
            /*
            THIS CODE IS COMMENTED TO WORK IDENTICALLY WITH THE PYTHON VERSION 
             
            for (int u = r;
                 u - (r = u % bound) + m < 0;
                 u = next(31))
                ;
            */
        }
        //System.out.println("nextInt(" + bound + "): " + r);
        return r;
    }
    
    /**
     * Implementation of George Marsaglia's elegant Xorshift random generator
     * 30% faster and better quality than the built-in java.util.random see also
     * see http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
     */
    protected int next(int nbits) {
        long x = seed;
        x ^= (x << 21) & 0xffffffffffffffffL;
        x ^= (x >>> 35);
        x ^= (x << 4);
        seed = x;
        x &= ((1L << nbits) - 1);
        
        return (int) x;
    }
    
    BigInteger bigSeed;
    /**
     * PYTHON COMPATIBLE (Protected against overflows)
     * 
     * Implementation of George Marsaglia's elegant Xorshift random generator
     * 30% faster and better quality than the built-in java.util.random see also
     * see http://www.javamex.com/tutorials/random_numbers/xorshift.shtml
     */
    protected int nextX(int nbits) {
        long x = seed;
        BigInteger bigX = bigSeed == null ? BigInteger.valueOf(seed) : bigSeed;
        bigX = bigX.shiftLeft(21).xor(bigX).and(new BigInteger("ffffffffffffffff", 16));
        bigX = bigX.shiftRight(35).xor(bigX).and(new BigInteger("ffffffffffffffff", 16));
        bigX = bigX.shiftLeft(4).xor(bigX).and(new BigInteger("ffffffffffffffff", 16));
        bigSeed = bigX;
        bigX = bigX.and(BigInteger.valueOf(1L).shiftLeft(nbits).subtract(BigInteger.valueOf(1)));
        x = bigX.intValue();
        
        //System.out.println("x = " + x + ",  seed = " + seed);
        return (int)x;
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
        
        random = new UniversalRandom(42);
        for(int i = 0;i < 10;i++) {
            double o = random.nextDouble();
            System.out.println("d = " + o);
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
            d = 0.945
            d = 0.2426
            d = 0.5214
            d = 0.0815
            d = 0.0988
            d = 0.5497
            d = 0.4013
            d = 0.4559
            d = 0.5415
            d = 0.2381
         */
        
        random = new UniversalRandom(42);
        TIntArrayList choices = new TIntArrayList(new int[] { 1,2,3,4,5,6,7,8,9 });
        int sampleSize = 6;
        int[] selectedIndices = new int[sampleSize];
        List<Integer> collectedRandoms = new ArrayList<>();
        int[] expectedSample = {1,2,3,7,8,9};
        List<Integer> expectedRandoms = Arrays.stream(new int[] {0,0,0,5,3,3}).boxed().collect(Collectors.toList());
        random.sampleWithPrintout(choices, selectedIndices, collectedRandoms);
        System.out.println("samples are equal ? " + Arrays.equals(expectedSample, selectedIndices));
        System.out.println("used randoms are equal ? " + collectedRandoms.equals(expectedRandoms));
        
        random = new UniversalRandom(42);
        int[] coll = ArrayUtils.range(0, 10);
        int[] before = Arrays.copyOf(coll, coll.length);
        random.shuffle(coll);
        System.out.println("collection before: " + Arrays.toString(before));
        System.out.println("collection shuffled: " + Arrays.toString(coll));
        int[] expected = { 5, 1, 8, 6, 2, 4, 7, 3, 9, 0 };
        System.out.println(Arrays.equals(expected, coll));
        System.out.println(!Arrays.equals(expected, before)); // not equal
    }

}
