package org.numenta.nupic.encoders;

import org.numenta.nupic.util.Tuple;

/**
 * Tuple to represent the results of computations in different forms.
 * 
 * @author metaware
 * @see {@link Encoder}
 */
public class EncoderResult extends Tuple {
	
	/**
	 * Constructs a new {@code EncoderResult}
	 * 
	 * @param value			A representation of the encoded value in the same format as the input
     *     					(i.e. float for scalars, string for categories)
	 * @param scalar		A representation of the encoded value as a number. All encoded values
     *     				    are represented as some form of numeric value before being encoded
     *      					(e.g. for categories, this is the internal index used by the encoder)
	 * @param encoding		The bit-string representation of the value
	 */
	public EncoderResult(Object value, Number scalar, String encoding) {
		super(4, "EncoderResult", value, scalar, encoding);
	}
	
	@Override
	public String toString() {
		return new StringBuilder("EncoderResult(value=").
			append(get(1)).append(", scalar=").append(get(2)).
			append(", encoding=").append(get(3)).toString();
	}
}
