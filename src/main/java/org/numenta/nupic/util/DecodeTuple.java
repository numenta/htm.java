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

package org.numenta.nupic.util;

import java.util.List;
import java.util.Map;

import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.RangeList;

/**
 * Subclass of Tuple to specifically contain the results of an
 * {@link Encoder}'s {@link Encoder#encode(double)}
 * call.
 * 
 * @author David Ray
 *
 * @param <M>	the fieldsMap
 * @param <L>	the fieldsOrder
 */
public class DecodeTuple<M extends Map<String, RangeList>, L extends List<String>> extends Tuple {
	private static final long serialVersionUID = 1L;
    
	protected M fields;
	protected L fieldDescriptions;
	
	public DecodeTuple(M m, L l) {
		super(m, l);
		this.fields = m;
		this.fieldDescriptions = l;
	}
}
