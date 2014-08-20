package org.numenta.nupic.data;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Utilities to match some of the functionality found in Python's Numpy.
 * @author David Ray
 */
public class ArrayUtils {
	/**
	 * Returns an array with the same shape and the contents
	 * converted to doubles.
	 * 
	 * @param ints	an array of ints.
	 * @return
	 */
	public static double[] toDoubleArray(int[] ints) {
		double[] retVal = new double[ints.length];
		for(int i = 0;i < ints.length;i++) {
			retVal[i] = ints[i];
		}
		return retVal;
	}
	
	/**
	 * Returns an array with the same shape and the contents
	 * converted to doubles.
	 * 
	 * @param ints	an array of ints.
	 * @return
	 */
	public static int[] toIntArray(double[] doubs) {
		int[] retVal = new int[doubs.length];
		for(int i = 0;i < doubs.length;i++) {
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
		for(int i = 0;i < doubs.length;i++) {
			retVal[i] = Math.max(doubs[i], maxValue);
		}
		return retVal;
	}
	
	/**
	 * Returns an array whose members are the quotient of the dividend array
	 * values and the divisor array values.
	 * 
	 * @param dividend
	 * @param divisor
	 * @param dividend adjustment
	 * @param divisor adjustment
	 * 
	 * @return
	 * @throws	IllegalArgumentException 	if the two argument arrays are not the same length
	 */
	public static double[] divide(double[] dividend, double[] divisor, 
		double dividendAdjustment, double divisorAdjustment) {
		
		if(dividend.length != divisor.length) {
			throw new IllegalArgumentException(
				"The dividend array and the divisor array must be the same length");
		}
		double[] quotient = new double[dividend.length];
		for(int i = 0;i < dividend.length;i++) {
			quotient[i] = (dividend[i] + dividendAdjustment) / (divisor[i] + divisorAdjustment);
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
	 * @param factor adjustment
	 * 
	 * @return
	 * @throws	IllegalArgumentException 	if the two argument arrays are not the same length
	 */
	public static double[] multiply(double[] multiplicand, double[] factor, 
		double multiplicandAdjustment, double factorAdjustment) {
		
		if(multiplicand.length != factor.length) {
			throw new IllegalArgumentException(
				"The multiplicand array and the factor array must be the same length");
		}
		double[] product = new double[multiplicand.length];
		for(int i = 0;i < multiplicand.length;i++) {
			product[i] = (multiplicand[i] + multiplicandAdjustment) * (factor[i] + factorAdjustment);
		}
		return product;
	}
	
	/**
	 * Prints the specified array to a returned String.
	 * 
	 * @param aObject	the array object to print.
	 * @return	the array in string form suitable for display.
	 */
	public static String print1DArray(Object aObject) {
	    if (aObject.getClass().isArray()) {
	        if (aObject instanceof Object[]) // can we cast to Object[]
	            return Arrays.toString((Object[]) aObject);
	        else {  // we can't cast to Object[] - case of primitive arrays
	            int length = Array.getLength(aObject);
	            Object[] objArr = new Object[length];
	            for (int i=0; i<length; i++)
	                objArr[i] =  Array.get(aObject, i);
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
}
