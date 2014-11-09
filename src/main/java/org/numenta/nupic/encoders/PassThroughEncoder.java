package org.numenta.nupic.encoders;

import java.util.List;

import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;
/**
 * 
 * @author wilsondy
 *
 */
public class PassThroughEncoder extends Encoder {
	//total #bits in output (must equal input bits * multiply)
	
	/**
	 * used to normalize the sparsity of the output, exactly w bits ON,
     * if Null (default) - do not alter the input, just pass it further.
	 */
	private Integer outputBitsOnCount;

	public PassThroughEncoder(int outputWidth, Integer outputBitsOnCount) {
		super.setW(outputWidth);
		super.setForced(false);
		this.outputBitsOnCount = outputBitsOnCount;
	}

	@Override
	public int getWidth() {
		return w;
	}

	@Override
	public boolean isDelta() {
		return false;
	}

	@Override
	public int[] encodeIntoArray(double inputData, int[] output) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] encodeIntoArray(String inputData, int[] output) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * TODO Why have the output parameter and a return value?
	 * @param input
	 * @param output
	 * @return
	 */
	public int[] encodeIntoArray(int[] input, int[] output){

		if( input.length != output.length)
			throw new IllegalArgumentException(String.format("Different input (%i) and output (%i) sizes", input.length, output.length));
		if(this.outputBitsOnCount != null && ArrayUtils.sum(input) != outputBitsOnCount)
			throw new IllegalArgumentException(String.format("Input has %i bits but w was set to %i.",  ArrayUtils.sum(input), outputBitsOnCount));
		
		System.arraycopy(input, 0, output, 0, input.length);

		return output;
	}

	@Override
	public void setLearning(boolean learningEnabled) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public List<Tuple> getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> List<T> getBucketValues(Class<T> returnType) {
		// TODO Auto-generated method stub
		return null;
	}

}
