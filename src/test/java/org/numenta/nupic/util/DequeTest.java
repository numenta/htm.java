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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;

import org.junit.Test;

public class DequeTest {

	@Test
	public void testConstruction() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
	}
	
	@Test
	public void testCapacity() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		deque.append(3);
		assertEquals(2, deque.size());
	}

	@Test
	public void testAppend() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		deque.append(3);
		assertEquals(2, deque.size());
		assertTrue(2 == deque.peekFirst());
		assertTrue(3 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void testInsert() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		deque.insert(1);
		deque.insert(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(2 == deque.peekFirst());
		assertTrue(1 == deque.peekLast());
		
		deque.insert(3);
		assertEquals(2, deque.size());
		assertTrue(3 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void testPushLast() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		Integer pushResult = null;
		pushResult = deque.pushLast(1);
		assertNull(pushResult);
		
		pushResult = deque.pushLast(2);
		assertNull(pushResult);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		pushResult = deque.pushLast(3);
		//Assert that the head item is removed and returned
		assertTrue(1 == pushResult);
		assertEquals(2, deque.size());
		assertTrue(2 == deque.peekFirst());
		assertTrue(3 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void testPushFirst() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		Integer pushResult = null;
		pushResult = deque.pushFirst(1);
		assertNull(pushResult);
		
		pushResult = deque.pushFirst(2);
		assertNull(pushResult);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(2 == deque.peekFirst());
		assertTrue(1 == deque.peekLast());
		
		pushResult = deque.pushFirst(3);
		//Assert that the tail item is removed and returned
		assertTrue(1 == pushResult);
		assertEquals(2, deque.size());
		assertTrue(3 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void testClear() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		deque.clear();
		assertEquals(2, deque.capacity());
		assertEquals(0, deque.size());
		assertNull(deque.takeFirst());
		assertNull(deque.takeLast());
	}
	
	@Test
	public void takeFirst() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		Integer f = deque.takeFirst();
		assertEquals(new Integer(1), f);
		assertEquals(1, deque.size());
		
		deque.insert(1);
		Integer l = deque.takeLast();
		assertEquals(new Integer(2), l);
		assertEquals(1, deque.size());
	}
	
	@Test
	public void takeLast() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		
		Integer f = deque.takeFirst();
		assertEquals(new Integer(1), f);
		assertEquals(1, deque.size());
		
		deque.insert(1);
		Integer l = deque.takeLast();
		assertEquals(new Integer(2), l);
		assertEquals(1, deque.size());
	}
	
	@Test
	public void peekFirst() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void peekLast() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertTrue(1 == deque.peekFirst());
		assertTrue(2 == deque.peekLast());
		assertEquals(deque.size(), deque.capacity());
	}
	
	@Test
	public void testResize() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertEquals(2, deque.size());
		
		deque.append(3);
		assertEquals(deque.size(), deque.capacity());
		assertEquals(2, deque.size());
		
		deque.resize(3);
		assertEquals(3, deque.capacity());
		assertEquals(2, deque.size());
		Integer result = deque.pushLast(4);
		assertNull(result);
		assertTrue(2 == deque.peekFirst());
		assertTrue(4 == deque.peekLast());
		assertEquals(3, deque.size());
	}
	
	@Test
	public void testIterator() {
		Deque<Integer> deque = new Deque<Integer>(2);
		assertEquals(2, deque.capacity());
		deque.append(1);
		deque.append(2);
		assertEquals(deque.size(), deque.capacity());
		assertEquals(2, deque.size());
		
		Iterator<Integer> i = deque.iterator();
		assertEquals(new Integer(1), i.next());
		assertEquals(new Integer(2), i.next());
		assertTrue(!i.hasNext());
	}
	
	@Test
	public void testHeadTail() {
	    Deque<Integer> deque = new Deque<Integer>(2);
	    deque.append(1);
        deque.append(2);
        assertEquals(1, (int)deque.head());
        assertEquals(2, (int)deque.tail());
	}
	
	@Test
	public void testHashCodeAndEquals() {
	    Deque<Integer> deque = new Deque<Integer>(2);
        deque.append(1);
        deque.append(2);
        
        Deque<Integer> deque2 = new Deque<Integer>(2);
        deque2.append(1);
        deque2.append(2);
        
        assertEquals(deque, deque2);
        assertEquals(deque.hashCode(), deque2.hashCode());
        
        Deque<Integer> deque3 = new Deque<Integer>(2);
        deque3.append(3);
        deque3.append(2);
        
        assertNotEquals(deque, deque3);
        assertNotEquals(deque.hashCode(), deque3.hashCode());
	}
}
