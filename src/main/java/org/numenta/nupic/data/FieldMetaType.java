package org.numenta.nupic.data;

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
