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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An immutable fixed data structure whose values are retrieved
 * via a given index. This data structure emulates multiple method
 * return values possible in Python.
 * 
 * @author David Ray
 */
public class Tuple {
    /** The internal container array */
	protected Object[] container;
	
	private int hashcode;
	
	/**
	 * Instantiates a new {@code Tuple}
	 * @param objects
	 */
	public Tuple(Object... objects) {
		container = new Object[objects.length];
		for(int i = 0;i < objects.length;i++) container[i] = objects[i];
		this.hashcode = hashCode();
	}
	
	/**
	 * Returns the object previously inserted into the
	 * specified index.
	 * 
	 * @param index    the index representing the insertion order.
	 * @return
	 */
	public Object get(int index) {
		return container[index];
	}
	
	/**
	 * Returns the number of items in this {@code Tuple}
	 * 
	 * @return
	 */
	public int size() {
	    return container.length;
	}
	
	/**
	 * Returns an <em>unmodifiable</em> view of the underlying data.
	 * @return
	 */
	public  List<Object> all() {
	    return Collections.unmodifiableList(Arrays.asList(container));
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < container.length;i++) {
			try {
				new Double((double) container[i]);
				sb.append(container[i]);
			}catch(Exception e) { sb.append("'").append(container[i]).append("'");}
			sb.append(":");
		}
		sb.setLength(sb.length() - 1);
		
		return sb.toString();
	}
	
	/**
     * {@inheritDoc}
     */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(container);
		return result;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (this.hashcode != other.hashcode)
			return false;
		return true;
	}
}
