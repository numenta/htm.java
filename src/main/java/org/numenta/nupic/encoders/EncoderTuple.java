package org.numenta.nupic.encoders;

import org.numenta.nupic.util.Tuple;

/**
 * Subclass of {@link Tuple} specialized to hold the 3-value contents 
 * of an "encoder tuple". Each {@code EncoderTuple} holds a name, encoder and offset
 * in that order. Also, every EncoderTuple's size == 3.
 * 
 * @author David Ray
 * @see Tuple
 */
public class EncoderTuple extends Tuple {
	/**
	 * Constructs a new {@code EncoderTuple}
	 * 
	 * @param name		the {@link Encoder}'s name
	 * @param e			the {@link Encoder}
	 * @param offset	the offset within the input (first on bit) that this 
	 * 					encoder encodes/decodes. (see  {@link ScalarEncoder#getFirstOnBit(
	 * 						org.numenta.nupic.research.Connections, double)})
	 */
	public EncoderTuple(String name, Encoder e, int offset) {
		super(3, name, e, offset);
		if(name == null) throw new IllegalArgumentException("Can't instantiate an EncoderTuple " +
			" with a null Name");
		if(e == null) throw new IllegalArgumentException("Can't instantiate an EncoderTuple " +
		    " with a null Encoder");
	}
	
	/**
	 * Returns the {@link Encoder}'s name
	 * @return
	 */
	public String getName() {
		return (String)get(0);
	}
	
	/**
	 * Returns this {@link Encoder}
	 * @return
	 */
	public Encoder getEncoder() {
		return (Encoder)get(1);
	}
	
	/**
	 * Returns the index of the first on bit (offset)
	 * the {@link Encoder} encodes.
	 * @return
	 */
	public int getOffset() {
		return (Integer)get(2);
	}
}
