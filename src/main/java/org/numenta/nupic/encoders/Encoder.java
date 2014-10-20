package org.numenta.nupic.encoders;

import org.numenta.nupic.research.Connections;

public abstract class Encoder {
	/** Value used to represent no data */
	public static final int[] SENTINEL_VALUE_FOR_MISSING_DATA = new int[0];
	
	
	/**
	 * Encodes inputData and puts the encoded value into the numpy output array,
     * which is a 1-D array of length returned by {@link Connections#getW()}.
	 *
     * Note: The numpy output array is reused, so clear it before updating it.
     * 
	 * @param c
	 * @param inputData Data to encode. This should be validated by the encoder.
     * @param output 1-D array of same length returned by {@link Connections#getW()}
     * 
	 * @return
	 */
	protected abstract int[] encodeIntoArray(Connections c, int[] inputData, int[] output);
	
	/**
	 * Convenience wrapper for {@link #encodeIntoArray(Connections, int[], int[])}
	 *  
     * @param c				the memory
	 * @param inputData		the input array
	 * @return	an array with the encoded representation of inputData
	 */
	public int[] encode(Connections c, int[] inputData) {
		int[] output = new int[c.getN()];
		encodeIntoArray(c, inputData, output);
		return output;
	}
}
