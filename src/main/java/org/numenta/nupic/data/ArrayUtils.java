package org.numenta.nupic.data;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.hash.TIntHashSet;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
		for(int i = lowerBounds;i < upperBounds;i++) {
			ints.add(i);
		}
		return ints.toArray();
	}
	
	/**
	 * Returns a sorted unique array of integers
	 * 
	 * @param nums	an unsorted array of integers with possible duplicates.
	 * @return
	 */
	public static int[] unique(int[] nums) {
		TIntHashSet set = new TIntHashSet(nums);
		int[] result = set.toArray();
		Arrays.sort(result);
		return result;
	}
	
	/**
	 * Called to merge a list of dimension arrays into a sequential row-major indexed
	 * list of coordinates.
	 * 
	 * @param dimensions	a list of dimension arrays, each array being a dimension 
	 * 						of an n-dimensional array.
	 * @return	a list of n-dimensional coordinates in row-major format.
	 */
	public static List<TIntList> dimensionsToCoordinateList(List<int[]> dimensions) {
		int[] depthIndexes = new int[dimensions.size()];
		for(int i = 0;i < dimensions.size();i++) {
			depthIndexes[i] = 0;
		}
		List<TIntList> retVal = new ArrayList<TIntList>();
		recurse(0, dimensions.size(), depthIndexes, new TIntArrayList(), dimensions, retVal);
		
		for(int i = 0;i < retVal.size();i++) {
			System.out.println(ArrayUtils.print1DArray(retVal.get(i)));
		}
		
		return retVal;
	}
	
	/**
	 * Takes a list of arrays; each array specifying a dimension of an n-dimensional array and merges
	 * them into a list of row-major indexed coordinates (the right most index incrementing the fastest).
	 * 
	 * This method recursively calls itself to step through the depth specified by each dimension and
	 * enter the index at that depth in the list specified as "resultList". The resultList is sequentially
	 * populated with the incrementing coordinates and holds the final list of coordinates - the returned
	 * value from this method is not meaningful to the caller due to the final results being populated
	 * within the "resultList".
	 * 
	 * @param depth					the current dimension depth
	 * @param maxDepth				the maximum number of dimensions to the array
	 * @param depthIndexes			an array holding the current index at the depth implied by that index's position.
	 * @param coords				a list of coordinates at a position in the sequence of coordinates
	 * @param dimensionIndexes		the array expressing the indexes of a given dimension
	 * @param resultList			the container of the final list of coordinates
	 * @return						meaningless for the caller but is of interim significance during the recursion
	 */
	public static TIntList recurse(int depth, int maxDepth, int[] depthIndexes, 
		TIntList coords, List<int[]> dimensionIndexes, List<TIntList> resultList) {
		
		//Return null if we've added all indexes for each dimension
		if(depth < maxDepth) {
			//Add the index coordinate at the current depth
			coords.add(dimensionIndexes.get(depth)[depthIndexes[depth]]);
			//Go to the next dimensional coordinate and add that
			coords = recurse(depth + 1, maxDepth, depthIndexes, coords, dimensionIndexes, resultList);
			
			//Return until we're back at the top
			if(depth > 0) {
				return null;
			}else{
				//Adjust all depth indexes by incrementing to its max then wrapping to zero
				for(int i = depthIndexes.length - 1;i >= 0;i--) {
					if(depthIndexes[i] < dimensionIndexes.get(i).length - 1) {
						depthIndexes[i] += 1;
						break;
					}else{
						if(i == 0) return null; //if the leftmost index is at its max, we're done.
						depthIndexes[i] = 0;
					}
				}
				recurse(0, dimensionIndexes.size(), depthIndexes, new TIntArrayList(), dimensionIndexes, resultList);
				//We've finished so unwind.
				return null;
			}
		}
		//Add the coordinates to the result container
		resultList.add(coords);
		//Return null if we've added all indexes for each dimension
		return null;
	}
}
