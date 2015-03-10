package org.numenta.nupic.network;

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
 *                                              used to manually insert resets.
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
 * </ul>
 * </p>
 * 
 * @author David Ray
 */
public enum SensorFlags {
    R("reset"), S("sequence"), T("timestamp"), C("category"), B("blank");
    
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
            default : return B;
        }
    }
}
