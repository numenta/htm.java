package org.numenta.nupic.data;

import java.lang.reflect.Array;
import java.util.Arrays;

public abstract class SparseMatrix<T> {
	
	protected int[] dimensionMultiples;
	protected int[] dimensions;
	protected int numDimensions;
	
	protected boolean isColumnMajor;
	
	public SparseMatrix(int[] dimensions) {
		this(dimensions, false);
	}
	
	public SparseMatrix(int[] dimensions, boolean useColumnMajorOrdering) {
		this.dimensions = dimensions;
		this.numDimensions = dimensions.length;
		this.dimensionMultiples = initDimensionMultiples(
			useColumnMajorOrdering ? reverse(dimensions) : dimensions);
		isColumnMajor = useColumnMajorOrdering;
	}
	
	/**
	 * Returns the utility array which holds the multiples used
	 * to calculate indexes.
	 * 
	 * @return	the utility multiples array.
	 */
	public int[] getDimensionMultiples() {
		return dimensionMultiples;
	}
	
	/**
	 * Returns the array describing the dimensionality of the configured array.
	 * @return	the array describing the dimensionality of the configured array.
	 */
	public int[] getDimensions() {
		return dimensions;
	}
	
	/**
	 * Returns the configured number of dimensions.
	 * 
	 * @return	the configured number of dimensions.
	 */
	public int getNumDimensions() {
		return numDimensions;
	}
	
	/**
	 * Sets the object to occupy the specified index.
	 * 
	 * @param index		the index the object will occupy
	 * @param object	the object to be indexed.
	 */
	protected void set(int index, T object) {}
	
	/**
	 * Sets the object to occupy the specified index.
	 * 
	 * @param index		the index the object will occupy
	 * @param value		the value to be indexed.
	 */
	protected void set(int index, int value) {}
	
	/**
	 * Sets the object to occupy the specified index.
	 * 
	 * @param index		the index the object will occupy
	 * @param value		the value to be indexed.
	 */
	protected void set(int index, double value) {}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param object		the object to be indexed.
	 */
	protected void set(int[] coordinates, T object) {}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param value			the value to be indexed.
	 */
	protected void set(int[] coordinates, int value) {}
	
	/**
	 * Sets the specified object to be indexed at the index
	 * computed from the specified coordinates.
	 * 
	 * @param coordinates	the row major coordinates [outer --> ,...,..., inner]
	 * @param value			the value to be indexed.
	 */
	protected void set(int[] coordinates, double value) {}
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	protected T getObject(int index) { return null; }
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	protected int getIntValue(int index) { return -1; }
	
	/**
	 * Returns the T at the specified index.
	 * 
	 * @param index		the index of the T to return
	 * @return	the T at the specified index.
	 */
	protected double getDoubleValue(int index) { return -1.0; }
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	protected T get(int[] coordinates) { return null; }
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	protected int getIntValue(int[] coordinates) { return -1; }
	
	/**
	 * Returns the T at the index computed from the specified coordinates
	 * @param coordinates	the coordinates from which to retrieve the indexed object
	 * @return	the indexed object
	 */
	protected double getDoubleValue(int[] coordinates) { return -1.0; }
	
	/**
	 * Returns a sorted array of occupied indexes.
	 * @return	a sorted array of occupied indexes.
	 */
	public int[] getSparseIndices() { return null; }
	
	/**
	 * Returns the maximum accessible flat index.
	 * @return	the maximum accessible flat index.
	 */
	public int getMaxIndex() {
		return dimensions[0] * dimensionMultiples[0] - 1;
	}
	
	/**
	 * Uses the specified {@link TypeFactory} to return an array
	 * filled with the specified object type, according this {@code SparseMatrix}'s 
	 * configured dimensions
	 * 
	 * @param factory	a factory to make a specific type
	 * @return	the dense array
	 */
	@SuppressWarnings("unchecked")
	public T[] asDense(TypeFactory<T> factory) {
		T[] retVal = (T[])Array.newInstance(factory.typeClass(), 2, 4, 8);
		fill(factory, 0, dimensions, dimensions[0], retVal);
		
		return (T[])retVal;
	}
	
	/**
	 * Uses reflection to create and fill a dynamically created multidimensional array.
	 * 
	 * @param f					the {@link TypeFactory}
	 * @param dimensionIndex	the current index into <em>this class's</em> configured dimensions array
	 * 							<em>*NOT*</em> the dimensions used as this method's argument	
	 * @param dimensions		the array specifying remaining dimensions to create
	 * @param count				the current dimensional size
	 * @param arr				the array to fill
	 * @return a dynamically created multidimensional array
	 */
	@SuppressWarnings("unchecked")
	protected Object[] fill(TypeFactory<T> f, int dimensionIndex, int[] dimensions, int count, Object[] arr) {
		if(dimensions.length == 1) {
			for(int i = 0;i < count;i++) {
				arr[i] = f.make(this.dimensions);
			}
			return arr;
		}else{
			for(int i = 0;i < count;i++) {
				int[] inner = copyInnerArray(dimensions);
				T[] r = (T[])Array.newInstance(f.typeClass(), inner);
				arr[i] = (Object[])fill(f, dimensionIndex + 1, inner, this.dimensions[dimensionIndex + 1], r);
			}
			return (T[])arr;
		}
	}
	
	/**
	 * Utility method to shrink a single dimension array by one index.
	 * @param array	the array to shrink
	 * @return
	 */
	protected int[] copyInnerArray(int[] array) {
		if(array.length == 1) return array;
		
		int[] retVal = new int[array.length - 1];
		for(int i = 1;i < array.length;i++) {
			retVal[i - 1] = array[i];
		}
		return retVal;
	}
	
	/**
	 * Reverses the specified array.
	 * @param input
	 * @return
	 */
	public static int[] reverse(int[] input) {
		int[] retVal = new int[input.length];
		for(int i = input.length - 1, j = 0;i >= 0;i--, j++) {
			retVal[j] = input[i];
		}
		return retVal;
	}
	
	/**
	 * Initializes internal helper array which is used for multidimensional
	 * index computation.
	 * 
	 * @param dimensions
	 * @return
	 */
	protected int[] initDimensionMultiples(int[] dimensions) {
		int holder = 1;
		int len = dimensions.length;
		int[] dimensionMultiples = new int[numDimensions];
		for(int i = 0;i < len;i++) {
			holder *= (i == 0 ? 1 : dimensions[len - i]);
			dimensionMultiples[len - 1 - i] = holder;
		}
		return dimensionMultiples;
	}
	
	/**
	 * Assumes row-major ordering. For a 3 dimensional array, the
	 * indexing expected is [depth, height, width] or [slice, row, column].
	 * 
	 * @param coordinates
	 * @return
	 */
	public int computeIndex(int[] coordinates) {
		checkDims(coordinates);
		
		int[] localMults = isColumnMajor ? reverse(dimensionMultiples) : dimensionMultiples;
		int base = 0;
		for(int i = 0;i < coordinates.length;i++) {
			base += (localMults[i] * coordinates[i]);
		}
		return base;
	}
	
	/**
	 * Returns an integer array representing the coordinates of the specified index
	 * in terms of the configuration of this {@code SparseMatrix}.
	 * 
	 * @param index		the flat index to be returned as coordinates
	 * @return
	 */
	public int[] computeCoordinates(int index) {
		int[] returnVal = new int[numDimensions];
		int base = index;
		for(int i = 0;i < dimensionMultiples.length;i++) {
			int quotient = base / dimensionMultiples[i];
			base %= dimensionMultiples[i];
			returnVal[i] = quotient;
		}
		return isColumnMajor ? reverse(returnVal) : returnVal;
	}
	
	/**
	 * Checks the indexes specified to see whether they are within the
	 * configured bounds and size parameters of this array configuration.
	 * 
	 * @param index	the array dimensions to check
	 */
	protected void checkDims(int[] index) {
		if(index.length != numDimensions) {
			throw new IllegalArgumentException("Specified coordinates exceed the configured array dimensions " +
			   "input dimensions: " + index.length + " > number of configured dimensions: " + numDimensions);
		}
		for(int i = 0;i < index.length;i++) {
			if(index[i] >= dimensions[i]) {
				throw new IllegalArgumentException("Specified coordinates exceed the configured array dimensions " +
					print1DArray(index) + " > " + print1DArray(dimensions));
			}
		}
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
}
