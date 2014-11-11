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

import java.util.List;

import org.numenta.nupic.util.Tuple;

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

    protected int season;
    protected int dayOfWeek;
    protected int weekend;
    protected int holiday;
    protected int timeOfDay;
    protected int customDays;

    public void setWidth(int width) {
        this.width = width;
    }

    public void setDescription(List<Tuple> description) {
        this.description = description;
    }

    public int getSeason() {
        return season;
    }

    public void setSeason(int season) {
        this.season = season;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getWeekend() {
        return weekend;
    }

    public void setWeekend(int weekend) {
        this.weekend = weekend;
    }

    public int getHoliday() {
        return holiday;
    }

    public void setHoliday(int holiday) {
        this.holiday = holiday;
    }

    public int getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(int timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    public int getCustomDays() {
        return customDays;
    }

    public void setCustomDays(int customDays) {
        this.customDays = customDays;
    }

    /**
     * Constructs a new {@code DateEncoder}
     */
    public DateEncoder() {}

    /**
     * Returns a builder for building DateEncoder.
     * This builder may be reused to produce multiple builders
     *
     * @return a {@code DateEncoder.Builder}
     */
    public static DateEncoder.Builder builder() {
        return new DateEncoder.Builder();
    }

    /**
     * Init the {@code DateEncoder} with parameters
     *
     * season=0, dayOfWeek=0, weekend=0, holiday=0, timeOfDay=0, customDays=0,
     * name = '', forced=True
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


    /**
     * Returns a {@link EncoderBuilder} for constructing {@link DateEncoder}s
     *
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses.
     * Hopefully! :-)
     *
     * @see DateEncoder.Builder#setStuff(int)
     */
    public static class Builder extends Encoder.Builder<DateEncoder.Builder, DateEncoder> {

        protected int season;
        protected int dayOfWeek;
        protected int weekend;
        protected int holiday;
        protected int timeOfDay;
        protected int customDays;

        private Builder() {}

        @Override
        public DateEncoder build() {
            //Must be instantiated so that super class can initialize
            //boilerplate variables.
            encoder = new DateEncoder();

            //Call super class here
            super.build();

            ////////////////////////////////////////////////////////
            //  Implementing classes would do setting of specific //
            //  vars here together with any sanity checking       //
            ////////////////////////////////////////////////////////
            DateEncoder e = ((DateEncoder)encoder);

            e.setSeason(this.season);
            e.setDayOfWeek(this.dayOfWeek);
            e.setWeekend(this.weekend);
            e.setHoliday(this.holiday);
            e.setTimeOfDay(this.timeOfDay);
            e.setCustomDays(this.customDays);

            ((DateEncoder)encoder).init();

            return (DateEncoder)encoder;
        }

        /**
         * Set how many bits are used to encode season
         */
        public DateEncoder.Builder season(int season) {
            this.season = season;
            return this;
        }

        /**
         * Set how many bits are used to encode dayOfWeek
         */
        public DateEncoder.Builder dayOfWeek(int dayOfWeek) {
            this.dayOfWeek = dayOfWeek;
            return this;
        }


        /**
         * Set how many bits are used to encode holiday
         */
        public DateEncoder.Builder holiday(int holiday) {
            this.holiday = holiday;
            return this;
        }

        /**
         * Set how many bits are used to encode timeOfDay
         */
        public DateEncoder.Builder timeOfDay(int timeOfDay) {
            this.timeOfDay = timeOfDay;
            return this;
        }

        /**
         * Set how many bits are used to encode customDays
         */
        public DateEncoder.Builder customDays(int customDays) {
            this.customDays = customDays;
            return this;
        }

        /**
         * Set the name of the encoder
         */
        public DateEncoder.Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Set how many bits are used to encode weekend
         */
        public DateEncoder.Builder weekend(int weekend) {
            this.weekend = weekend;
            return this;
        }

    }
}
