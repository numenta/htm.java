/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
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
	
    private static final long serialVersionUID = 1L;

    /**
	 * Constructs a new {@code EncoderTuple}
	 * 
	 * @param name		the {@link Encoder}'s name
	 * @param e			the {@link Encoder}
	 * @param offset	the offset within the input (first on bit) that this 
	 * 					encoder encodes/decodes. (see  {@link ScalarEncoder#getFirstOnBit(
	 * 						org.numenta.nupic.research.Connections, double)})
	 */
	public EncoderTuple(String name, Encoder<?> e, int offset) {
		super(name, e, offset);
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
	public Encoder<?> getEncoder() {
		return (Encoder<?>)get(1);
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
