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

import java.util.Arrays;

/**
 * Tuple to represent the results of computations in different forms.
 *
 * @author metaware
 * @see {@link Encoder}
 */
public class Encoding extends Tuple {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code EncoderResult}
     *
     * @param value    A representation of the encoded value in the same format as the input
     *                 (i.e. float for scalars, string for categories)
     * @param scalar   A representation of the encoded value as a number. All encoded values
     *                 are represented as some form of numeric value before being encoded
     *                 (e.g. for categories, this is the internal index used by the encoder)
     * @param encoding The bit-string representation of the value
     */
    public Encoding(Object value, Number scalar, int[] encoding) {
        super("EncoderResult", value, scalar, encoding);
    }

    @Override
    public String toString() {
        return new StringBuilder("EncoderResult(value=").
                append(get(1)).append(", scalar=").append(get(2)).
                append(", encoding=").append(get(3)).toString();
    }

    /**
     * Returns a representation of the encoded value in the same format as the input.
     *
     * @return the encoded value
     */
    public Object getValue() {
        return get(1);
    }

    /**
     * Returns the encoded value as a number.
     *
     * @return
     */
    public Number getScalar() {
        return (Number)get(2);
    }

    /**
     * Returns the bit-string encoding of the value
     *
     * @return
     */
    public int[] getEncoding() {
        return (int[])get(3);
    }

    @Override public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Encoding)) {
            return false;
        }
        Encoding other = (Encoding)obj;
        if (!this.getScalar().equals(other.getScalar())) {
            return false;
        }
        if (!this.getValue().equals(other.getValue())) {
            return false;
        }
        if(!Arrays.equals(this.getEncoding(), other.getEncoding())){
            return false;
        }
        return true;
    }
}
