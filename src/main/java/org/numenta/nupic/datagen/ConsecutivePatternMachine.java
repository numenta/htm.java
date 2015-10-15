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

package org.numenta.nupic.datagen;

import java.util.LinkedHashSet;

/**
 * Utilities for generating and manipulating patterns consisting of consecutive
 * sequences of numbers, for use in experimentation and tests.
 * 
 * 
 * @author Chetan Surpur
 * @author David Ray
 *
 * @see PatternMachine
 */
public class ConsecutivePatternMachine extends PatternMachine {
    public ConsecutivePatternMachine(int n, int w) {
        super(n, w);
    }
    
    @Override
    public void generate() {
        LinkedHashSet<Integer> pattern;
        for(int i = 0;i < n / w;i++) {
            pattern = xrange(i * w, (i + 1) * w);
            patterns.put(i, pattern);
        }
    }
}
