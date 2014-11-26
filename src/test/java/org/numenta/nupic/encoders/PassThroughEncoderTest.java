package org.numenta.nupic.encoders;

import static org.junit.Assert.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;

import org.junit.*;
import org.numenta.nupic.util.*;

public class PassThroughEncoderTest {

	@Test
	public void testEncodeArray() {
		PassThroughEncoder encoder = new PassThroughEncoder(9, null);
		encoder.setName("foo");
		int bitmap[] = {0,0,0,1,0,0,0,0,0};
		int output[] = new int[9];
		encoder.encodeIntoArray(bitmap, output);
		assertEquals(ArrayUtils.sum(bitmap), ArrayUtils.sum(output));
		Tuple decode = encoder.decode(output, null);
		assertTrue(((HashMap) decode.get(0)).containsKey(encoder.getName()));
		
		encoder = PassThroughEncoder.builder()
				.n(9)
				.name("foo")
				.build();
		encoder.setName("foo");
		output = new int[9];
		encoder.encodeIntoArray(bitmap, output);
		assertEquals(ArrayUtils.sum(bitmap), ArrayUtils.sum(output));
		decode = encoder.decode(output, null);
		assertTrue(((HashMap) decode.get(0)).containsKey(encoder.getName()));
	}

	@Test
	public void testEncodeBitArray(){
		PassThroughEncoder encoder = new PassThroughEncoder(9, null);
		encoder.setName("foo");
		int bitmap[] = {0,0,0,1,0,1,0,0,0};
		int[] output = encoder.encode(bitmap);
		assertEquals(ArrayUtils.sum(bitmap),ArrayUtils.sum(output));  
		
		encoder = PassThroughEncoder.builder()
				.n(9)
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
		PassThroughEncoder encoder = new PassThroughEncoder(9, null);
		encoder.setName("foo");
		
		int[] out1 = encoder.encode(bitmap1);
		int[] out2 = encoder.encode(bitmap2);

		TDoubleList result = encoder.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
		
		encoder = PassThroughEncoder.builder()
				.n(9)
				.name("foo")
				.build();
		out1 = encoder.encode(bitmap1);
		out2 = encoder.encode(bitmap2);
		result = encoder.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
	}
}
