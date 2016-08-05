/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.util;

import java.util.List;

import org.numenta.nupic.Connections;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.DistalDendrite;


/**
 * Just a snippet to contain the method {@link #excitedColumnsGenerator(List, List, List, Connections)}
 * @author cogmission
 */
public class Snippet {
    
    public static void main(String[] args) {
        UniversalRandom random = new UniversalRandom(42);
        
        long s = 2858730232218250L;
        long e = (s >>> 35);
        System.out.println("e = " + e);
        
        int x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        x = random.nextInt(50);
        System.out.println("x = " + x);
        
        for(int i = 0;i < 10;i++) {
            int o = random.nextInt(50);
            System.out.println("x = " + o);
        }
        
        /*
         * e = 83200
x = 0
x = 26
x = 14
x = 15
x = 38
x = 47
x = 13
x = 9
x = 15
x = 31
x = 6
x = 3
x = 0
x = 21
x = 45
         */
    }
}
