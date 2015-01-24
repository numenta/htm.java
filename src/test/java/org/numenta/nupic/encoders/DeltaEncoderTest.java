package org.numenta.nupic.encoders;

import org.junit.After;
import org.junit.AfterClass;
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
		for (int i = 0; i < 5; i++) {
			
		}
	}

}
