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
package org.numenta.nupic.network.sensor;

import org.numenta.nupic.FieldMetaType;

/**
 * <p>
 * Designates field type and processing information. One of the 3 row 
 * information types such as Field Name, {@link FieldMetaType} and these
 * {@code SensorFlags}.
 * </p>
 * <p>
 * <ul>
 *      <li><b>R</b> - <em>"reset"</em> -       Specify that a reset should be inserted into the 
 *                                              model when this field evaluates to true. This is 
 *                                              used to manually insert resets. "1" equals reset,
 *                                              "0" equals no reset.
 *                              
 *      <li><b>S</b> - <em>"sequence"</em> -    Specify that a reset should be inserted into the 
 *                                              model when this field changes. This is used when 
 *                                              you have a field that identifies sequences and 
 *                                              you want to insert resets between each sequence.
 *                        
 *      <li><b>T</b> - <em>"timestamp"</em> -   This identifies a date/time field that should be 
 *                                              used as the timestamp for aggregation and other 
 *                                              time-related functions.
 *                              
 *      <li><b>C</b> - <em>"category"</em> -    This indicates that the category encoder should be used.
 *      
 *      <li><b>L</b> - <em>"learn"</em> -       If "1" then learn, if "0" then stop learning.   
 *      
 *      <li><b>B</b> - <em>"blank"</em> -       Blank meaning do nothing (space filler)
 * </ul>
 * 
 * 
 * @author David Ray
 */
public enum SensorFlags {
    R("reset"), S("sequence"), T("timestamp"), C("category"), L("learn"), B("blank");
    
    /** Flag description */
    private String description;
    
    private SensorFlags(String desc) {
        this.description = desc;
    }
    
    /**
     * Returns the description associated with a particular flag.
     * 
     * @return
     */
    public String description() {
        return description;
    }
    
    /**
     * Returns the flag indicator which specifies special processing 
     * or hints. (see this class' doc)
     * 
     * @param o
     * @return
     */
    public static SensorFlags fromString(Object o) {
        String val = o.toString().toLowerCase();
        switch(val) {
            case "r" : return R;
            case "s" : return S;
            case "t" : return T;
            case "c" : return C;
            case "l" : return L;
            default : return B;
        }
    }
}
