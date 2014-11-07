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
 * DOCUMENTATION TAKEN DIRECTLY FROM THE PYTHON VERSION:
 *
 * A date encoder encodes a date according to encoding parameters specified in its constructor.
 *
 * The input to a date encoder is a datetime.datetime object. The output is
 * the concatenation of several sub-encodings, each of which encodes a different
 * aspect of the date. Which sub-encodings are present, and details of those
 * sub-encodings, are specified in the DateEncoder constructor.
 *
 * Each parameter describes one attribute to encode. By default, the attribute
 * is not encoded.
 *
 * season (season of the year; units = day):
 * (int) width of attribute; default radius = 91.5 days (1 season)
 * (tuple)  season[0] = width; season[1] = radius
 *
 * dayOfWeek (monday = 0; units = day)
 * (int) width of attribute; default radius = 1 day
 * (tuple) dayOfWeek[0] = width; dayOfWeek[1] = radius
 *
 * weekend (boolean: 0, 1)
 * (int) width of attribute
 *
 * holiday (boolean: 0, 1)
 * (int) width of attribute
 *
 * timeOfday (midnight = 0; units = hour)
 * (int) width of attribute: default radius = 4 hours
 * (tuple) timeOfDay[0] = width; timeOfDay[1] = radius
 *
 * customDays TODO: what is it?
 *
 * forced (default True) : if True, skip checks for parameters' settings; see {@code ScalarEncoders} for details
 *
 * @author utensil
 *
 * TODO Improve the document:
 *
 * - improve wording on unspecified attributes: "Each parameter describes one extra attribute(other than the datetime
 *   object itself) to encode. By default, the unspecified attributes are not encoded."
 * - change datetime.datetime object to joda-time object
 * - refer to Parameters, which where these parameters are defined.
 */

public class DateEncoder extends Encoder {

    protected int width;

    protected List<Tuple> description;

    /**
     * Constructs a new {@code DateEncoder}
     */
    public DateEncoder(/* self, season=0, dayOfWeek=0, weekend=0, holiday=0, timeOfDay=0, customDays=0,
                name = '', forced=True */) {
    }

    /**
     * Init the {@code DateEncoder} with parameters
     */
    public void init() {

    }

    /**
     * Should return the output width, in bits.
     */
    @Override
    public int getWidth() {
        return width;
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

    /**
     * This returns a list of tuples, each containing (name, offset).
     * The 'name' is a string description of each sub-field, and offset is the bit
     * offset of the sub-field for that encoder.
     *
     * For now, only the 'multi' and 'date' encoders have multiple (name, offset)
     * pairs. All other encoders have a single pair, where the offset is 0.
     *
     * @return		list of tuples, each containing (name, offset)
     */
    @Override
    public List<Tuple> getDescription() {
        return description;
    }

    @Override
    public <T> List<T> getBucketValues(Class<T> returnType) {
        return null;
    }
}
