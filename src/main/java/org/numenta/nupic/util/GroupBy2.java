/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2014-2016, Numenta, Inc.  Unless you have an agreement
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.numenta.nupic.util.GroupBy2.Slot;

import chaschev.lang.Pair;


/**
 * An Java extension to groupby in Python's itertools. Allows to walk across n sorted lists
 * with respect to their key functions and yields a {@link Tuple} of n lists of the
 * members of the next *smallest* group.
 * 
 * @author cogmission
 * @param <R>   The return type of the user-provided {@link Function}s
 */
public class GroupBy2<R extends Comparable<R>> implements Generator<Tuple> {
    /** serial version */
    private static final long serialVersionUID = 1L;
    
    /** stores the user inputted pairs */
    private Pair<List<Object>, Function<Object, R>>[] entries;
    
    /** stores the {@link GroupBy} {@link Generator}s created from the supplied lists */
    private List<GroupBy<Object, R>> generatorList;
    
    /** the current interation's minimum key value */
    private R minKeyVal;
    
    
    ///////////////////////
    //    Control Lists  //
    ///////////////////////
    private boolean[] advanceList;
    private Slot<Pair<Object, R>>[] nextList;
    
    private int numEntries;
    
    /**
     * Private internally used constructor. To instantiate objects of this
     * class, please see the static factory method {@link #of(Pair...)}
     * 
     * @param entries   a {@link Pair} of lists and their key-producing functions
     */
    private GroupBy2(Pair<List<Object>, Function<Object, R>>[] entries) {
        this.entries = entries;
    }
    
    /**
     * <p>
     * Returns a {@code GroupBy2} instance which is used to group lists of objects
     * in ascending order using keys supplied by their associated {@link Function}s.
     * </p><p>
     * <b>Here is an example of the usage and output of this object: (Taken from {@link GroupBy2Test})</b>
     * </p>
     * <pre>
     *  List<Integer> sequence0 = Arrays.asList(new Integer[] { 7, 12, 16 });
     *  List<Integer> sequence1 = Arrays.asList(new Integer[] { 3, 4, 5 });
     *  
     *  Function<Integer, Integer> identity = Function.identity();
     *  Function<Integer, Integer> times3 = x -> x * 3;
     *  
     *  @SuppressWarnings({ "unchecked", "rawtypes" })
     *  GroupBy2<Integer> groupby2 = GroupBy2.of(
     *      new Pair(sequence0, identity), 
     *      new Pair(sequence1, times3));
     *  
     *  for(Tuple tuple : groupby2) {
     *      System.out.println(tuple);
     *  }
     * </pre>
     * <br>
     * <b>Will output the following {@link Tuple}s:</b>
     * <pre>
     *  '7':'[7]':'[NONE]'
     *  '9':'[NONE]':'[3]'
     *  '12':'[12]':'[4]'
     *  '15':'[NONE]':'[5]'
     *  '16':'[16]':'[NONE]'
     *  
     *  From row 1 of the output:
     *  Where '7' == Tuple.get(0), 'List[7]' == Tuple.get(1), 'List[NONE]' == Tuple.get(2) == empty list with no members
     * </pre>
     * 
     * <b>Note: Read up on groupby here:</b><br>
     *   https://docs.python.org/dev/library/itertools.html#itertools.groupby
     * <p> 
     * @param entries
     * @return  a n + 1 dimensional tuple, where the first element is the
     *          key of the group and the other n entries are lists of
     *          objects that are a member of the current group that is being
     *          iterated over in the nth list passed in. Note that this
     *          is a generator and a n+1 dimensional tuple is yielded for
     *          every group. If a list has no members in the current
     *          group, {@link Slot#NONE} is returned in place of a generator.
     */
    @SuppressWarnings("unchecked")
    public static <R extends Comparable<R>> GroupBy2<R> of(Pair<List<Object>, Function<Object, R>>... entries) {
        return new GroupBy2<>(entries);
    }
    
    /**
     * (Re)initializes the internal {@link Generator}(s). This method
     * may be used to "restart" the internal {@link Iterator}s and
     * reuse this object.
     */
    @SuppressWarnings("unchecked")
    public void reset() {
        generatorList = new ArrayList<>();
        
        for(int i = 0;i < entries.length;i++) {
            generatorList.add(GroupBy.of(entries[i].getFirst(), entries[i].getSecond()));
        }
        
        numEntries = generatorList.size();
        
//        for(int i = 0;i < numEntries;i++) {
//            for(Pair<Object, R> p : generatorList.get(i)) {
//                System.out.println("generator " + i + ": " + p.getKey() + ",  " + p.getValue());
//            }
//            System.out.println("");
//        }
//        
//        generatorList = new ArrayList<>();
//        
//        for(int i = 0;i < entries.length;i++) {
//            generatorList.add(GroupBy.of(entries[i].getKey(), entries[i].getValue()));
//        }
        
        advanceList = new boolean[numEntries];
        Arrays.fill(advanceList, true);
        nextList = new Slot[numEntries];
        Arrays.fill(nextList, Slot.NONE);
    }
    
    /**
     * {@inheritDoc}
     */
    public Iterator<Tuple> iterator() { return this; }
    
    /**
     * Returns a flag indicating that at least one {@link Generator} has
     * a matching key for the current "smallest" key generated.
     * 
     * @return a flag indicating that at least one {@link Generator} has
     * a matching key for the current "smallest" key generated.
     */
    @Override
    public boolean hasNext() {
        if(generatorList == null) {
            reset();
        }
        
        advanceSequences();
        
        return nextMinKey();
    }
    
    /**
     * Returns a {@link Tuple} containing the current key in the
     * zero'th slot, and a list objects which are members of the
     * group specified by that key.
     * 
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public Tuple next() {
        
        Object[] objs = IntStream
            .range(0, numEntries + 1)
            .mapToObj(i -> i==0 ? minKeyVal : new ArrayList<R>())
            .toArray();
        
        Tuple retVal = new Tuple((Object[])objs);
        
        for(int i = 0;i < numEntries;i++) {
            if(isEligibleList(i, minKeyVal)) {
                ((List<Object>)retVal.get(i + 1)).add(nextList[i].get().getFirst());
                drainKey(retVal, i, minKeyVal);
                advanceList[i] = true;
            }else{
                advanceList[i] = false;
                ((List<Object>)retVal.get(i + 1)).add(Slot.empty());
            }
        }
        
        return retVal;
    }
    
    /**
     * Internal method which advances index of the current
     * {@link GroupBy}s for each group present.
     */
    private void advanceSequences() {
        for(int i = 0;i < numEntries;i++) {
            if(advanceList[i]) {
                nextList[i] = generatorList.get(i).hasNext() ?
                    Slot.of(generatorList.get(i).next()) : Slot.empty();
            }
        }
    }
    
    /**
     * Returns the next smallest generated key.
     * 
     * @return  the next smallest generated key.
     */
    private boolean nextMinKey() {
        return Arrays.stream(nextList)
            .filter(opt -> opt.isPresent())
            .map(opt -> opt.get().getSecond())
            .min((k, k2) -> k.compareTo(k2))
            .map(k -> { minKeyVal = k; return k; } )
            .isPresent();
    }
    
    /**
     * Returns a flag indicating whether the list currently pointed
     * to by the specified index contains a key which matches the
     * specified "targetKey".
     * 
     * @param listIdx       the index pointing to the {@link GroupBy} being
     *                      processed.
     * @param targetKey     the specified key to match.
     * @return  true if so, false if not
     */
    private boolean isEligibleList(int listIdx, Object targetKey) {
       return nextList[listIdx].isPresent() && nextList[listIdx].get().getSecond().equals(targetKey);
    }
    
    /**
     * Each input grouper may generate multiple members which match the
     * specified "targetVal". This method guarantees that all members 
     * are added to the list residing at the specified Tuple index.
     * 
     * @param retVal        the Tuple being added to
     * @param listIdx       the index specifying the list within the 
     *                      tuple which will have members added to it
     * @param targetVal     the value to match in order to be an added member
     */
    @SuppressWarnings("unchecked")
    private void drainKey(Tuple retVal, int listIdx, R targetVal) {
        while(generatorList.get(listIdx).hasNext()) {
            if(generatorList.get(listIdx).peek().getSecond().equals(targetVal)) {
                nextList[listIdx] = Slot.of(generatorList.get(listIdx).next());
                ((List<Object>)retVal.get(listIdx + 1)).add(nextList[listIdx].get().getFirst());
            }else{
                nextList[listIdx] = Slot.empty();
                break;
            }
        }
    }
    
    /**
     * A minimal {@link Serializable} version of an {@link Slot}
     * @param <T>   the value held within this {@code Slot}
     */
    public static final class Slot<T> implements Serializable {
        /** Default Serial */
        private static final long serialVersionUID = 1L;
        
        /**
         * Common instance for {@code empty()}.
         */
        public static final Slot<?> NONE = new Slot<>();

        /**
         * If non-null, the value; if null, indicates no value is present
         */
        private final T value;
        
        private Slot() { this.value = null; }
        
        /**
         * Constructs an instance with the value present.
         *
         * @param value the non-null value to be present
         * @throws NullPointerException if value is null
         */
        private Slot(T value) {
            this.value = Objects.requireNonNull(value);
        }

        /**
         * Returns an {@code Slot} with the specified present non-null value.
         *
         * @param <T> the class of the value
         * @param value the value to be present, which must be non-null
         * @return an {@code Slot} with the value present
         * @throws NullPointerException if value is null
         */
        public static <T> Slot<T> of(T value) {
            return new Slot<>(value);
        }

        /**
         * Returns an {@code Slot} describing the specified value, if non-null,
         * otherwise returns an empty {@code Slot}.
         *
         * @param <T> the class of the value
         * @param value the possibly-null value to describe
         * @return an {@code Slot} with a present value if the specified value
         * is non-null, otherwise an empty {@code Slot}
         */
        @SuppressWarnings("unchecked")
        public static <T> Slot<T> ofNullable(T value) {
            return value == null ? (Slot<T>)NONE : of(value);
        }

        /**
         * If a value is present in this {@code Slot}, returns the value,
         * otherwise throws {@code NoSuchElementException}.
         *
         * @return the non-null value held by this {@code Slot}
         * @throws NoSuchElementException if there is no value present
         *
         * @see Slot#isPresent()
         */
        public T get() {
            if (value == null) {
                throw new NoSuchElementException("No value present");
            }
            return value;
        }
        
        /**
         * Returns an empty {@code Slot} instance.  No value is present for this
         * Slot.
         *
         * @param <T> Type of the non-existent value
         * @return an empty {@code Slot}
         */
        public static<T> Slot<T> empty() {
            @SuppressWarnings("unchecked")
            Slot<T> t = (Slot<T>) NONE;
            return t;
        }

        /**
         * Return {@code true} if there is a value present, otherwise {@code false}.
         *
         * @return {@code true} if there is a value present, otherwise {@code false}
         */
        public boolean isPresent() {
            return value != null;
        }
        
        /**
         * Indicates whether some other object is "equal to" this Slot. The
         * other object is considered equal if:
         * <ul>
         * <li>it is also an {@code Slot} and;
         * <li>both instances have no value present or;
         * <li>the present values are "equal to" each other via {@code equals()}.
         * </ul>
         *
         * @param obj an object to be tested for equality
         * @return {code true} if the other object is "equal to" this object
         * otherwise {@code false}
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof Slot)) {
                return false;
            }

            Slot<?> other = (Slot<?>) obj;
            return Objects.equals(value, other.value);
        }

        /**
         * Returns the hash code value of the present value, if any, or 0 (zero) if
         * no value is present.
         *
         * @return hash code value of the present value or 0 if no value is present
         */
        @Override
        public int hashCode() {
            return Objects.hashCode(value);
        }

        /**
         * Returns a non-empty string representation of this Slot suitable for
         * debugging. The exact presentation format is unspecified and may vary
         * between implementations and versions.
         *
         * @implSpec If a value is present the result must include its string
         * representation in the result. Empty and present Slots must be
         * unambiguously differentiable.
         *
         * @return the string representation of this instance
         */
        @Override
        public String toString() {
            return value != null ? String.format("Slot[%s]", value) : "NONE";
        }
    }
}
