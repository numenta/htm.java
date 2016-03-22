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

import java.util.List;

import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.RangeTuple;
import org.numenta.nupic.util.Tuple;

/**
 * Convenience subclass of {@link Tuple} to contain the list of
 * ranges expressed for a particular decoded output of an
 * {@link Encoder} by using tightly constrained types without 
 * the verbosity at the instantiation site.
 * 
 * @author David Ray
 */
public class RangeList extends RangeTuple<List<MinMax>, String>{

	private static final long serialVersionUID = 1L;

    /**
	 * Constructs and new {@code Ranges} object.
	 * @param l		the {@link List} of {@link MinMax} objects which are the 
	 * 				minimum and maximum positions of 1's
	 * @param s
	 */
	public RangeList(List<MinMax> l, String s) {
		super(l, s);
	}

	/**
	 * Returns a List of the {@link MinMax}es.
	 * @return
	 */
	public List<MinMax> getRanges() {
		return l;
	}
	
	/**
	 * Returns a comma-separated String containing the descriptions
	 * for all of the {@link MinMax}es
	 * @return
	 */
	public String getDescription() {
		return desc;
	}
	
	/**
	 * Adds a {@link MinMax} to this list of ranges
	 * @param mm
	 */
	public void add(MinMax mm) {
		l.add(mm);
	}
	
	/**
	 * Returns the specified {@link MinMax} 
	 * 	
	 * @param index		the index of the MinMax to return
	 * @return			the specified {@link MinMax} 
	 */
	public MinMax getRange(int index) {
		return l.get(index);
	}
	
	/**
	 * Sets the entire comma-separated description string
	 * @param s
	 */
	public void setDescription(String s) {
		this.desc = s;
	}
	 
	/**
	 * Returns the count of ranges contained in this Ranges object
	 * @return
	 */
	public int size() {
		return l.size();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return l.toString();
	}
}
