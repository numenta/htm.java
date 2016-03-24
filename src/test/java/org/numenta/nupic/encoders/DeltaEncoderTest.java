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
		de = deBuilder.build();
		for (int i = 2; i < 5; i++) {
			de.encodeIntoArray(Double.valueOf(i), intArray);
		}
		de.setStateLock(true);
		for (int i = 5; i < 7; i++) {
			de.encodeIntoArray(Double.valueOf(i), intArray);
		}
		List<Encoding> res = de.topDownCompute(intArray);
		Assert.assertEquals("The value is not matching with expected", res.get(0).getValue(), 9.962025316455696);
		Assert.assertEquals("The value is not matching with expected", res.get(0).getScalar(), 9.962025316455696);
	}
	
	@Test
	public void testEncodingVerification() {
		int[] feedIn = { 1, 8, 4, 7, 8, 6, 3, 1 };
		int[] expectedOut = { 1, 8, 4, 7, 8, 6, 3, 1 };
		de = deBuilder.build();
		ase = aseBuilder.build();
		de.setLearningEnabled(true);
		ase.setLearningEnabled(true);
		for (int i = 0; i < feedIn.length; i++) {
			int[] deArray = new int[100];
			Arrays.fill(deArray, 0);
			int[] aseArray = new int[100];
			Arrays.fill(aseArray, 0);
			ase.encodeIntoArray(Double.valueOf(expectedOut[i]), aseArray);
			// System.out.println(Arrays.toString(aseArray));
			de.encodeIntoArray(Double.valueOf(feedIn[i]), deArray);
			// System.out.println(Arrays.toString(deArray));
			Assert.assertTrue("The arrays are not matching", Arrays.equals(aseArray, deArray));
		}
	}
	
}
