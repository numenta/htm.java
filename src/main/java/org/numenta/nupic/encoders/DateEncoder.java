/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014, Numenta, Inc.  Unless you have an agreement
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

package org.numenta.nupic.encoders;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.array.TDoubleArrayList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.numenta.nupic.Connections;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.ArrayUtils;
import org.numenta.nupic.util.Condition;
import org.numenta.nupic.util.MinMax;
import org.numenta.nupic.util.SparseObjectMatrix;
import org.numenta.nupic.util.Tuple;
import org.numenta.nupic.encoders.ScalarEncoder;

/**
 * Encodes a date according to encoding parameters specified in its constructor.
 *
 * The input to a date encoder is a datetime object. The output is
 * the concatenation of several sub-encodings, each of which encodes a different
 * aspect of the date. Which sub-encodings are present, and details of those
 * sub-encodings, are specified in the DateEncoder constructor.
 *
 * Each parameter describes one attribute to encode. By default, the attribute is
 * not encoded.
 *
 * @author utensil
 */

public class DateEncoder extends Encoder {

    /**
     * Constructs a new {@code DateEncoder}
     *
     * Each parameter describes one attribute to encode. By default, the attribute
     * is not encoded.
     *
     * @param season        season of the year; units = day
     *                      (int) width of attribute; default radius = 91.5 days (1 season)
     *                      (tuple)  season[0] = width; season[1] = radius
     * @param dayOfWeek     monday = 0; units = day
     *                      (int) width of attribute; default radius = 1 day
     *                      (tuple) dayOfWeek[0] = width; dayOfWeek[1] = radius
     * @param weekend       boolean: 0, 1
     *                      (int) width of attribute
     * @param holiday       boolean: 0, 1
     *                      (int) width of attribute
     * @param timeOfday     midnight = 0; units = hour
     *                      (int) width of attribute: default radius = 4 hours
     *                      (tuple) timeOfDay[0] = width; timeOfDay[1] = radius
     * @param customDays    TODO: what is it?
     * @param forced        (default True) : if True, skip checks for parameters' settings;
     *                      see {@code ScalarEncoders} for details
     */
    public DateEncoder(/* self, season=0, dayOfWeek=0, weekend=0, holiday=0, timeOfDay=0, customDays=0,
                name = '', forced=True */) {
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public boolean isDelta() {
        return false;
    }

    @Override
    public int[] encodeIntoArray(double inputData, int[] output) {
        return new int[0];
    }

    @Override
    public int[] encodeIntoArray(String inputData, int[] output) {
        return new int[0];
    }

    @Override
    public void setLearning(boolean learningEnabled) {

    }

    @Override
    public List<Tuple> getDescription() {
        return null;
    }

    @Override
    public <T> List<T> getBucketValues(Class<T> returnType) {
        return null;
    }
}
