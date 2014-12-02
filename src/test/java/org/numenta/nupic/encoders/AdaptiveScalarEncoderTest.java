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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.encoders.ScalarEncoder.Builder;

/**
 * @author sambit
 *
 */
public class AdaptiveScalarEncoderTest {

	private AdaptiveScalarEncoder ase;
	private Builder builder;

	@Rule
	public ExpectedException exception = ExpectedException.none();

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		/*builder = AdaptiveScalarEncoder.builder()
				.n(14)
				.w(3)
				.minVal(1)
				.maxVal(8)
				.radius(1.5)
				.resolution(0.5)
				.periodic(false)
				.forced(true);*/
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}
	
	private void initASE() {
		/*ase = builder.build();*/
	}

	/**
	 * Test method for {@link org.numenta.nupic.encoders.AdaptiveScalarEncoder#AdaptiveScalarEncoder()}.
	 */
	@Test
	public void testAdaptiveScalarEncoder() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
	}

	/**
	 * Test method for
	 * {@link org.numenta.nupic.encoders.AdaptiveScalarEncoder#init()}.
	 */
	@Test
	public void testInitThrowsIllegalStateExceptionForW() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		exception.expect(IllegalStateException.class);
		adaptiveScalarEncoder.init();
	}

	@Test
	public void testInitThrowsIllegalStateExceptionForMaxVal() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		adaptiveScalarEncoder.setW(3);
		exception.expect(IllegalStateException.class);
		adaptiveScalarEncoder.init();
	}

	@Test
	public void testInitThrowsIllegalStateExceptionForN() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		adaptiveScalarEncoder.setW(3);
		adaptiveScalarEncoder.setMinVal(1);
		adaptiveScalarEncoder.setMaxVal(8);
		adaptiveScalarEncoder.setN(14);
		exception.expect(IllegalStateException.class);
		adaptiveScalarEncoder.init();
	}

	@Test
	public void testInitThrowsIllegalStateExceptionForRadius() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		adaptiveScalarEncoder.setW(3);
		adaptiveScalarEncoder.setMinVal(1);
		adaptiveScalarEncoder.setMaxVal(8);
		adaptiveScalarEncoder.setN(14);
		adaptiveScalarEncoder.setRadius(1.5);
		exception.expect(IllegalStateException.class);
		adaptiveScalarEncoder.init();
	}

	@Test
	public void testInitThrowsIllegalStateExceptionForInterval() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		adaptiveScalarEncoder.setW(3);
		adaptiveScalarEncoder.setMinVal(1);
		adaptiveScalarEncoder.setMaxVal(8);
		adaptiveScalarEncoder.setN(14);
		adaptiveScalarEncoder.setResolution(0.5);
		exception.expect(IllegalStateException.class);
		adaptiveScalarEncoder.init();
	}

	@Test
	public void testInit() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);
		adaptiveScalarEncoder.setW(3);
		adaptiveScalarEncoder.setMinVal(1);
		adaptiveScalarEncoder.setMaxVal(8);
		adaptiveScalarEncoder.setN(14);
		adaptiveScalarEncoder.setRadius(1.5);
		adaptiveScalarEncoder.setResolution(0.5);
		adaptiveScalarEncoder.setForced(true);
		adaptiveScalarEncoder.init();
	}

	/**
	 * Test method for
	 * {@link org.numenta.nupic.encoders.AdaptiveScalarEncoder#initEncoder(int, double, double, int, double, double)}
	 * .
	 */
	@Test
	public void testInitEncoder() {
		AdaptiveScalarEncoder adaptiveScalarEncoder = new AdaptiveScalarEncoder();
		adaptiveScalarEncoder.initEncoder(3, 1, 8, 14, 1.5, 0.5);
		Assert.assertNotNull("AdaptiveScalarEncoder class is null",
				adaptiveScalarEncoder);

	}
}
