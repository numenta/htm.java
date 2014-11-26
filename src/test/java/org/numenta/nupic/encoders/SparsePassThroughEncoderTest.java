package org.numenta.nupic.encoders;

import static org.junit.Assert.*;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.HashMap;

import org.junit.*;
import org.junit.rules.ExpectedException;
import org.numenta.nupic.util.*;

public class SparsePassThroughEncoderTest {

	@Test
	public void testEncodeArray() {
		SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(24, null);
		encoder.setName("foo");
		
		//Send bitmap as array of indices  
		int bitmap[] = {2,7,15,18,23};
		int output[] = new int[24];
		encoder.encodeIntoArray(bitmap, output);
		assertEquals(bitmap.length,ArrayUtils.sum(output));
		Tuple decode = encoder.decode(output, null);
		assertTrue(((HashMap) decode.get(0)).containsKey(encoder.getName()));
		
		encoder = SparsePassThroughEncoder.sparseBuilder()
				.n(24)
				.name("foo")
				.build();
		
		output = new int[24];
		encoder.encodeIntoArray(bitmap, output);
		assertEquals(bitmap.length,ArrayUtils.sum(output));
		decode = encoder.decode(output, null);
		assertTrue(((HashMap) decode.get(0)).containsKey(encoder.getName()));
	}
	 @Rule
	  public ExpectedException exception = ExpectedException.none();
	
	@Test
	public void testArrayInvalidWTooBig(){
		SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(9, 3);
		exception.expect(IllegalArgumentException.class);
		encoder.encode(new int[] {2});
		
		encoder = SparsePassThroughEncoder.sparseBuilder()
				.n(9)
				.w(3)
				.name("foo")
				.build();
		exception.expect(IllegalArgumentException.class);
		encoder.encode(new int[] {2});
	}
	
	@Test
	public void testArrayInvalidWTooSmall(){
		SparsePassThroughEncoder encoder = new SparsePassThroughEncoder(9, 3);
		exception.expect(IllegalArgumentException.class);
		encoder.encode(new int[] {2,7,15,18,23});
		
		encoder = SparsePassThroughEncoder.sparseBuilder()
				.n(9)
				.w(3)
				.name("foo")
				.build();
		exception.expect(IllegalArgumentException.class);
		encoder.encode(new int[] {2,7,15,18,23});
	}

	@Ignore
	private void testCloseInner(int[] bitmap1, int outputWidth1, int[] bitmap2, int outputWidth2, double expectedScore){
		SparsePassThroughEncoder encoder1 = new SparsePassThroughEncoder(outputWidth1, null);
		SparsePassThroughEncoder encoder2 = new SparsePassThroughEncoder(outputWidth2, null);
		
		
		int[] out1 = encoder1.encode(bitmap1);
		int[] out2 = encoder2.encode(bitmap2);

		TDoubleList result = encoder1.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
		
		encoder1 = SparsePassThroughEncoder.sparseBuilder()
				.n(outputWidth1)
				.build();
		encoder2 = SparsePassThroughEncoder.sparseBuilder()
				.n(outputWidth2)
				.build();
		
		out1 = encoder1.encode(bitmap1);
		out2 = encoder2.encode(bitmap2);

		result = encoder1.closenessScores(new TDoubleArrayList(ArrayUtils.toDoubleArray(out1)), new TDoubleArrayList(ArrayUtils.toDoubleArray(out2)), true);
		assertTrue(result.size() == 1 );
		assertEquals(expectedScore, result.get(0), 0.0);
	}

	@Test
	public void testClosenessScores(){
	//Identical => 1
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {2,7,15,18,23}, 24, 1.0);
  
	//No overlap => 0
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {3,9,14,19,24}, 25, .0);
	//Similar => 4 of 5 match
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {2,7,17,18,23}, 24, .8);
	
	//Little => 1 of 5 match
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {3,7,17,19,24}, 25, .2);

	//Extra active bit => off by 1 of 5
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {2,7,11,15,18,23}, 24, .8);

	//Missing active bit => off by 1 of 5
	testCloseInner(new int[] {2,7,15,18,23}, 24, new int[] {2,7,18,23}, 24, .8);
	}
}
