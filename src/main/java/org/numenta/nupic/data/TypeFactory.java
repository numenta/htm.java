package org.numenta.nupic.data;

public interface TypeFactory<T> {
	public T make(int... args);
	public Class<T> typeClass();
}
