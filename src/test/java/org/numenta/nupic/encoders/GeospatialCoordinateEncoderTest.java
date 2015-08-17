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

package org.numenta.nupic.encoders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Tuple;

public class GeospatialCoordinateEncoderTest {
	private GeospatialCoordinateEncoder ge;
	private GeospatialCoordinateEncoder.Builder builder;
	
	private void setUp() {
		builder = GeospatialCoordinateEncoder.geobuilder()
			.name("coordinate")
			.n(33)
			.w(3);
	}
	
	private void initGE() {
		ge = builder.build();
	}
	
	@Test
	public void testCoordinateForPosition() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60);
		initGE();
		
		double[] coords = new double[] { -122.229194, 37.486782 };
		int[] coordinate = ge.coordinateForPosition(coords[0], coords[1]);
		
		assertTrue(Arrays.equals(new int[] { -453549, 150239 }, coordinate));
	}
	
	@Test
	public void testCoordinateForPositionOrigin() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		initGE();
		
		double[] coords = new double[] { 0, 0 };
		int[] coordinate = ge.coordinateForPosition(coords[0], coords[1]);
		
		assertTrue(Arrays.equals(new int[] { 0, 0 }, coordinate));
	}
	
	@Test
	public void testRadiusForSpeed() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		initGE();
		
		double speed = 50;//meters per second
		double radius = ge.radiusForSpeed(speed);
		assertEquals(radius, 75, 0.1);
	}
	
	@Test
	public void testRadiusForSpeed0() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		builder.n(999);
		builder.w(27);
		initGE();
		
		double speed = 0;//meters per second
		double radius = ge.radiusForSpeed(speed);
		assertEquals(radius, 3, 0.1);
	}
	
	@Test
	public void testRadiusForSpeedInt() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		initGE();
		
		double speed = 25;//meters per second
		double radius = ge.radiusForSpeed(speed);
		assertEquals(radius, 38, 0.1);
	}
	
	@Test
	public void testEncodeIntoArray() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		builder.n(999);
		builder.w(25);
		initGE();
		
		double speed = 2.5;//meters per second
		
		int[] encoding1 = encode(ge, new double[] { -122.229194, 37.486782 }, speed);
		int[] encoding2 = encode(ge, new double[] { -122.229294, 37.486882 }, speed);
		int[] encoding3 = encode(ge, new double[] { -122.229294, 37.486982 }, speed);
		
		double overlap1 = overlap(encoding1, encoding2);
		double overlap2 = overlap(encoding1, encoding3);
		
		assertTrue(overlap1 > overlap2);
	}
	
	public int[] encode(CoordinateEncoder encoder, double[] coordinate, double radius) {
		int[] output = new int[encoder.getWidth()];
		encoder.encodeIntoArray(new Tuple(coordinate[0], coordinate[1], radius), output);
		return output;
	}
	
	public double overlap(int[] sdr1, int[] sdr2) {
		assertEquals(sdr1.length, sdr2.length);
		int sum = ArrayUtils.sum(ArrayUtils.and(sdr1, sdr2));

		return (double)sum / (double)ArrayUtils.sum(sdr1);
	} 

	@Test
	public void testLongLatMercatorTransform() {
		setUp();
		builder.scale(30); //meters
		builder.timestep(60); //seconds
		initGE();
		
		double[] coords = new double[] { -122.229194, 37.486782 };
		
		double[] mercatorCoords = ge.toMercator(coords[0], coords[1]);
		assertEquals(mercatorCoords[0], -13606491.6342, 0.0001);
        assertEquals(mercatorCoords[1], 4507176.870955294, 0.0001);
        
        double[] longlats = ge.inverseMercator(mercatorCoords[0], mercatorCoords[1]);
        assertEquals(coords[0], longlats[0], 0.0001);
        assertEquals(coords[1], longlats[1], 0.0001);
        
	}
}
