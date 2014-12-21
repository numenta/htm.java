/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, In  Unless you have an agreement
 * with Numenta, In, for a separate license for this software code, the
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

public class DeltaEncoder extends AdaptiveScalarEncoder {

	/**
	 * 
	 */
	public DeltaEncoder() {
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#init()
	 */
	@Override
	public void init() {
		super.init();
	}

	/* (non-Javadoc)
	 * @see org.numenta.nupic.encoders.AdaptiveScalarEncoder#initEncoder(int, double, double, int, double, double)
	 */
	@Override
	public void initEncoder(int w, double minVal, double maxVal, int n,
			double radius, double resolution) {
		super.initEncoder(w, minVal, maxVal, n, radius, resolution);
	}

	/**
	 * Returns a builder for building DeltaEncoder. This builder may be
	 * reused to produce multiple builders
	 * 
	 * @return a {@code DeltaEncoder.Builder}
	 */
	public static DeltaEncoder.Builder deltaBuilder() {
		return new DeltaEncoder.Builder();
	}

	public static class Builder extends Encoder.Builder<DeltaEncoder.Builder, DeltaEncoder> {
		private Builder() {}

		@Override
		public DeltaEncoder build() {
			encoder = new DeltaEncoder();
			super.build();
			((DeltaEncoder) encoder).init();
			return (DeltaEncoder) encoder;
		}
	}
}
