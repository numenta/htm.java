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

package org.numenta.nupic;

import org.numenta.nupic.encoders.CoordinateEncoder;
import org.numenta.nupic.encoders.DateEncoder;
import org.numenta.nupic.encoders.Encoder;
import org.numenta.nupic.encoders.GeospatialCoordinateEncoder;
import org.numenta.nupic.encoders.RandomDistributedScalarEncoder;
import org.numenta.nupic.encoders.SDRCategoryEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;

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
	LIST("list"),
	COORD("coord"),
	GEO("geo");
	
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
	 * Returns the {@link Encoder} matching this field type.
	 * @return
	 */
	public Encoder<?> newEncoder() {
	    switch(this) {
	        case LIST :
	        case STRING : return SDRCategoryEncoder.builder().build();
	        case DATETIME : return DateEncoder.builder().build();
	        case BOOLEAN : return ScalarEncoder.builder().build();
	        case COORD : return CoordinateEncoder.builder().build();
	        case GEO : return GeospatialCoordinateEncoder.geobuilder().build();
	        case INTEGER : 
	        case FLOAT : return RandomDistributedScalarEncoder.builder().build();
	        default : return null;
	    }
	}
	
	/**
	 * Returns the input type for the {@code FieldMetaType} that this is...
	 * @param input
	 * @param enc
	 * @return
	 */
	@SuppressWarnings("unchecked")
    public <T> T decodeType(String input, Encoder<?> enc) {
	    switch(this) {
            case LIST : 
            case STRING : return (T)input;
            case DATETIME : return (T)((DateEncoder)enc).parse(input);
            case BOOLEAN : return (T)(Boolean.valueOf(input) == true ? new Integer(1) : new Integer(0));
            case COORD : 
            case GEO : return (T)new double[] { Double.parseDouble(input.split("\\;")[0]), Double.parseDouble(input.split("\\;")[1]) }; 
            case INTEGER : 
            case FLOAT : return (T)new Double(input);
            default : return null;
        }
	}
	
	/**
	 * Returns the display string
	 * @return the display string
	 */
	public String display() {
		return displayString;
	}
	
	/**
	 * Parses the specified String and returns a {@link FieldMetaType}
	 * representing the passed in value.
	 * 
	 * @param s  the type in string form
	 * @return the FieldMetaType indicated or the default: {@link FieldMetaType#FLOAT}.
	 */
	public static FieldMetaType fromString(Object s) {
	    String val = s.toString().toLowerCase();
	    switch(val) {
	        case "char" : 
	        case "string" :
	        case "category" : {
	            return STRING;
	        }
	        case "date" :
	        case "date time" :
	        case "datetime" :
	        case "time" : {
	            return DATETIME;
	        }
	        case "int" :
	        case "integer" :
	        case "long" : {
	            return INTEGER;
	        }
	        case "double" :
	        case "float" :
	        case "number" :
	        case "numeral" :
	        case "num" :
	        case "scalar" :
	        case "floating point" : {
	            return FLOAT;
	        }
	        case "bool" :
	        case "boolean" : {
	            return BOOLEAN;
	        }
	        case "list" : {
	            return LIST;
	        }
	        case "geo" : {
                return GEO;
            }
	        case "coord" : {
                return COORD;
            }
	        default : return FLOAT;
	    }
	}
}
