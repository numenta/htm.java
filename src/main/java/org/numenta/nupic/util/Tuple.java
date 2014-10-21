package org.numenta.nupic.util;

/**
 * Testing to see if this is more meaningful than a generic 
 * native java data structure for encoder work.
 * @author metaware
 *
 */
public abstract class Tuple {
	private Object[] container;
	
	protected Tuple(int size, Object... objects) {
		container = new Object[size];
		for(int i = 0;i < size;i++) container[i] = objects[i];
	}
	
	public final Object get(int index) {
		return container[index];
	}
}
