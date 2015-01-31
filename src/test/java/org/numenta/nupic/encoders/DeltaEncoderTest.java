package org.numenta.nupic.encoders;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DeltaEncoderTest {
	
	private AdaptiveScalarEncoder ase;
	private AdaptiveScalarEncoder.Builder aseBuilder;
	private DeltaEncoder de;
	private DeltaEncoder.Builder deBuilder;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		aseBuilder = AdaptiveScalarEncoder.adaptiveBuilder().n(100).w(21).minVal(1)
				.maxVal(8).radius(1.5).resolution(0.5).periodic(false)
				.forced(true);
		deBuilder = DeltaEncoder.deltaBuilder().n(100).w(21).minVal(1)
				.maxVal(8).radius(1.5).resolution(0.5).periodic(false)
				.forced(true);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDeltaEncoder() {
		int[] intArray = new int[100];
		Arrays.fill(intArray, 0);
		for (int i = 2; i < 5; i++) {
			deBuilder.build().encodeIntoArray(Double.valueOf(i), intArray);
			List<EncoderResult> res = deBuilder.build().topDownCompute(intArray);
			System.out.println("Before lock val for: " + i + " " + res.get(0).getValue());
			System.out.println("Before lock scalar for: " + i + " " + res.get(0).getScalar());
			System.out.println("Before lock encoding for: " + i + " " + res.get(0).getEncoding());
		}
		deBuilder.build().setStateLock(true);
		for (int i = 5; i < 7; i++) {
			deBuilder.build().encodeIntoArray(Double.valueOf(i), intArray);
			List<EncoderResult> res = deBuilder.build().topDownCompute(intArray);
			System.out.println("After lock val for: " + i + " " + res.get(0).getValue());
			System.out.println("After lock scalar for: " + i + " " + res.get(0).getScalar());
			System.out.println("After lock encoding for: " + i + " " + res.get(0).getEncoding());
		}
		List<EncoderResult> res = deBuilder.build().topDownCompute(intArray);
		Assert.assertEquals("The value is not matching with expected", res.get(0).getValue(), 6);
	      /*self.assertEqual(res[0].value, 6)
	      self.assertEqual(self._dencoder.topDownCompute(encarr)[0].value, res[0].value)
	      self.assertEqual(self._dencoder.topDownCompute(encarr)[0].scalar, res[0].scalar)
	      self.assertTrue((self._dencoder.topDownCompute(encarr)[0].encoding == res[0].encoding).all())*/
	}

}
