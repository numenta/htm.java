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

import java.util.*;

import gnu.trove.list.TDoubleList;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;
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

public class DateEncoder extends Encoder<Date> {

    protected int width;

    //See DateEncoder.Builder for default values.

    protected Tuple season;
    protected ScalarEncoder seasonEncoder;

    protected Tuple dayOfWeek;
    protected ScalarEncoder dayOfWeekEncoder;

    protected Tuple weekend;
    protected ScalarEncoder weekendEncoder;

    protected Tuple customDays;
    protected ScalarEncoder customDaysEncoder;

    protected Tuple holiday;
    protected ScalarEncoder holidayEncoder;

    protected Tuple timeOfDay;
    protected ScalarEncoder timeOfDayEncoder;

    protected List<Integer> customDaysList = new ArrayList<>();

    // Currently the only holiday we know about is December 25
    // holidays is a list of holidays that occur on a fixed date every year
    protected List<Tuple> HolidaysList = Arrays.asList(
            new Tuple(2, 12, 25)
    );

    /**
     * Constructs a new {@code DateEncoder}
     *
     * Package private to encourage construction using the Builder Pattern
     * but still allow inheritance.
     */
    DateEncoder() {
    }

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
     */
    public void init() {

        width = 0;

        //TODO figure out how to remove this
        //forced (default True)
        setForced(true);

        // Adapted from MultiEncoder
        // TODO figure out why a initial EncoderTuple
        encoders = new LinkedHashMap<>();
        encoders.put(new EncoderTuple("", this, 0), new ArrayList<EncoderTuple>());

        // Note: The order of adding encoders matters, must be in the following
        // season, dayOfWeek, weekend, customDays, holiday, timeOfDay

        if(isValidEncoderPropertyTuple(season)) {
            seasonEncoder = ScalarEncoder.builder()
                    .w((int) season.get(0))
                    .radius((double) season.get(1))
                    .minVal(0)
                    .maxVal(366)
                    .periodic(true)
                    .name("season")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(seasonEncoder);
        }

        if(isValidEncoderPropertyTuple(dayOfWeek)) {
            dayOfWeekEncoder = ScalarEncoder.builder()
                    .w((int) dayOfWeek.get(0))
                    .radius((double) dayOfWeek.get(1))
                    .minVal(0)
                    .maxVal(7)
                    .periodic(true)
                    .name("day of week")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(dayOfWeekEncoder);
        }

        if(isValidEncoderPropertyTuple(weekend)) {
            weekendEncoder = ScalarEncoder.builder()
                    .w((int) weekend.get(0))
                    .radius((double) weekend.get(1))
                    .minVal(0)
                    .maxVal(1)
                    .periodic(false)
                    .name("weekend")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(weekendEncoder);
        }

        if(isValidEncoderPropertyTuple(customDays)) {
            customDaysEncoder = ScalarEncoder.builder()
                    .w((int) customDays.get(0))
                    .radius(1)
                    .minVal(0)
                    .maxVal(1)
                    .periodic(true)
                    .name("customdays")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(customDaysEncoder);

            addCustomDays((ArrayList<String>) customDays.get(1));
        }

        if(isValidEncoderPropertyTuple(holiday)) {
            holidayEncoder = ScalarEncoder.builder()
                    .w((int) holiday.get(0))
                    .radius((double) holiday.get(1))
                    .minVal(0)
                    .maxVal(1)
                    .periodic(true)
                    .name("holiday")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(holidayEncoder);
        }

        if(isValidEncoderPropertyTuple(timeOfDay)) {
            timeOfDayEncoder = ScalarEncoder.builder()
                    .w((int) timeOfDay.get(0))
                    .radius((double) timeOfDay.get(1))
                    .minVal(0)
                    .maxVal(24)
                    .periodic(true)
                    .name("time of day")
                    .forced(this.isForced())
                    .build();
            addChildEncoder(timeOfDayEncoder);
        }
    }

    private boolean isValidEncoderPropertyTuple(Tuple encoderPropertyTuple) {
        return encoderPropertyTuple != null && (int)encoderPropertyTuple.get(0) != 0;
    }

    // Adapted from MultiEncoder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addEncoder(String name, Encoder child) {
        super.addEncoder(this, name, child, width);

        for (Object d : child.getDescription()) {
            Tuple dT = (Tuple) d;
            description.add(new Tuple(2, dT.get(0), (int)dT.get(1) + getWidth()));
        }
        width += child.getWidth();
    }

    protected void addChildEncoder(ScalarEncoder encoder) {
        this.addEncoder(encoder.getName(), encoder);
//        childEncoders.add(new EncoderTuple(encoder.getName(), encoder, offset));
//        description.add(new Tuple(2, encoder.getName(), offset));
//        width += encoder.getWidth();
    }

    protected void addCustomDays(ArrayList<String> daysList) {
        for(String dayStr : daysList)
        {
            switch (dayStr.toLowerCase())
            {
                case "mon":
                case "monday":
                    customDaysList.add(0);
                    break;
                case "tue":
                case "tuesday":
                    customDaysList.add(1);
                    break;
                case "wed":
                case "wednesday":
                    customDaysList.add(2);
                    break;
                case "thu":
                case "thursday":
                    customDaysList.add(3);
                    break;
                case "fri":
                case "friday":
                    customDaysList.add(4);
                    break;
                case "sat":
                case "saturday":
                    customDaysList.add(5);
                    break;
                case "sun":
                case "sunday":
                    customDaysList.add(6);
                    break;
                default:
                    throw new IllegalArgumentException("Invalid custom day: " + dayStr);
            }
        }
    }

    public Tuple getSeason() {
        return season;
    }

    public void setSeason(Tuple season) {
        this.season = season;
    }

    public Tuple getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Tuple dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Tuple getWeekend() {
        return weekend;
    }

    public void setWeekend(Tuple weekend) {
        this.weekend = weekend;
    }

    public Tuple getCustomDays() {
        return customDays;
    }

    public void setCustomDays(Tuple customDays) {
        this.customDays = customDays;
    }

    public Tuple getHoliday() {
        return holiday;
    }

    public void setHoliday(Tuple holiday) {
        this.holiday = holiday;
    }

    public Tuple getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(Tuple timeOfDay) {
        this.timeOfDay = timeOfDay;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getN() {
        return width;
    }

    @Override
    public int getW() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDelta() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    // Adapted from MultiEncoder
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void encodeIntoArray(Date inputData, int[] output) {
        //TODO deal with bad inputData
//        if input == SENTINEL_VALUE_FOR_MISSING_DATA:
//        output[0:] = 0
//        else:
//        if not isinstance(input, datetime.datetime):
//        raise ValueError("Input is type %s, expected datetime. Value: %s" % (
//                type(input), str(input)))

        // Get the scalar values for each sub-field
        TDoubleList scalars = getScalars(inputData);

        int fieldCounter = 0;
        for (EncoderTuple t : getEncoders(this)) {
            String name = t.getName();
            Encoder encoder = t.getEncoder();
            int offset = t.getOffset();

            int[] tempArray = new int[encoder.getWidth()];
            encoder.encodeIntoArray(scalars.get(fieldCounter), tempArray);

            for (int i = 0; i < tempArray.length; i++) {
                output[i + offset] = tempArray[i];
            }

            ++fieldCounter;
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TDoubleList getScalars(Date d) {
        TDoubleList retVals = new TDoubleArrayList();
        List<String> encodedValues = getEncodedValues(d);

        for (String v : encodedValues) {
            //TODO swap it with getEncodedValues():
            // not Double.parseDouble() here , but String.valueOf() there
            retVals.add(Double.parseDouble(v));
        }

        return retVals;
    }

    public List<String> getEncodedValues(Date inputData) {
        //TODO check null (if date is null, it acts like new DateTime())
        if(inputData == null) {
            //TODO return a proper fallback value
            return new ArrayList<>();
        }

        List<String> values = new ArrayList<>();

        DateTime date = new DateTime(inputData);

        //Get the scalar values for each sub-field

        double timeOfDay = date.getHourOfDay() + date.getMinuteOfHour() / 60.0;

        // The day of week was 1 based, so convert to 0 based
        int dayOfWeek = date.getDayOfWeek() - 1; // + timeOfDay / 24.0

        if(seasonEncoder != null) {
            // The day of year was 1 based, so convert to 0 based
            double dayOfYear = date.getDayOfYear() - 1;
            values.add(String.valueOf(dayOfYear));
        }

        if(dayOfWeekEncoder != null) {
            values.add(String.valueOf(dayOfWeek));
        }

        if(weekendEncoder != null) {

            //saturday, sunday or friday evening
            boolean isWeekend = dayOfWeek == 6 || dayOfWeek == 5 ||
                    (dayOfWeek == 4 && timeOfDay > 18);

            int weekend = isWeekend ? 1 : 0;

            values.add(String.valueOf(weekend));
        }

        if(customDaysEncoder != null) {
            boolean isCustomDays = customDaysList.contains(dayOfWeek);

            int customDay = isCustomDays ? 1 : 0;

            values.add(String.valueOf(customDay));
        }

        if(holidayEncoder != null) {
            // A "continuous" binary value. = 1 on the holiday itself and smooth ramp
            //  0->1 on the day before the holiday and 1->0 on the day after the holiday.
            Tuple dateTuple = new Tuple(date.getMonthOfYear(), date.getDayOfMonth());

            double holidayness = 0;

            for(Tuple h : HolidaysList) {
                //hdate is midnight on the holiday
                DateTime hdate = new DateTime(date.getYear(), (int)h.get(0), (int)h.get(1), 0, 0, 0);

                if(date.isAfter(hdate)) {
                    Duration diff = new Interval(hdate, date).toDuration();
                    long days = diff.getStandardDays();
                    if(days == 0) {
                        //return 1 on the holiday itself
                        holidayness = 1;
                        break;
                    } else if(days == 1) {
                        //ramp smoothly from 1 -> 0 on the next day
                        holidayness = 1.0 - (diff.getStandardSeconds() / 86400.0);
                        break;
                    }

                } else {
                    //TODO this is not the same as when date.isAfter(hdate), why?
                    Duration diff = new Interval(date, hdate).toDuration();
                    long days = diff.getStandardDays();
                    if(days == 0) {
                        //ramp smoothly from 0 -> 1 on the previous day
                        holidayness = 1.0 - (diff.getStandardSeconds() / 86400.0);
                        break;
                    }
                }
            }

            values.add(String.valueOf(holidayness));
        }

        if(timeOfDayEncoder != null) {
            values.add(String.valueOf(timeOfDay));
        }

        return values;
    }

    //TODO Should delegate Encoder.getScalarNames() to Encoder.getScalarNames(null) ?

    @Override
    public <S> List<S> getBucketValues(Class<S> returnType) {
        return null;
    }

    public int[] getBucketIndices(Date input) {

        TDoubleList scalars = getScalars(input);

        TIntList l = new TIntArrayList();
        List<EncoderTuple> encoders = getEncoders(this);
        if(encoders != null && encoders.size() > 0) {
            int i = 0;
            for(EncoderTuple t : encoders) {
                l.addAll(t.getEncoder().getBucketIndices(scalars.get(i)));
                ++i;
            }
        }else{
            throw new IllegalStateException("Should be implemented in base classes that are not " +
                    "containers for other encoders");
        }
        return l.toArray();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void setLearning(boolean learningEnabled) {
        for (EncoderTuple t : getEncoders(this)) {
            Encoder encoder = t.getEncoder();
            encoder.setLearningEnabled(learningEnabled);
        }
    }

    /**
     * Returns a {@link Encoder.Builder} for constructing {@link DateEncoder}s
     *
     * The base class architecture is put together in such a way where boilerplate
     * initialization can be kept to a minimum for implementing subclasses.
     * Hopefully! :-)
     *
     * @see ScalarEncoder.Builder#setStuff(int)
     */
    public static class Builder extends Encoder.Builder<DateEncoder.Builder, DateEncoder> {

        //    Ignore leapyear differences -- assume 366 days in a year
        //    Radius = 91.5 days = length of season
        //    Value is number of days since beginning of year (0 - 355)
        protected Tuple season = new Tuple(2, 0, 91.5);

        // Value is day of week (floating point)
        // Radius is 1 day
        protected Tuple dayOfWeek = new Tuple(2, 0, 1.0);

        // Binary value.
        protected Tuple weekend = new Tuple(2, 0, 1.0);

        // Custom days encoder, first argument in tuple is width
        // second is either a single day of the week or a list of the days
        // you want encoded as ones.
        protected Tuple customDays = new Tuple(2, 0, new ArrayList<String>());

        // A "continuous" binary value. = 1 on the holiday itself and smooth ramp
        //  0->1 on the day before the holiday and 1->0 on the day after the holiday.
        protected Tuple holiday = new Tuple(2, 0, 1.0);

        // Value is time of day in hours
        // Radius = 4 hours, e.g. morning, afternoon, evening, early night,
        //  late night, etc.
        protected Tuple timeOfDay = new Tuple(2, 0, 4.0);

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
        public DateEncoder.Builder season(int season, double radius) {
            this.season = new Tuple(2, season, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode season
         */
        public DateEncoder.Builder season(int season) {
            return this.season(season, (double) this.season.get(1));
        }

        /**
         * Set how many bits are used to encode dayOfWeek
         */
        public DateEncoder.Builder dayOfWeek(int dayOfWeek, double radius) {
            this.dayOfWeek = new Tuple(2, dayOfWeek, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode dayOfWeek
         */
        public DateEncoder.Builder dayOfWeek(int dayOfWeek) {
            return this.dayOfWeek(dayOfWeek, (double) this.dayOfWeek.get(1));
        }

        /**
         * Set how many bits are used to encode weekend
         */
        public DateEncoder.Builder weekend(int weekend, double radius) {
            this.weekend = new Tuple(2, weekend, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode weekend
         */
        public DateEncoder.Builder weekend(int weekend) {
            return this.weekend(weekend, (double) this.weekend.get(1));
        }

        /**
         * Set how many bits are used to encode customDays
         */
        public DateEncoder.Builder customDays(int customDays, ArrayList<String> customDaysList) {
            this.customDays = new Tuple(2, customDays, customDaysList);
            return this;
        }

        /**
         * Set how many bits are used to encode customDays
         */
        public DateEncoder.Builder customDays(int customDays) {
            return this.customDays(customDays, (ArrayList<String>) this.customDays.get(1));
        }

        /**
         * Set how many bits are used to encode holiday
         */
        public DateEncoder.Builder holiday(int holiday, double radius) {
            this.holiday = new Tuple(2, holiday, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode holiday
         */
        public DateEncoder.Builder holiday(int holiday) {
            return this.holiday(holiday, (double) this.holiday.get(1));
        }

        /**
         * Set how many bits are used to encode timeOfDay
         */
        public DateEncoder.Builder timeOfDay(int timeOfDay, double radius) {
            this.timeOfDay = new Tuple(2, timeOfDay, radius);
            return this;
        }

        /**
         * Set how many bits are used to encode timeOfDay
         */
        public DateEncoder.Builder timeOfDay(int timeOfDay) {
            return this.timeOfDay(timeOfDay, (double) this.timeOfDay.get(1));
        }

        /**
         * Set the name of the encoder
         */
        public DateEncoder.Builder name(String name) {
            this.name = name;
            return this;
        }


    }
}
