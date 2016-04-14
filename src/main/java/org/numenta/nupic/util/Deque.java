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

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.LinkedBlockingDeque;

import com.cedarsoftware.util.DeepEquals;

/**
 * Double ended queue implementation which has a restricted capacity.
 * Operations may be conducted on both ends and when capacity is reached,
 * the next addition to either end will result in a removal on the opposite
 * end, thus always maintaining a size &lt;= initial size.
 * 
 * This behavior differs from the {@link LinkedBlockingDeque} implementation
 * of the Java Collections Framework, and is the reason for the development of this
 * "alternative" - by allowing constant mutation of this list without an exception
 * being thrown and forcing the client to handle capacity management logic. 
 * 
 * @author David Ray
 *
 * @param <E>
 */
public class Deque<E> implements Iterable<E>, Serializable {
	private static final long serialVersionUID = 1L;
    
	/** Backing array list */
	private LinkedBlockingDeque<E> backingList = new LinkedBlockingDeque<E>();
	/** Originating size of this {@code Deque} */
	private int capacity;
	/** The internal size monitor */
	private int currentSize;
	
	/**
	 * Constructs a new {@code Deque} with the specified capacity.
	 * @param capacity
	 */
	public Deque(int capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Appends the specified item to the end of this {@code Deque}
	 * 
	 * @param t		the object of type &lt;T&gt; to add
	 * @return		flag indicating whether capacity had been reached 
	 * 				<em><b>prior</b></em> to this call.
	 */
	public boolean append(E t) {
		boolean ret = currentSize == capacity;
		if(ret) {
			backingList.removeFirst();
			backingList.addLast(t);
		}else{
			backingList.addLast(t);
			currentSize++;
		}
		return ret;
	}
	
	/**
	 * Inserts the specified item at the head of this {@code Deque}
	 * 
	 * @param t		the object of type &lt;T&gt; to add
	 * @return		flag indicating whether capacity had been reached 
	 * 				<em><b>prior</b></em> to this call.
	 */
	public boolean insert(E t) {
		boolean ret = currentSize == capacity;
		if(ret) {
			backingList.removeLast();
			backingList.addFirst(t);
		}else{
			backingList.addFirst(t);
			currentSize++;
		}
		return ret;
	}
	
	/**
	 * Appends the specified item to the end of this {@code Deque},
	 * and if this deque was at capacity prior to this call, the object
	 * residing at the head of this queue is returned, otherwise null
	 * is returned
	 * 
	 * @param t		the object of type &lt;T&gt; to add
	 * @return		the object residing at the head of this queue is 
	 * 				returned if previously at capacity, otherwise null 
	 * 				is returned
	 */
	public E pushLast(E t) {
		E retVal = null;
		boolean ret = currentSize == capacity;
		if(ret) {
			retVal = backingList.removeFirst();
			backingList.addLast(t);
		}else{
			backingList.addLast(t);
			currentSize++;
		}
		return retVal;
	}
	
	/**
	 * Inserts the specified item at the head of this {@code Deque},
	 * and if this deque was at capacity prior to this call, the object
	 * residing at the tail of this queue is returned, otherwise null
	 * is returned
	 * 
	 * @param t		the object of type &lt;T&gt; to add
	 * @return		the object residing at the tail of this queue is 
	 * 				returned if previously at capacity, otherwise null 
	 * 				is returned
	 */
	public E pushFirst(E t) {
		E retVal = null;
		boolean ret = currentSize == capacity;
		if(ret) {
			retVal = backingList.removeLast();
			backingList.addFirst(t);
		}else{
			backingList.addFirst(t);
			currentSize++;
		}
		return retVal;
	}
	
	/**
	 * Clears this {@code Deque} of all contents
	 */
	public void clear() {
		backingList.clear();
		currentSize = 0;
	}
	
	/**
	 * Returns the item at the head of this {@code Deque} or null
	 * if it is empty. This call does not block if empty.
	 * 
	 * @return	item at the head of this {@code Deque} or null
	 * 			if it is empty.
	 */
	public E takeFirst() {
		if(currentSize == 0) return null;
		
		E val = null;
		try {
			val = backingList.takeFirst();
			currentSize--;
		}catch(Exception e) { e.printStackTrace(); }
		
		return val;
	}
	
	/**
	 * Returns the item at the tail of this {@code Deque} or null
	 * if it is empty. This call does not block if empty.
	 * 
	 * @return	item at the tail of this {@code Deque} or null
	 * 			if it is empty.
	 */
	public E takeLast() {
		if(currentSize == 0) return null;
		
		E val = null;
		try {
			val = backingList.takeLast();
			currentSize--;
		}catch(Exception e) { e.printStackTrace(); }
		
		return val;
	}
	
	/**
	 * Returns the item at the head of this {@code Deque}, blocking
	 * until an item is available.
	 * 
	 * @return	item at the tail of this {@code Deque}
	 */
	public E head() {
		E val = null;
		try {
			val = backingList.takeFirst();
			currentSize--;
		}catch(Exception e) { e.printStackTrace(); }
		
		return val;
	}
	
	/**
	 * Returns the item at the tail of this {@code Deque} or null
	 * if it is empty. This call does not block if empty.
	 * 
	 * @return	item at the tail of this {@code Deque} or null
	 * 			if it is empty.
	 */
	public E tail() {
		E val = null;
		try {
			val = backingList.takeLast();
			currentSize--;
		}catch(Exception e) { e.printStackTrace(); }
		
		return val;
	}
	
	/**
	 * Returns an array containing all of the elements in this deque, 
	 * in proper sequence; the runtime type of the returned array is 
	 * that of the specified array.
	 *  
	 * @param a		array indicating return type
	 * @return		the contents of this {@code Deque} in an array of
	 * 				type &lt;T&gt;
	 */
	public <T> T[] toArray(T[] a) {
		return backingList.toArray(a);
	}
	
	/**
	 * Returns the number of elements in this {@code Deque}
	 * @return
	 */
	public int size() {
		return currentSize;
	}
	
	/**
	 * Returns the capacity this {@code Deque} was last configured with
	 * @return
	 */
	public int capacity() {
		return capacity;
	}
	
	/**
	 * Resizes the capacity of this {@code Deque} to the capacity
	 * specified. 
	 * 
	 * @param newCapacity
	 * @throws IllegalArgumentException if the specified new capacity is less than
	 * the previous capacity
	 */
	public void resize(int newCapacity) {
		if(capacity == newCapacity) return;
		if(capacity > newCapacity) {
			throw new IllegalArgumentException("Cannot resize to less than " +
				"the original capacity: " + capacity + " > " + newCapacity);
		}
		
		this.capacity = newCapacity;
	}
	
	/**
	 * Retrieves, but does not remove, the first element of this deque, or 
	 * returns null if this deque is empty.
	 * 
	 * @return
	 */
	public E peekFirst() {
		return backingList.peekFirst();
	}
	
	/**
	 * Retrieves, but does not remove, the last element of this deque, or 
	 * returns null if this deque is empty.
	 * 
	 * @return
	 */
	public E peekLast() {
		return backingList.peekLast();
	}
	
	/**
	 * Returns an {@link Iterator} over the contents of this {@code Deque}
	 * @return
	 */
	public Iterator<E> iterator() {
		return backingList.iterator();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((backingList == null) ? 0 : DeepEquals.deepHashCode(backingList));
		result = prime * result + capacity;
		result = prime * result + currentSize;
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Deque<E> other = (Deque<E>) obj;
		if (capacity != other.capacity)
			return false;
		if (currentSize != other.currentSize)
			return false;
		if (backingList == null) {
			if (other.backingList != null)
				return false;
		} else if (!deepEquals(other))
			return false;
		
		return true;
	}
	
	private boolean deepEquals(Deque<E> other) {
		Iterator<E> otherIt = other.iterator();
		for(Iterator<E> it = iterator();it.hasNext();) {
			if(!otherIt.hasNext() || !it.next().equals(otherIt.next())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public String toString() {
		return backingList.toString() + " capacity: " + capacity;
	}
}
