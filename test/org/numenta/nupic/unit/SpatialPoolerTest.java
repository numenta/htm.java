package org.numenta.nupic.unit;

import static org.junit.Assert.*;

import java.util.EnumMap;

import org.junit.Test;
import org.numenta.nupic.research.Parameters;
import org.numenta.nupic.research.Parameters.KEY;

public class SpatialPoolerTest {
	private Parameters parameters;
	
	public void defaultSetup() {
		parameters = new Parameters();
		EnumMap<Parameters.KEY, Object> p = parameters.getMap();
		p.put(KEY.INPUT_DIMENSIONS, new int[] { 9 });
		p.put(KEY.COLUMN_DIMENSIONS, new int[] { 5 });
		p.put(KEY.POTENTIAL_RADIUS, 3);
		p.put(KEY.POTENTIAL_PCT, 0.5);
		p.put(KEY.GLOBAL_INHIBITIONS, false);
		p.put(KEY.LOCAL_AREA_DENSITY, -1.0);
		p.put(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 3);
		p.put(KEY.STIMULUS_THRESHOLD, 1);
		p.put(KEY.SYN_PERM_INACTIVE_DEC, 0.01);
		p.put(KEY.SYN_PERM_ACTIVE_INC, 0.1);
		p.put(KEY.SYN_PERM_CONNECTED, 0.1);
		p.put(KEY.MIN_PCT_OVERLAP_DUTY_CYCLE, 0.1);
		p.put(KEY.MIN_PCT_ACTIVE_DUTY_CYCLE, 0.1);
		p.put(KEY.DUTY_CYCLE_PERIOD, 10);
		p.put(KEY.MAX_BOOST, 10.0);
		p.put(KEY.SEED, 42);
		p.put(KEY.SP_VERBOSITY, 0);
	}
	
	private void initSP() {
		
	}

	@Test
	public void testCompute1() {
		
	}

}
