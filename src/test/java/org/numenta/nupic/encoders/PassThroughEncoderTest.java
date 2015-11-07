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

import static org.junit.Assert.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;

import org.junit.*;
import org.numenta.nupic.util.*;

public class PassThroughEncoderTest {

	@SuppressWarnings("unchecked")
	@Test
	public void testEncodeArray() {
		PassThroughEncoder<int[]> encoder = new PassThroughEncoder<>(9, 1);
		encoder.setName("foo");
		int bitmap[] = {0,0,0,1,0,0,0,0,0};
		int output[] = new int[9];
		encoder.encodeIntoArray(bitmap, output);
		assertEquals(ArrayUtils.sum(bitmap), ArrayUtils.sum(output));
		Tuple decode = encoder.decode(output, null);
		assertTrue(((HashMap<String, RangeList>) decode.get(0)).containsKey(encoder.getName()));// -1 means test doesn't care
	}

	@Test
	public void testEncodeBitArray(){
		PassThroughEncoder<int[]> encoder = new PassThroughEncoder<>(9, 2);
		encoder.setName("foo");
		int bitmap[] = {0,0,0,1,0,1,0,0,0};
		int[] output = encoder.encode(bitmap);
		assertEquals(ArrayUtils.sum(bitmap),ArrayUtils.sum(output));  
		
		encoder = PassThroughEncoder.builder()
				.n(9)
				.w(ArrayUtils.where(output, ArrayUtils.WHERE_1).length)
				.name("foo")
				.build();
		output = encoder.encode(bitmap);
		assertEquals(ArrayUtils.sum(bitmap),ArrayUtils.sum(output));
	}
	

	@Test
	public void testClosenessScores(){
	
		//Identical => 1
		this.testCloseInner(new int[]{0,0,0,1,1,1,0,0,0}, new int[]{0,0,0,1,1,1,0,0,0}, 1.0);

		//No overlap => 0
		this.testCloseInner(new int[]{0,0,0,1,1,1,0,0,0}, new int[]{1,1,1,0,0,0,1,1,1}, 0.0);

		//Similar => 4 of 5 match
		this.testCloseInner(new int[]{1,0,1,0,1,0,1,0,1}, new int[]{1,0,0,1,1,0,1,0,1}, 0.8);
		
		//Little => 1 of 5 match
		this.testCloseInner(new int[]{1,0,0,1,1,0,1,0,1}, new int[]{0,1,1,1,0,1,0,1,0}, 0.2);
		
		//Extra active bit => off by 1 of 5		
		this.testCloseInner(new int[]{1,0,1,0,1,0,1,0,1}, new int[]{1,0,1,1,1,0,1,0,1}, 0.8);
		
		//Missing active bit => off by 1 of 5
		this.testCloseInner(new int[]{1,0,1,0,1,0,1,0,1}, new int[]{1,0,0,0,1,0,1,0,1}, 0.8);	
	}
	
	@Ignore
	private void testCloseInner(int[] bitmap1, int[] bitmap2, double expectedScore){
		PassThroughEncoder<int[]> encoder = new PassThroughEncoder<>(9, ArrayUtils.where(bitmap1, ArrayUtils.WHERE_1).length);
		encoder.setName("foo");
		
		int[] out1 = encoder.encode(bitmap1);
		encoder.setW(ArrayUtils.where(bitmap2, ArrayUtils.WHERE_1).length);
		int[] out2 = encoder.encode(bitmap2);

		TDoubleList result = encoder.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
		
		encoder = PassThroughEncoder.builder()
				.n(9)
				.w(ArrayUtils.where(bitmap1, ArrayUtils.WHERE_1).length)
				.name("foo")
				.build();
		out1 = encoder.encode(bitmap1);
		encoder.setW(ArrayUtils.where(bitmap2, ArrayUtils.WHERE_1).length);
		out2 = encoder.encode(bitmap2);
		result = encoder.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
	}
}
