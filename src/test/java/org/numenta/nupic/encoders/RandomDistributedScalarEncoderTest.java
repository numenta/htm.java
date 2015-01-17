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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.Test;

/**
 * @author Sean Connolly
 */
public class RandomDistributedScalarEncoderTest {

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

}
