package org.numenta.nupic.util;

import java.util.Arrays;

/**
 * Testing to see if this is more meaningful than a generic 
 * native java data structure for encoder work.
 * @author metaware
 *
 */
public class Tuple {
	private Object[] container;
	
	public Tuple(int size, Object... objects) {
		container = new Object[size];
		for(int i = 0;i < size;i++) container[i] = objects[i];
	}
	
	public Object get(int index) {
		return container[index];
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(int i = 0;i < container.length;i++) {
			try {
				new Double(Double.parseDouble(container[i] +""));
				sb.append(container[i]);
			}catch(Exception e) { sb.append("'").append(container[i]).append("'");}
			sb.append(":");
		}
		sb.setLength(sb.length() - 1);
		
		return sb.toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(container);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple other = (Tuple) obj;
		if (!Arrays.equals(container, other.container))
			return false;
		return true;
	}
}
