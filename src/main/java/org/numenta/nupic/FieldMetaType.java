/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 3 as
 * published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 *
 * http://numenta.org/licenses/
 * ---------------------------------------------------------------------
 */

package org.numenta.nupic;

/**
 * Public values for the field data types
 * 
 * @author David Ray
 */
public enum FieldMetaType {
	STRING("string"), 
	DATETIME("datetime"),
	INTEGER("int"),
	FLOAT("float"),
	BOOLEAN("bool"),
	LIST("list");
	
	/**
	 * String representation to be used when a display
	 * String is required.
	 */
	private String displayString;
	
	/** Private constructor */
	private FieldMetaType(String s) {
		this.displayString = s;
	}
	
	/**
	 * Returns the display string
	 * @return the display string
	 */
	public String display() {
		return displayString;
	}
}
