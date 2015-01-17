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
package org.numenta.nupic.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for {@link RandomDistributedScalarEncoder}.
 *
 * @author Sean Connolly
 */
public class RandomDistributedScalarEncoderTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * Test basic encoding functionality.<br/>
	 * Create encodings without crashing and check they contain the correct
	 * number of on and off bits. Check some encodings for expected overlap.
	 * Test that encodings for old values don't change once we generate new
	 * buckets.
	 */
	@Test
	public void testEncoding() {
		// Initialize with non-default parameters
		RandomDistributedScalarEncoder enc = RandomDistributedScalarEncoder
				.builder().name("enc").resolution(1.0).w(23).n(500).offset(0.0)
				.build();
		// Encode with a number close to the offset
		int[] e0 = enc.encode(-0.1);
		assertEquals("Number of on bits is incorrect", 23, sum(e0));
		assertEquals("Width of the vector is incorrect", 500, e0.length);
		assertEquals("Offset doesn't correspond to middle bucket", 1000 / 2,
				enc.getBucketIndices(0.0)[0]);
		assertEquals("Number of buckets is not 1", 1, enc.getBucketMap().size());
		// Encode with a number that is resolution away from offset. Now we
		// should have two buckets and this encoding should be one bit away from
		// e0
		int[] e1 = enc.encode(1.0);
		assertEquals("Number of buckets is not 2", 2, enc.getBucketMap().size());
		assertEquals("Number of on bits is incorrect", 23, sum(e1));
		assertEquals("Width of the vector is incorrect", 500, e1.length);
		assertEquals("Overlap is not equal to w-1", 22, overlap(e0, e1));
		// Encode with a number that is resolution*w away from offset. Now we
		// should have many buckets and this encoding should have very little
		// overlap with e0
		int[] e25 = enc.encode(25.0);
		assertTrue("Number of buckets is not 2", enc.getBucketMap().size() > 23);
		assertEquals("Number of on bits is incorrect", 23, sum(e25));
		assertEquals("Width of the vector is incorrect", 500, e25.length);
		assertTrue("Overlap is too high", overlap(e0, e25) < 4);
		// Test encoding consistency. The encodings for previous numbers
		// shouldn't change even though we have added additional buckets
		assertEquals("Encodings are not consistent - they have changed after "
				+ "new buckets have been created", e0, enc.encode(-0.1));
		assertEquals("Encodings are not consistent - they have changed after "
				+ "new buckets have been created", e1, enc.encode(1.0));
	}

	/**
	 * Test that nothing is printed out when {@code verbosity = 0}.
	 */
	@Test
	public void testVerbosity() {
		// Given
		ByteArrayOutputStream outContent = new ByteArrayOutputStream();
		ByteArrayOutputStream errContent = new ByteArrayOutputStream();
		System.setOut(new PrintStream(outContent));
		System.setErr(new PrintStream(errContent));
		Encoder<Double> encoder = RandomDistributedScalarEncoder.builder()
				.name("mv").resolution(1.0).verbosity(0).build();
		int[] output = new int[encoder.getWidth()];
		// When
		encoder.encodeIntoArray(23.0, output);
		encoder.getBucketIndices(23.0);
		// Then
		assertEquals("zero verbosity doesn't lead to zero output", "",
				outContent.toString());
		assertEquals("zero verbosity doesn't lead to zero err output", "",
				errContent.toString());
	}

	/**
	 * Test that the {@link RandomDistributedScalarEncoder} only encodes
	 * {@code Double}.<br/>
	 * Note: This is well covered by our, ahem, statically typed compiler.
	 * Included here for completeness of consistency with the Python project.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testEncodeInvalidInputType() {
		// Given
		Encoder encoder = RandomDistributedScalarEncoder.builder().name("enc")
				.resolution(1.0).verbosity(0).build();
		// Then
		exception.expect(ClassCastException.class);
		exception
				.expectMessage("java.lang.String cannot be cast to java.lang.Double");
		// When
		encoder.encode("String");
	}

	/**
	 * Compute the sum of all on bits; equivelant to the number of on bits.
	 */
	private int sum(int[] x) {
		int s = 0;
		for (int next : x) {
			s += next;
		}
		return s;
	}

	/**
	 * Given two binary arrays, compute their overlap. The overlap is the number
	 * of bits where x[i] and y[i] are both 1
	 */
	private int overlap(int[] x, int[] y) {
		List<Integer> xList = new ArrayList<>(x.length);
		for (int next : x) {
			xList.add(next);
		}
		int overlap = 0;
		for (int next : y) {
			if (xList.contains(next)) {
				overlap++;
			}
		}
		return overlap;
	}

}
