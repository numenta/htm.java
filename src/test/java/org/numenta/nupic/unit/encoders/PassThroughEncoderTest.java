package org.numenta.nupic.unit.encoders;

import static org.junit.Assert.*;

import java.util.EnumMap;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.encoders.PassThroughEncoder;
import org.numenta.nupic.encoders.ScalarEncoder;

public class PassThroughEncoderTest {

	private PassThroughEncoder encoder;
	private Parameters parameters;

	
	

	@Test
	public void testEncodeArray() {
		//new PassThruEncoder(9);
	}
	
	/*
	def setUp(self):
    self.n = 9
    self.m = 1
    self.name = "foo"
    self._encoder = PassThruEncoder


  def testInitialization(self):
    e = self._encoder(self.n, multiply=self.m, name=self.name)
    self.assertIsInstance(e, self._encoder)


  def testEncodeArray(self):
    """Send bitmap as array"""
    e = self._encoder(self.n, multiply=self.m, name=self.name)
    bitmap = [0,0,0,1,0,0,0,0,0]
    out = e.encode(bitmap)
    self.assertEqual(out.sum(), sum(bitmap)*self.m)

    x = e.decode(out)
    self.assertIsInstance(x[0], dict)
    self.assertTrue(self.name in x[0])

	 */

}
