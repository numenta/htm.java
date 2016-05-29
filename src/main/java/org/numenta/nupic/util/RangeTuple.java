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

import org.numenta.nupic.encoders.Encoder;

/**
 * Subclasses the {@link Tuple} utility class to constrain 
 * the number of arguments and argument types to those specifically
 * related to the {@link Encoder} functionality.
 * 
 * @author David Ray
 *
 * @param <L>
 * @param <S>
 */
public class RangeTuple<L extends List<MinMax>, S> extends Tuple {
	
    private static final long serialVersionUID = 1L;
    
    protected L l;
	protected String desc;
	
	/**
	 * Instantiates a {@code RangeTuple}
	 * @param l
	 * @param s
	 */
	public RangeTuple(L l, String s) {
		super(l, s);
		this.l = l;
		this.desc = s;
	}
}
