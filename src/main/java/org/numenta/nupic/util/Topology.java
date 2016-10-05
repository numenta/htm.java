/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
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
package org.numenta.nupic.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;


public class Topology extends Coordinator implements Serializable {
    /** keep it simple */
    private static final long serialVersionUID = 1L;
    
    private IntGenerator[] igs;
    private int[] centerPosition;
    
    
    /**
     * Constructs a new {@link AbstractFlatMatrix} object to be configured with specified
     * dimensions and major ordering.
     * @param shape  the dimensions of this matrix 
     */
    public Topology(int... shape) {
        super(shape, false);
    }
    
    /**
     * Translate an index into coordinates, using the given coordinate system.
     * 
     * @param index     The index of the point. The coordinates are expressed as a single index by
     *                  using the dimensions as a mixed radix definition. For example, in dimensions
     *                  42x10, the point [1, 4] is index 1*420 + 4*10 = 460.
     * @return          A array of coordinates of length len(dimensions).
     */
    public int[] coordinatesFromIndex(int index) {
        return computeCoordinates(index);
    }

    /**
     * Translate coordinates into an index, using the given coordinate system.
     * 
     * @param coordinates       A array of coordinates of length dimensions.size().
     * @param shape             The coordinate system.
     * @return                  The index of the point. The coordinates are expressed as a single index by
     *                          using the dimensions as a mixed radix definition. For example, in dimensions
     *                          42x10, the point [1, 4] is index 1*420 + 4*10 = 460.
     */
    public int indexFromCoordinates(int... coordinates) {
        return computeIndex(coordinates);
    }
    
    /**
     * Get the points in the neighborhood of a point.
     *
     * A point's neighborhood is the n-dimensional hypercube with sides ranging
     * [center - radius, center + radius], inclusive. For example, if there are two
     * dimensions and the radius is 3, the neighborhood is 6x6. Neighborhoods are
     * truncated when they are near an edge.
     * 
     * @param centerIndex       The index of the point. The coordinates are expressed as a single index by
     *                          using the dimensions as a mixed radix definition. For example, in dimensions
     *                          42x10, the point [1, 4] is index 1*420 + 4*10 = 460.
     * @param radius            The radius of this neighborhood about the centerIndex.
     * @return  The points in the neighborhood, including centerIndex.
     */
    public int[] neighborhood(int centerIndex, int radius) {
        centerPosition = coordinatesFromIndex(centerIndex);
        
        igs = IntStream.range(0, dimensions.length)
            .mapToObj(i -> 
                IntGenerator.of(Math.max(0, centerPosition[i] - radius), 
                    Math.min(dimensions[i] - 1, centerPosition[i] + radius) + 1))
            .toArray(IntGenerator[]::new);
       
        List<TIntList> result = new ArrayList<>();
        result.add(new TIntArrayList());
        List<TIntList> interim = new ArrayList<>();
        for(IntGenerator pool : igs) {
            int size = result.size();
            interim.clear();
            interim.addAll(result);
            result.clear();
            for(int x = 0;x < size;x++) {
                TIntList lx = interim.get(x);
                pool.reset();
                for(int y = 0;y < pool.size();y++) {
                    int py = pool.next();
                    TIntArrayList tl = new TIntArrayList();
                    tl.addAll(lx);
                    tl.add(py);
                    result.add(tl);
                }
            }
        }
        
        return result.stream().mapToInt(tl -> indexFromCoordinates(tl.toArray())).toArray();
    }
    
    /**
     * Like {@link #neighborhood(int, int)}, except that the neighborhood isn't truncated when it's
     * near an edge. It wraps around to the other side.
     * 
     * @param centerIndex       The index of the point. The coordinates are expressed as a single index by
     *                          using the dimensions as a mixed radix definition. For example, in dimensions
     *                          42x10, the point [1, 4] is index 1*420 + 4*10 = 460.
     * @param radius            The radius of this neighborhood about the centerIndex.
     * @return  The points in the neighborhood, including centerIndex.
     */
    public int[] wrappingNeighborhood(int centerIndex, int radius) {
        int[] cp = coordinatesFromIndex(centerIndex);
        
        IntGenerator[] igs = IntStream.range(0, dimensions.length)
            .mapToObj(i -> 
                new IntGenerator(cp[i] - radius, 
                    Math.min((cp[i] - radius) + dimensions[i] - 1, cp[i] + radius) + 1))
            .toArray(IntGenerator[]::new);
        
        List<TIntList> result = new ArrayList<>();
        result.add(new TIntArrayList());
        List<TIntList> interim = new ArrayList<>();
        for(int i = 0;i < igs.length;i++) {
            IntGenerator pool = igs[i];
            int size = result.size();
            interim.clear();
            interim.addAll(result);
            result.clear();
            for(int x = 0;x < size;x++) {
                TIntList lx = interim.get(x);
                pool.reset();
                for(int y = 0;y < pool.size();y++) {
                    int py = ArrayUtils.modulo(pool.next(), dimensions[i]);
                    TIntArrayList tl = new TIntArrayList();
                    tl.addAll(lx);
                    tl.add(py);
                    result.add(tl);
                }
            }
        }
        
        return result.stream().mapToInt(tl -> indexFromCoordinates(tl.toArray())).toArray();
    }
}
