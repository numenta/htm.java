/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic.util;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TDoubleIntHashMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Utilities to match some of the functionality found in Python's Numpy.
 * @author David Ray
 */
public class ArrayUtils {
    /** Empty array constant */
    private static int[] EMPTY_ARRAY = new int[0];
    
    public static Condition<Integer> WHERE_1 = new Condition.Adapter<Integer>() {
        public boolean eval(int i) {
            return i == 1;
        }
    };
    public static Condition<Double> GREATER_THAN_0 = new Condition.Adapter<Double>() {
        public boolean eval(double i) {
            return i > 0;
        }
    };
    public static Condition<Integer> INT_GREATER_THAN_0 = new Condition.Adapter<Integer>() {
        public boolean eval(int i) {
            return i > 0;
        }
    };
    public static Condition<Integer> GREATER_OR_EQUAL_0 = new Condition.Adapter<Integer>() {
        @Override public boolean eval(int n) { return n >= 0; }
    };
    
    
    /**
     * Returns the product of each integer in the specified array.
     * 
     * @param dims
     * @return
     */
    public static int product(int[] dims) {
        int retVal = 1;
        for(int i = 0;i < dims.length;i++) {
            retVal *= dims[i];
        }
        
        return retVal;
    }
    
    /**
     * Returns an array containing the successive elements of each
     * argument array as in [ first[0], second[0], first[1], second[1], ... ].
     * 
     * Arrays may be of zero length, and may be of different sizes, but may not be null.
     * 
     * @param first     the first array
     * @param second    the second array
     * @return
     */
    public static <F, S> Object[] interleave(F first, S second) {
        int flen, slen;
        Object[] retVal = new Object[(flen = Array.getLength(first)) + (slen = Array.getLength(second))];
        for(int i = 0, j = 0, k = 0;i < flen || j < slen;) {
            if(i < flen) {
                retVal[k++] = Array.get(first, i++);
            }
            if(j < slen) {
                retVal[k++] = Array.get(second, j++);
            }
        }
        
        return retVal;
    }
    
    /**
     * Return a new double[] containing the difference of each element and its
     * succeding element.
     * <p/>
     * The first order difference is given by ``out[n] = a[n+1] - a[n]``
     * along the given axis, higher order differences are calculated by using `diff`
     * recursively.
     *
     * @param d
     * @return
     */
    public static double[] diff(double[] d) {
        double[] retVal = new double[d.length - 1];
        for (int i = 0; i < retVal.length; i++) {
            retVal[i] = d[i + 1] - d[i];
        }

        return retVal;
    }

    /**
     * Returns a flag indicating whether the container list contains an
     * array which matches the specified match array.
     *
     * @param match     the array to match
     * @param container the list of arrays to test
     * @return true if so, false if not
     */
    public static boolean contains(int[] match, List<int[]> container) {
        int len = container.size();
        for (int i = 0; i < len; i++) {
            if (Arrays.equals(match, container.get(i))) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Returns a new array of size first.length + second.length, with the
     * contents of the first array loaded into the returned array starting
     * at the zero'th index, and the contents of the second array appended
     * to the returned array beginning with index first.length.
     * 
     * This method is fail fast, meaning that it depends on the two arrays
     * being non-null, and if not, an exception is thrown.
     *  
     * @param first     the data to load starting at index 0
     * @param second    the data to load starting at index first.length;
     * @return  a concatenated array
     * @throws NullPointerException if either first or second is null
     */
    public static double[] concat(double[] first, double[] second) {
        double[] retVal = Arrays.copyOf(first, first.length + second.length);
        for(int i = first.length, j = 0;i < retVal.length;i++, j++) {
            retVal[i] = second[j];
        }
        return retVal;
    }

    /**
     * Utility to compute a flat index from coordinates.
     *
     * @param coordinates an array of integer coordinates
     * @return a flat index
     */
    public static int fromCoordinate(int[] coordinates, int[] shape) {
        int[] localMults = initDimensionMultiples(shape);
        int base = 0;
        for (int i = 0; i < coordinates.length; i++) {
            base += (localMults[i] * coordinates[i]);
        }
        return base;
    }

    /**
     * Utility to compute a flat index from coordinates.
     *
     * @param coordinates an array of integer coordinates
     * @return a flat index
     */
    public static int fromCoordinate(int[] coordinates) {
        int[] localMults = initDimensionMultiples(coordinates);
        int base = 0;
        for (int i = 0; i < coordinates.length; i++) {
            base += (localMults[i] * coordinates[i]);
        }
        return base;
    }

    /**
     * Initializes internal helper array which is used for multidimensional
     * index computation.
     *
     * @param dimensions
     * @return
     */
    public static int[] initDimensionMultiples(int[] dimensions) {
        int holder = 1;
        int len = dimensions.length;
        int[] dimensionMultiples = new int[dimensions.length];
        for (int i = 0; i < len; i++) {
            holder *= (i == 0 ? 1 : dimensions[len - i]);
            dimensionMultiples[len - 1 - i] = holder;
        }
        return dimensionMultiples;
    }

    /**
     * Returns a string representing an array of 0's and 1's
     *
     * @param arr an binary array (0's and 1's only)
     * @return
     */
    public static String bitsToString(int[] arr) {
        char[] s = new char[arr.length + 1];
        Arrays.fill(s, '.');
        s[0] = 'c';
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] == 1) {
                s[i + 1] = '*';
            }
        }
        return new String(s);
    }

    /**
     * Return a list of tuples, where each tuple contains the i-th element
     * from each of the argument sequences.  The returned list is
     * truncated in length to the length of the shortest argument sequence.
     *
     * @param arg1 the first list to be the zero'th entry in the returned tuple
     * @param arg2 the first list to be the one'th entry in the returned tuple
     * @return a list of tuples
     */
    public static List<Tuple> zip(List<?> arg1, List<?> arg2) {
        List<Tuple> tuples = new ArrayList<Tuple>();
        int len = Math.min(arg1.size(), arg2.size());
        for (int i = 0; i < len; i++) {
            tuples.add(new Tuple(arg1.get(i), arg2.get(i)));
        }

        return tuples;
    }
    
    /**
     * Return a list of tuples, where each tuple contains the i-th element
     * from each of the argument sequences.  The returned list is
     * truncated in length to the length of the shortest argument sequence.
     *
     * @param arg1 the first list to be the zero'th entry in the returned tuple
     * @param arg2 the first list to be the one'th entry in the returned tuple
     * @return a list of tuples
     */
    public static List<Tuple> zip(Object[]... args) {
        List<Tuple> tuples = new ArrayList<Tuple>();
        int len = args.length;
        for (int i = 0; i < len; i++) {
            tuples.add(new Tuple(args[i]));
        }

        return tuples;
    }

    /**
     * Returns an array with the same shape and the contents
     * converted to doubles.
     *
     * @param ints an array of ints.
     * @return
     */
    public static double[] toDoubleArray(int[] ints) {
        double[] retVal = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            retVal[i] = ints[i];
        }
        return retVal;
    }

    /**
     * Performs a modulus operation in Python style.
     *
     * @param a
     * @param b
     * @return
     */
    public static int modulo(int a, int b) {
        if (b == 0) throw new IllegalArgumentException("Division by Zero!");
        if (a > 0 && b > 0 && b > a) return a;
        boolean isMinus = Math.abs(b - (a - b)) < Math.abs(b - (a + b));
        if (isMinus) {
            while (a >= b) {
                a -= b;
            }
        } else {
            if (a % b == 0) return 0;

            while (a + b < b) {
                a += b;
            }
        }
        return a;
    }

    /**
     * Performs a modulus on every index of the first argument using
     * the second argument and places the result in the same index of
     * the first argument.
     *
     * @param a
     * @param b
     * @return
     */
    public static int[] modulo(int[] a, int b) {
        for (int i = 0; i < a.length; i++) {
            a[i] = modulo(a[i], b);
        }
        return a;
    }

    /**
     * Returns an array with the same shape and the contents
     * converted to integers.
     *
     * @param doubs an array of doubles.
     * @return
     */
    public static int[] toIntArray(double[] doubs) {
        int[] retVal = new int[doubs.length];
        for (int i = 0; i < doubs.length; i++) {
            retVal[i] = (int)doubs[i];
        }
        return retVal;
    }

    /**
     * Returns a double array whose values are the maximum of the value
     * in the array and the max value argument.
     * @param doubs
     * @param maxValue
     * @return
     */
    public static double[] maximum(double[] doubs, double maxValue) {
        double[] retVal = new double[doubs.length];
        for (int i = 0; i < doubs.length; i++) {
            retVal[i] = Math.max(doubs[i], maxValue);
        }
        return retVal;
    }

    /**
     * Returns an array of identical shape containing the maximum
     * of the values between each corresponding index. Input arrays
     * must be the same length.
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static int[] maxBetween(int[] arr1, int[] arr2) {
        int[] retVal = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            retVal[i] = Math.max(arr1[i], arr2[i]);
        }
        return retVal;
    }

    /**
     * Returns an array of identical shape containing the minimum
     * of the values between each corresponding index. Input arrays
     * must be the same length.
     *
     * @param arr1
     * @param arr2
     * @return
     */
    public static int[] minBetween(int[] arr1, int[] arr2) {
        int[] retVal = new int[arr1.length];
        for (int i = 0; i < arr1.length; i++) {
            retVal[i] = Math.min(arr1[i], arr2[i]);
        }
        return retVal;
    }

    /**
     * Returns an array of values that test true for all of the
     * specified {@link Condition}s.
     *
     * @param values
     * @param conditions
     * @return
     */
    public static int[] retainLogicalAnd(int[] values, Condition<?>[] conditions) {
        TIntArrayList l = new TIntArrayList();
        for (int i = 0; i < values.length; i++) {
            boolean result = true;
            for (int j = 0; j < conditions.length && result; j++) {
                result &= conditions[j].eval(values[i]);
            }
            if (result) l.add(values[i]);
        }
        return l.toArray();
    }

    /**
     * Returns an array of values that test true for all of the
     * specified {@link Condition}s.
     *
     * @param values
     * @param conditions
     * @return
     */
    public static double[] retainLogicalAnd(double[] values, Condition<?>[] conditions) {
        TDoubleArrayList l = new TDoubleArrayList();
        for (int i = 0; i < values.length; i++) {
            boolean result = true;
            for (int j = 0; j < conditions.length && result; j++) {
                result &= conditions[j].eval(values[i]);
            }
            if (result) l.add(values[i]);
        }
        return l.toArray();
    }

    /**
     * Returns an array whose members are the quotient of the dividend array
     * values and the divisor array values.
     *
     * @param dividend
     * @param divisor
     * @param dividend adjustment
     * @param divisor  adjustment
     *
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] divide(double[] dividend, double[] divisor,
                                  double dividendAdjustment, double divisorAdjustment) {

        if (dividend.length != divisor.length) {
            throw new IllegalArgumentException(
                    "The dividend array and the divisor array must be the same length");
        }
        double[] quotient = new double[dividend.length];
        double denom = 1;
        for (int i = 0; i < dividend.length; i++) {
            quotient[i] = (dividend[i] + dividendAdjustment) /
                          ((denom = divisor[i] + divisorAdjustment) == 0 ? 1 : denom); //Protect against division by 0
        }
        return quotient;
    }

    /**
     * Returns an array whose members are the quotient of the dividend array
     * values and the divisor array values.
     *
     * @param dividend
     * @param divisor
     * @param dividend adjustment
     * @param divisor  adjustment
     *
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] divide(int[] dividend, int[] divisor) {

        if (dividend.length != divisor.length) {
            throw new IllegalArgumentException(
                    "The dividend array and the divisor array must be the same length");
        }
        double[] quotient = new double[dividend.length];
        double denom = 1;
        for (int i = 0; i < dividend.length; i++) {
            quotient[i] = (dividend[i]) /
                          (double)((denom = divisor[i]) == 0 ? 1 : denom); //Protect against division by 0
        }
        return quotient;
    }

    /**
     * Returns an array whose members are the quotient of the dividend array
     * values and the divisor value.
     *
     * @param dividend
     * @param divisor
     * @param dividend adjustment
     * @param divisor  adjustment
     *
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] divide(double[] dividend, double divisor) {
        double[] quotient = new double[dividend.length];
        double denom = 1;
        for (int i = 0; i < dividend.length; i++) {
            quotient[i] = (dividend[i]) /
                          (double)((denom = divisor) == 0 ? 1 : denom); //Protect against division by 0
        }
        return quotient;
    }

    /**
     * Returns an array whose members are the quotient of the dividend array
     * values and the divisor array values.
     *
     * @param dividend
     * @param divisor
     * @param dividend adjustment
     * @param divisor  adjustment
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] roundDivide(double[] dividend, double[] divisor, int scale) {

        if (dividend.length != divisor.length) {
            throw new IllegalArgumentException(
                    "The dividend array and the divisor array must be the same length");
        }
        double[] quotient = new double[dividend.length];
        for (int i = 0; i < dividend.length; i++) {
            quotient[i] = (dividend[i]) / (divisor[i] == 0 ? 1 : divisor[i]); //Protect against division by 0
            quotient[i] = new BigDecimal(quotient[i]).round(new MathContext(scale, RoundingMode.HALF_UP)).doubleValue();
        }
        return quotient;
    }

    /**
     * Returns an array whose members are the product of the multiplicand array
     * values and the factor array values.
     *
     * @param multiplicand
     * @param factor
     * @param multiplicand adjustment
     * @param factor       adjustment
     *
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] multiply(
    	double[] multiplicand, double[] factor, double multiplicandAdjustment, double factorAdjustment) {

        if (multiplicand.length != factor.length) {
            throw new IllegalArgumentException(
                    "The multiplicand array and the factor array must be the same length");
        }
        double[] product = new double[multiplicand.length];
        for (int i = 0; i < multiplicand.length; i++) {
            product[i] = (multiplicand[i] + multiplicandAdjustment) * (factor[i] + factorAdjustment);
        }
        return product;
    }

    /**
     * Returns an array whose members are the product of the multiplicand array
     * values and the factor array values.
     *
     * @param multiplicand
     * @param factor
     * @param multiplicand adjustment
     * @param factor       adjustment
     *
     * @return
     * @throws IllegalArgumentException if the two argument arrays are not the same length
     */
    public static double[] multiply(double[] multiplicand, int[] factor) {

        if (multiplicand.length != factor.length) {
            throw new IllegalArgumentException(
                    "The multiplicand array and the factor array must be the same length");
        }
        double[] product = new double[multiplicand.length];
        for (int i = 0; i < multiplicand.length; i++) {
            product[i] = (multiplicand[i]) * (factor[i]);
        }
        return product;
    }

    /**
     * Returns a new array containing the result of multiplying
     * each index of the specified array by the 2nd parameter.
     *
     * @param array
     * @param d
     * @return
     */
    public static int[] multiply(int[] array, int d) {
        int[] product = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            product[i] = array[i] * d;
        }
        return product;
    }

    /**
     * Returns a new array containing the result of multiplying
     * each index of the specified array by the 2nd parameter.
     *
     * @param array
     * @param d
     * @return
     */
    public static double[] multiply(double[] array, double d) {
        double[] product = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            product[i] = array[i] * d;
        }
        return product;
    }

    /**
     * Returns an integer array containing the result of subtraction
     * operations between corresponding indexes of the specified arrays.
     *
     * @param minuend
     * @param subtrahend
     * @return
     */
    public static int[] subtract(int[] minuend, int[] subtrahend) {
        int[] retVal = new int[minuend.length];
        for (int i = 0; i < minuend.length; i++) {
            retVal[i] = minuend[i] - subtrahend[i];
        }
        return retVal;
    }

    /**
     * Subtracts the contents of the first argument from the last argument's list.
     *
     * <em>NOTE: Does not destroy/alter the argument lists. </em>
     *
     * @param minuend
     * @param subtrahend
     * @return
     */
    public static List<Integer> subtract(List<Integer> subtrahend, List<Integer> minuend) {
        ArrayList<Integer> sList = new ArrayList<Integer>(minuend);
        sList.removeAll(subtrahend);
        return new ArrayList<Integer>(sList);
    }

    /**
     * Returns the average of all the specified array contents.
     * @param arr
     * @return
     */
    public static double average(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum / (double)arr.length;
    }

    /**
     * Returns the average of all the specified array contents.
     * @param arr
     * @return
     */
    public static double average(double[] arr) {
        double sum = 0;
        for (int i = 0; i < arr.length; i++) {
            sum += arr[i];
        }
        return sum / (double)arr.length;
    }
    
    /**
     * Computes and returns the variance.
     * @param arr
     * @param mean
     * @return
     */
    public static double variance(double[] arr, double mean) {
        double accum = 0.0;
        double dev = 0.0;
        double accum2 = 0.0;
        for (int i = 0; i < arr.length; i++) {
            dev = arr[i] - mean;
            accum += dev * dev;
            accum2 += dev;
        }
        
        double var = (accum - (accum2 * accum2 / arr.length)) / arr.length;
        
        return var;
    }
    
    /**
     * Computes and returns the variance.
     * @param arr
     * @return
     */
    public static double variance(double[] arr) {
        return variance(arr, average(arr));
    }

    /**
     * Returns the passed in array with every value being altered
     * by the addition of the specified amount.
     *
     * @param arr
     * @param amount
     * @return
     */
    public static int[] add(int[] arr, int amount) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] += amount;
        }
        return arr;
    }

    /**
     * Returns the passed in array with every value being altered
     * by the addition of the specified double amount at the same
     * index
     *
     * @param arr
     * @param amount
     * @return
     */
    public static int[] i_add(int[] arr, int[] amount) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] += amount[i];
        }
        return arr;
    }

    /**
     * Returns the passed in array with every value being altered
     * by the addition of the specified double amount at the same
     * index
     *
     * @param arr
     * @param amount
     * @return
     */
    public static double[] d_add(double[] arr, double[] amount) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] += amount[i];
        }
        return arr;
    }

    /**
     * Returns the passed in array with every value being altered
     * by the addition of the specified double amount
     *
     * @param arr
     * @param amount
     * @return
     */
    public static double[] d_add(double[] arr, double amount) {
        for (int i = 0; i < arr.length; i++) {
            arr[i] += amount;
        }
        return arr;
    }

    /**
     * Returns the sum of all contents in the specified array.
     * @param array
     * @return
     */
    public static int sum(int[] array) {
        int sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }
    
    /**
     * Test whether each element of a 1-D array is also present in a second 
     * array.
     *
     * Returns a int array whose length is the number of intersections.
     * 
     * @param ar1   the array of values to find in the second array 
     * @param ar2   the array to test for the presence of elements in the first array.
     * @return  an array containing the intersections or an empty array if none are found.
     */
    public static int[] in1d(int[] ar1, int[] ar2) {
        if(ar1 == null || ar2 == null) {
            return EMPTY_ARRAY;
        }
        
        TIntSet retVal = new TIntHashSet(ar2);
        retVal.retainAll(ar1);
        return retVal.toArray();
    }
    
    /**
     * Returns the sum of all contents in the specified array.
     * @param array
     * @return
     */
    public static double sum(double[] array) {
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i];
        }
        return sum;
    }

    /**
     * Sparse or due to the arrays containing the indexes of "on bits",
     * the <em>or</em> of which is equal to the mere combination of the two
     * arguments - eliminating duplicates and sorting.
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static int[] sparseBinaryOr(int[] arg1, int[] arg2) {
        TIntArrayList t = new TIntArrayList(arg1);
        t.addAll(arg2);
        return unique(t.toArray());
    }

    /**
     * Prints the specified array to a returned String.
     *
     * @param aObject the array object to print.
     * @return the array in string form suitable for display.
     */
    public static String print1DArray(Object aObject) {
        if (aObject.getClass().isArray()) {
            if (aObject instanceof Object[]) // can we cast to Object[]
            {
                return Arrays.toString((Object[])aObject);
            } else {  // we can't cast to Object[] - case of primitive arrays
                int length = Array.getLength(aObject);
                Object[] objArr = new Object[length];
                for (int i = 0; i < length; i++)
                    objArr[i] = Array.get(aObject, i);
                return Arrays.toString(objArr);
            }
        }
        return "[]";
    }

    /**
     * Another utility to account for the difference between Python and Java.
     * Here the modulo operator is defined differently.
     *
     * @param n
     * @param divisor
     * @return
     */
    public static double positiveRemainder(double n, double divisor) {
        if (n >= 0) {
            return n % divisor;
        } else {
            double val = divisor + (n % divisor);
            return val == divisor ? 0 : val;
        }
    }

    /**
     * Returns an array which starts from lowerBounds (inclusive) and
     * ends at the upperBounds (exclusive).
     *
     * @param lowerBounds
     * @param upperBounds
     * @return
     */
    public static int[] range(int lowerBounds, int upperBounds) {
        TIntList ints = new TIntArrayList();
        for (int i = lowerBounds; i < upperBounds; i++) {
            ints.add(i);
        }
        return ints.toArray();
    }

    /**
     * Returns an array which starts from lowerBounds (inclusive) and
     * ends at the upperBounds (exclusive).
     *
     * @param lowerBounds the starting value
     * @param upperBounds the maximum value (exclusive)
     * @param interval    the amount by which to increment the values
     * @return
     */
    public static double[] arange(double lowerBounds, double upperBounds, double interval) {
        TDoubleList doubs = new TDoubleArrayList();
        for (double i = lowerBounds; i < upperBounds; i += interval) {
            doubs.add(i);
        }
        return doubs.toArray();
    }

    /**
     * Returns a sorted unique array of integers
     *
     * @param nums an unsorted array of integers with possible duplicates.
     * @return
     */
    public static int[] unique(int[] nums) {
        TIntHashSet set = new TIntHashSet(nums);
        int[] result = set.toArray();
        Arrays.sort(result);
        return result;
    }

    /**
     * Helper Class for recursive coordinate assembling
     */
    private static class CoordinateAssembler {
        final private int[] position;
        final private List<int[]> dimensions;
        final List<int[]> result = new ArrayList<int[]>();

        public static List<int[]> assemble(List<int[]> dimensions) {
            CoordinateAssembler assembler = new CoordinateAssembler(dimensions);
            assembler.process(dimensions.size());
            return assembler.result;
        }

        private CoordinateAssembler(List<int[]> dimensions) {
            this.dimensions = dimensions;
            position = new int[dimensions.size()];
        }

        private void process(int level) {
            if (level == 0) {// terminating condition
                int[] coordinates = new int[position.length];
                System.arraycopy(position, 0, coordinates, 0, position.length);
                result.add(coordinates);
            } else {// inductive condition
                int index = dimensions.size() - level;
                int[] currentDimension = dimensions.get(index);
                for (int i = 0; i < currentDimension.length; i++) {
                    position[index] = currentDimension[i];
                    process(level - 1);
                }
            }
        }
    }


    /**
     * Called to merge a list of dimension arrays into a sequential row-major indexed
     * list of coordinates.
     *
     * @param dimensions a list of dimension arrays, each array being a dimension
     *                   of an n-dimensional array.
     * @return a list of n-dimensional coordinates in row-major format.
     */
    public static List<int[]> dimensionsToCoordinateList(List<int[]> dimensions) {
        return CoordinateAssembler.assemble(dimensions);
    }

    /**
     * Sets the values in the specified values array at the indexes specified,
     * to the value "setTo".
     *
     * @param values  the values to alter if at the specified indexes.
     * @param indexes the indexes of the values array to alter
     * @param setTo   the value to set at the specified indexes.
     */
    public static void setIndexesTo(double[] values, int[] indexes, double setTo) {
        for (int i = 0; i < indexes.length; i++) {
            values[indexes[i]] = setTo;
        }
    }

    /**
     * Sets the values in the specified values array at the indexes specified,
     * to the value "setTo".
     *
     * @param values  the values to alter if at the specified indexes.
     * @param indexes the indexes of the values array to alter
     * @param setTo   the value to set at the specified indexes.
     */
    public static void setIndexesTo(int[] values, int[] indexes, int setTo) {
        for (int i = 0; i < indexes.length; i++) {
            values[indexes[i]] = setTo;
        }
    }

    /**
     * Sets the values in range start to stop to the value specified. If
     * stop < 0, then stop indicates the number of places counting from the
     * length of "values" back.
     *
     * @param values the array to alter
     * @param start  the start index (inclusive)
     * @param stop   the end index (exclusive)
     * @param setTo  the value to set the indexes to
     */
    public static void setRangeTo(int[] values, int start, int stop, int setTo) {
        stop = stop < 0 ? values.length + stop : stop;
        for (int i = start; i < stop; i++) {
            values[i] = setTo;
        }
    }

    /**
     * Returns a random, sorted, and  unique array of the specified sample size of
     * selections from the specified list of choices.
     *
     * @param sampleSize the number of selections in the returned sample
     * @param choices    the list of choices to select from
     * @param random     a random number generator
     * @return a sample of numbers of the specified size
     */
    public static int[] sample(int sampleSize, TIntArrayList choices, Random random) {
        TIntHashSet temp = new TIntHashSet();
        int upperBound = choices.size();
        for (int i = 0; i < sampleSize; i++) {
            int randomIdx = random.nextInt(upperBound);
            while (temp.contains(choices.get(randomIdx))) {
                randomIdx = random.nextInt(upperBound);
            }
            temp.add(choices.get(randomIdx));
        }
        TIntArrayList al = new TIntArrayList(temp);
        al.sort();
        return al.toArray();
    }

    /**
     * Returns a double[] filled with random doubles of the specified size.
     * @param sampleSize
     * @param random
     * @return
     */
    public static double[] sample(int sampleSize, Random random) {
        double[] sample = new double[sampleSize];
        for (int i = 0; i < sampleSize; i++) {
            sample[i] = random.nextDouble();
        }
        return sample;
    }

    /**
     * Ensures that each entry in the specified array has a min value
     * equal to or greater than the specified min and a maximum value less
     * than or equal to the specified max.
     *
     * @param values the values to clip
     * @param min    the minimum value
     * @param max    the maximum value
     */
    public static double[] clip(double[] values, double min, double max) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.min(1, Math.max(0, values[i]));
        }
        return values;
    }

    /**
     * Ensures that each entry in the specified array has a min value
     * equal to or greater than the min at the specified index and a maximum value less
     * than or equal to the max at the specified index.
     *
     * @param values the values to clip
     * @param min    the minimum value
     * @param max    the maximum value
     */
    public static int[] clip(int[] values, int[] min, int[] max) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(min[i], Math.min(max[i], values[i]));
        }
        return values;
    }

    /**
     * Ensures that each entry in the specified array has a min value
     * equal to or greater than the min at the specified index and a maximum value less
     * than or equal to the max at the specified index.
     *
     * @param values the values to clip
     * @param max    the minimum value
     * @param adj    the adjustment amount
     */
    public static int[] clip(int[] values, int[] max, int adj) {
        for (int i = 0; i < values.length; i++) {
            values[i] = Math.max(0, Math.min(max[i] + adj, values[i]));
        }
        return values;
    }

    /**
     * Returns the count of values in the specified array that are
     * greater than the specified compare value
     *
     * @param compare the value to compare to
     * @param array   the values being compared
     *
     * @return the count of values greater
     */
    public static int valueGreaterCount(double compare, double[] array) {
        int count = 0;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > compare) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns the count of values in the specified array that are
     * greater than the specified compare value
     *
     * @param compare the value to compare to
     * @param array   the values being compared
     *
     * @return the count of values greater
     */
    public static int valueGreaterCountAtIndex(double compare, double[] array, int[] indexes) {
        int count = 0;
        for (int i = 0; i < indexes.length; i++) {
            if (array[indexes[i]] > compare) {
                count++;
            }
        }

        return count;
    }

    /**
     * Returns an array containing the n greatest values.
     * @param array
     * @param n
     * @return
     */
    public static int[] nGreatest(double[] array, int n) {
        TDoubleIntHashMap places = new TDoubleIntHashMap();
        int i;
        double key;
        for (int j = 1; j < array.length; j++) {
            key = array[j];
            for (i = j - 1; i >= 0 && array[i] < key; i--) {
                array[i + 1] = array[i];
            }
            array[i + 1] = key;
            places.put(key, j);
        }

        int[] retVal = new int[n];
        for (i = 0; i < n; i++) {
            retVal[i] = places.get(array[i]);
        }
        return retVal;
    }

    /**
     * Raises the values in the specified array by the amount specified
     * @param amount the amount to raise the values
     * @param values the values to raise
     */
    public static void raiseValuesBy(double amount, double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] += amount;
        }
    }

    /**
     * Raises the values at the indexes specified by the amount specified.
     * @param amount the amount to raise the values
     * @param values the values to raise
     */
    public static void raiseValuesBy(double amount, double[] values, int[] indexesToRaise) {
        for (int i = 0; i < indexesToRaise.length; i++) {
            values[indexesToRaise[i]] += amount;
        }
    }

    /**
     * Raises the values at the indexes specified by the amount specified.
     * @param amount the amount to raise the values
     * @param values the values to raise
     */
    public static void raiseValuesBy(double[] amounts, double[] values) {
        for (int i = 0; i < values.length; i++) {
            values[i] += amounts[i];
        }
    }

    /**
     * Raises the values at the indicated indexes, by the amount specified
     *
     * @param amount
     * @param indexes
     * @param values
     */
    public static void raiseValuesBy(int amount, int[] indexes, int[] values) {
        for (int i = 0; i < indexes.length; i++) {
            values[indexes[i]] += amount;
        }
    }

    /**
     * Scans the specified values and applies the {@link Condition} to each
     * value, returning the indexes of the values where the condition evaluates
     * to true.
     *
     * @param values the values to test
     * @param c      the condition used to test each value
     * @return
     */
    public static <T> int[] where(double[] d, Condition<T> c) {
        TIntArrayList retVal = new TIntArrayList();
        int len = d.length;
        for (int i = 0; i < len; i++) {
            if (c.eval(d[i])) {
                retVal.add(i);
            }
        }
        return retVal.toArray();
    }

    /**
     * Scans the specified values and applies the {@link Condition} to each
     * value, returning the indexes of the values where the condition evaluates
     * to true.
     *
     * @param values the values to test
     * @param c      the condition used to test each value
     * @return
     */
    public static <T> int[] where(int[] d, Condition<T> c) {
        TIntArrayList retVal = new TIntArrayList();
        int len = d.length;
        for (int i = 0; i < len; i++) {
            if (c.eval(d[i])) {
                retVal.add(i);
            }
        }
        return retVal.toArray();
    }
    
    /**
     * Returns a flag indicating whether the specified array
     * is a sparse array of 0's and 1's or not.
     * 
     * @param ia
     * @return
     */
    public static boolean isSparse(int[] ia) {
        for(int i = ia.length - 1;i >= 0;i--) {
            if(ia[i] > 1) return true;
        }
        return false;
    }
    
    /**
     * Returns a bit vector of the specified size whose "on" bit
     * indexes are specified in "in"; basically converting a sparse
     * array to a dense one.
     * 
     * @param in       the sparse array specifying the on bits of the returned array
     * @param size    the size of the dense array to be returned.
     * @return
     */
    public static int[] asDense(int[] in, int size) {
        int[] retVal = new int[size];
        Arrays.stream(in).forEach(i -> {retVal[i] = 1;});
        return retVal;
    }

    /**
     * Scans the specified values and applies the {@link Condition} to each
     * value, returning the indexes of the values where the condition evaluates
     * to true.
     *
     * @param values the values to test
     * @param c      the condition used to test each value
     * @return
     */
    public static <T> int[] where(List<T> l, Condition<T> c) {
        TIntArrayList retVal = new TIntArrayList();
        int len = l.size();
        for (int i = 0; i < len; i++) {
            if (c.eval(l.get(i))) {
                retVal.add(i);
            }
        }
        return retVal.toArray();
    }

    /**
     * Scans the specified values and applies the {@link Condition} to each
     * value, returning the indexes of the values where the condition evaluates
     * to true.
     *
     * @param values the values to test
     * @param c      the condition used to test each value
     * @return
     */
    public static <T> int[] where(T[] t, Condition<T> c) {
        TIntArrayList retVal = new TIntArrayList();
        for (int i = 0; i < t.length; i++) {
            if (c.eval(t[i])) {
                retVal.add(i);
            }
        }
        return retVal.toArray();
    }

    /**
     * Makes all values in the specified array which are less than or equal to the specified
     * "x" value, equal to the specified "y".
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void lessThanOrEqualXThanSetToY(double[] array, double x, double y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] <= x) array[i] = y;
        }
    }

    /**
     * Makes all values in the specified array which are less than the specified
     * "x" value, equal to the specified "y".
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void lessThanXThanSetToY(double[] array, double x, double y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] < x) array[i] = y;
        }
    }

    /**
     * Makes all values in the specified array which are less than the specified
     * "x" value, equal to the specified "y".
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void lessThanXThanSetToY(int[] array, int x, int y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] < x) array[i] = y;
        }
    }

    /**
     * Makes all values in the specified array which are greater than or equal to the specified
     * "x" value, equal to the specified "y".
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void greaterThanOrEqualXThanSetToY(double[] array, double x, double y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] >= x) array[i] = y;
        }
    }

    /**
     * Makes all values in the specified array which are greater than the specified
     * "x" value, equal to the specified "y".
     *
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void greaterThanXThanSetToY(double[] array, double x, double y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] > x) array[i] = y;
        }
    }

    /**
     * Makes all values in the specified array which are greater than the specified
     * "x" value, equal to the specified "y".
     * @param array
     * @param x     the comparison
     * @param y     the value to set if the comparison fails
     */
    public static void greaterThanXThanSetToY(int[] array, int x, int y) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] > x) array[i] = y;
        }
    }

    /**
     * Returns the index of the max value in the specified array
     * @param array the array to find the max value index in
     * @return the index of the max value
     */
    public static int argmax(int[] array) {
        int index = -1;
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                index = i;
            }
        }
        return index;
    }
    
    /**
     * Returns the index of the max value in the specified array
     * @param array the array to find the max value index in
     * @return the index of the max value
     */
    public static int argmax(double[] array) {
        int index = -1;
        double max = Double.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Returns the maximum value in the specified array
     * @param array
     * @return
     */
    public static int max(int[] array) {
        int max = Integer.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    /**
     * Returns the maximum value in the specified array
     * @param array
     * @return
     */
    public static double max(double[] array) {
        double max = Double.MIN_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] > max) {
                max = array[i];
            }
        }
        return max;
    }

    /**
     * Returns a new array containing the items specified from
     * the source array by the indexes specified.
     *
     * @param source
     * @param indexes
     * @return
     */
    public static double[] sub(double[] source, int[] indexes) {
        double[] retVal = new double[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            retVal[i] = source[indexes[i]];
        }
        return retVal;
    }

    /**
     * Returns a new array containing the items specified from
     * the source array by the indexes specified.
     *
     * @param source
     * @param indexes
     * @return
     */
    public static int[] sub(int[] source, int[] indexes) {
        int[] retVal = new int[indexes.length];
        for (int i = 0; i < indexes.length; i++) {
            retVal[i] = source[indexes[i]];
        }
        return retVal;
    }

    /**
     * Returns a new 2D array containing the items specified from
     * the source array by the indexes specified.
     *
     * @param source
     * @param indexes
     * @return
     */
    public static int[][] sub(int[][] source, int[] indexes) {
        int[][] retVal = new int[indexes.length][];
        for (int i = 0; i < indexes.length; i++) {
            retVal[i] = source[indexes[i]];
        }
        return retVal;
    }
    
    /**
     * Returns the minimum value in the specified array
     * @param array
     * @return
     */
    public static int min(int[] array) {
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    /**
     * Returns the minimum value in the specified array
     * @param array
     * @return
     */
    public static double min(double[] array) {
        double min = Double.MAX_VALUE;
        for (int i = 0; i < array.length; i++) {
            if (array[i] < min) {
                min = array[i];
            }
        }
        return min;
    }

    /**
     * Returns a copy of the specified integer array in
     * reverse order
     *
     * @param d
     * @return
     */
    public static int[] reverse(int[] d) {
        int[] ret = new int[d.length];
        for (int i = 0, j = d.length - 1; j >= 0; i++, j--) {
            ret[i] = d[j];
        }
        return ret;
    }

    /**
     * Returns a copy of the specified double array in
     * reverse order
     *
     * @param d
     * @return
     */
    public static double[] reverse(double[] d) {
        double[] ret = new double[d.length];
        for (int i = 0, j = d.length - 1; j >= 0; i++, j--) {
            ret[i] = d[j];
        }
        return ret;
    }

    /**
     * Returns a new int array containing the or'd on bits of
     * both arg1 and arg2.
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static int[] or(int[] arg1, int[] arg2) {
        int[] retVal = new int[Math.max(arg1.length, arg2.length)];
        for (int i = 0; i < arg1.length; i++) {
            retVal[i] = arg1[i] > 0 || arg2[i] > 0 ? 1 : 0;
        }
        return retVal;
    }

    /**
     * Returns a new int array containing the and'd bits of
     * both arg1 and arg2.
     *
     * @param arg1
     * @param arg2
     * @return
     */
    public static int[] and(int[] arg1, int[] arg2) {
        int[] retVal = new int[Math.max(arg1.length, arg2.length)];
        for (int i = 0; i < arg1.length; i++) {
            retVal[i] = arg1[i] > 0 && arg2[i] > 0 ? 1 : 0;
        }
        return retVal;
    }

    /**
     * Copies the passed array <tt>original</tt>  into a new array except first element and returns it
     *
     * @param original the array from which a tail is taken
     * @return a new array containing the tail from the original array
     */
    public static int[] tail(int[] original) {
        return Arrays.copyOfRange(original, 1, original.length);
    }

    /**
     * Set <tt></tt>value for <tt>array</tt> at specified position <tt>indexes</tt>
     *
     * @param array
     * @param value
     * @param indexes
     */
    public static void setValue(Object array, int value, int... indexes) {
        if (indexes.length == 1) {
            ((int[])array)[indexes[0]] = value;
        } else {
            setValue(Array.get(array, indexes[0]), value, tail(indexes));
        }
    }

    /**
     *Assigns the specified int value to each element of the specified any dimensional array
     * of ints.
     * @param array
     * @param value
     */
    public static void fillArray(Object array, int value) {
        if (array instanceof int[]) {
            Arrays.fill((int[])array, value);
        } else {
            for (Object agr : (Object[])array) {
                fillArray(agr, value);
            }
        }
    }

    /**
    * Aggregates all element of multi dimensional array of ints
    * @param array
    * @return sum of all array elements
    */
    public static int aggregateArray(Object array) {
        int sum = 0;
        if(array instanceof Integer){
            return (int)array;
        } else if (array instanceof int[]) {
            int[] set = (int[])array;
            for (int element : set) {
                sum += element;
            }
            return sum;
        } else {
            for (Object agr : (Object[])array) {
                sum += aggregateArray(agr);
            }
            return sum;
        }
    }

    /**
     * Convert multidimensional array to readable String
     * @param array
     * @return String representation of array
     */
    public static String intArrayToString(Object array){
        StringBuilder result = new StringBuilder();
        if(array instanceof Object[]){
            result.append(Arrays.deepToString((Object[]) array));
        } else {
            //One dimension
            result.append(Arrays.toString((int[])array));
        }
        return result.toString();
    }

    /**
     * Return True if all elements of the  <tt>values</tt> have evaluated to true with <tt>condition</tt>
     * @param values
     * @param condition
     * @param <T>
     * @return
     */
    public static <T> boolean  all(final int[]  values, final Condition<T> condition) {
        for (int element : values) {
            if (!condition.eval(element)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Concat arrays
     *
     * @return The concatenated array
     *
     * http://stackoverflow.com/a/784842
     */
    @SafeVarargs
    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest) {
            totalLength += array.length;
        }
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    /**
     * Concat int arrays
     *
     * @return The concatenated array
     *
     * http://stackoverflow.com/a/784842
     */
    @SafeVarargs
    public static int[] concatAll(int[] first, int[]... rest) {
        int totalLength = first.length;
        for (int[] array : rest) {
            totalLength += array.length;
        }
        int[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (int[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}
