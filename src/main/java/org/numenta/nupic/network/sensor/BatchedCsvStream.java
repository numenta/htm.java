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
package org.numenta.nupic.network.sensor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Specialized {@link Stream} for CSV (Comma Separated Values)
 * stream processing. Configure this Stream with a batch size and
 * a header length, and just treat as normal {@link Stream}.
 * </p>
 * <p>
 * To create a {@code BatchedCsvStream}, call {@link BatchedCsvStream#batch(Stream, int, boolean, int)}
 * handing it the underlying Stream to handle, the batch size, whether it should be parallelized,
 * and the size of the header and it will return a Stream that will handle 
 * batching when "isParallel" is set to true. When "isParallel" is set to false, no batching
 * takes place because there would be no point.
 * </p>
 * <p>
 * A side effect to be aware of when batching is the insertion of a "sequenceNumber" to the first column
 * of every line. This sequenceNumber describes the "encounter order" of the line in question
 * and can reliably be used to "re-order" the entire stream at a later point.
 * </p>
 * 
 * <p>
 * <pre>
 * To reorder the Stream use code such as:
 *      Stream thisStream;
 *      List&lt;String&gt; sortedList = thisStream.sorted(
 *          (String[] i, String[] j) -&gt; {
 *              return Integer.valueOf(i[0]).compareTo(Integer.valueOf(j[0]));
 *          }).collect(Collectors.toList());
 * </pre>
 *
 * 
 * The batching implemented is pretty straight forward. The underlying iterator is
 * advanced to i + min(batchSize, remainingCount), where each line is fed into
 * a queue of Objects, the {@link BatchSpliterator#tryAdvance(Consumer)}
 * is called with a {@link BatchSpliterator.SequencingConsumer} which inserts
 * the sequenceNumber into the head of the line array after calling 
 * {@link System#arraycopy(Object, int, Object, int, int)} to increase its size.
 * 
 *  
 * 
 * @author David Ray
 *
 * @param <T> The Type of data on each line of this Stream (String[] for this implementation)
 */
public class BatchedCsvStream<T> implements MetaStream<T>, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    
    // TOP TWO CLASSES ARE THE BatchSpliterator AND THE BatchedCsvHeader //
    // See main() at bottom for localized mini-test
    
    //////////////////////////////////////////////////////////////
    //                      Inner Classes                       //
    //////////////////////////////////////////////////////////////
    /**
     * The internal batching {@link Spliterator} implementation.
     * This does all the magic of splitting the stream into "jobs"
     * that each cpu core can handle.
     * 
     * @author David Ray
     * @see Header
     * @see BatchedCsvStream
     */
    private static class BatchSpliterator implements Spliterator<String[]> {        
        private final int batchSize;
        private final int characteristics;
        private int sequenceNum;
        private long est;
        private BatchedCsvStream<String[]> csv;
        private transient Spliterator<String[]> spliterator;
        
        
        /**
         * Creates a new BatchSpliterator
         * 
         * @param characteristics   the bit flags indicating the different
         *                          {@link Spliterator} configurations
         * @param batchSize         the size of each "chunk" to hand off to
         *                          a Thread
         * @param est               estimation-only, of the remaining size
         */
        public BatchSpliterator(int characteristics, int batchSize, long est) {
            this.characteristics = characteristics | Spliterator.SUBSIZED;
            this.batchSize = batchSize;
            this.est = est;
        }
        
        /**
         * Called internally to store the reference to the parent {@link BatchedCsvStream}.
         * 
         * @param csv       the parent {@code BatchedCsvStream}
         * @return          this {@code BatchSpliterator}
         */
        private BatchSpliterator setCSV(BatchedCsvStream<String[]> csv) {
            this.csv = csv;
            return this;
        }
        
        /**
         * Called internally to store a reference to the functional {@link Spliterator}
         * @param toWrap
         * @return
         */
        private BatchSpliterator setToWrap(Spliterator<String[]> toWrap) {
            this.spliterator = toWrap;
            return this;
        }
        
        /**
         * Overridden to call the delegate {@link Spliterator} and update
         * this Spliterator's sequence number.
         * 
         * @return a flag indicating whether there is a value available
         */
        @Override 
        public boolean tryAdvance(Consumer<? super String[]> action) {
            boolean hasNext;
            if(hasNext = spliterator.tryAdvance(action)) {
                sequenceNum++;
            }
            return hasNext;
        }

        /**
         * Little cousin to {@link #tryAdvance(Consumer)} which is called 
         * after the spliterator is depleted to see if there are any remaining
         * values.
         */
        @Override 
        public void forEachRemaining(Consumer<? super String[]> action) {
            spliterator.forEachRemaining(action);
        }

        /**
         * Called by the Fork/Join mechanism to divide and conquer by 
         * creating {@link Spliterator}s for each thread. This method
         * returns a viable Spliterator over the configured number of
         * lines. see {@link #batchSize}
         */
        @Override 
        public Spliterator<String[]> trySplit() {
            final SequencingConsumer holder = csv.isArrayType ? new SequencingArrayConsumer() : new SequencingConsumer();
         
            //This is the line that makes this implementation tricky due to
            //a side effect in the purpose of this method. The try advance
            //actually advances so when it is called twice, (because it is
            //used to query if there is a "next" also) we need to handle it
            //for the first and last sequence. We also have to make sure our
            //sequence number is being handled so that we can "re-order" the
            //parallel pieces later. (They're inserted at the row-heads of each
            //line).
            if (!tryAdvance(holder)) {
                return null;
            }
            
            csv.setBatchOp(true);
            
            final Object[] lines = new Object[batchSize];
            int j = 0;
            do {
                lines[j] = holder.value;
            } while (++j < batchSize && tryAdvance(holder));
            
            if (est != Long.MAX_VALUE) est -= j;
            return Spliterators.spliterator(lines, 0, j, characteristics | SIZED);
        }

        /**
         * Returns a specialized {@link Comparator} if the characteristics are set
         * to {@link Spliterator#SORTED} and a call to {@link 
         * @return
         */
        @Override 
        public Comparator<? super String[]> getComparator() {
            if (hasCharacteristics(Spliterator.SORTED) && csv.isBatchOp) {
                return (i, j) -> { return Long.valueOf(i[0]).compareTo(Long.valueOf(j[0])); };
            }else if(csv.isBatchOp) {
                return null;
            }
            throw new IllegalStateException();
        }

        @Override 
        public long estimateSize() { 
            return est; 
        }

        @Override 
        public int characteristics() { 
            return characteristics; 
        }

        class SequencingConsumer implements Consumer<String[]> {
            String[] value;
            @Override public void accept(String[] value) { 
                csv.isTerminal = true;
                this.value = new String[value.length + 1];
                System.arraycopy(value, 0, this.value, 1, value.length);
                this.value[0] = String.valueOf(sequenceNum);
            }
        }
        
        final class SequencingArrayConsumer extends SequencingConsumer implements Consumer<String[]> {
            String[] value;
            @Override public void accept(String[] value) { 
                csv.isTerminal = true;
                this.value = new String[2];
                this.value[0] = String.valueOf(sequenceNum);
                this.value[1] = Arrays.toString(value).trim();
            }
        }
    }
    
    /**
     * Implementation of the @FunctionalInterface {@link Header}
     * 
     * @author David Ray
     * @see Header
     */
    public static class BatchedCsvHeader implements ValueList, Serializable {
        
        private static final long serialVersionUID = 1L;
        
        /** Container for the field values */
        private Tuple[] headerValues;
                
        /**
         * Constructs a new {@code BatchedCsvHeader}
         * 
         * @param lines                     List of csv strings
         * @param configuredHeaderLength    number of header rows
         */
        public <T> BatchedCsvHeader(List<T> lines, int configuredHeaderLength) {
            
            if((configuredHeaderLength < 1 || lines == null || lines.size() < 1) || 
                (configuredHeaderLength > 1 && lines.size() != configuredHeaderLength)) {
                
                throw new IllegalStateException("Actual Header was not the expected size: " + 
                    (configuredHeaderLength < 1 ? "> 1" : configuredHeaderLength) + 
                        ", but was: " + (lines == null ? "null" : lines.size()));
            }
            
            headerValues = new Tuple[configuredHeaderLength];
            for(int i = 0;i < headerValues.length;i++) {
                headerValues[i] = new Tuple((Object[])lines.get(i));
            }
        }
        
        /**
         * Returns the array of values ({@link Tuple}) at the specified
         * index.
         * 
         * @param index     the index of the Tuple to be retrieved.
         * @return
         */
        public Tuple getRow(int index) {
            if(index >= headerValues.length) {
                return null;
            }
            return headerValues[index];
        }
        
        /**
         * Returns the current number of lines in the header.
         * 
         * @return
         */
        public int size() {
            return headerValues == null ? 0 : headerValues.length;
        }
        
        /**
         * {@inheritDoc}
         * @return
         */
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Stream.of(headerValues).forEach(l -> sb.append(l).append("\n"));
            return sb.toString();
        }
    }
    //////////////////   End Inner Classes  //////////////////////
    
    
    //////////////////////////////////////////////////////////////
    //                      Main Class                          //
    //////////////////////////////////////////////////////////////
    private static final transient Logger LOGGER = LoggerFactory.getLogger(BatchedCsvStream.class);
    
    private Iterator<String[]> it;
    private int fence;
    private boolean isBatchOp;
    private boolean isTerminal;
    private boolean isArrayType;
    private BatchedCsvHeader header;
    private transient Stream<T> delegate;
    private int headerStateTracker = 0;

    
    /**
     * Constructs a new {@code BatchedCsvStream}
     * 
     * @param s                 the underlying JDK {@link Stream}
     * @param headerLength      the number of header lines preceding the data.
     * @see Header
     */
    public BatchedCsvStream(Stream<String> s, int headerLength) {
        this.it = s.map(line -> { 
            ++headerStateTracker;
            return line.split("[\\s]*,[\\s]*", -1); 
        }).iterator();
        this.fence = headerLength;
        makeHeader();
        
        LOGGER.debug("Created BatchedCsvStream");
    }
    
    /**
     * Called internally to create this csv stream's header
     */
    private void makeHeader() {
        List<String[]> contents = new ArrayList<>();
        
        int i = 0;
        while(i++ < fence) {
            String[] h = it.next();
            contents.add(h);
        }
        this.header = new BatchedCsvHeader(contents, fence);
        this.isArrayType = isArrayType();
        
        if(LOGGER.isDebugEnabled()) {
            LOGGER.debug("Created Header:");
            for(String[] h : contents) {
                LOGGER.debug("\t" + Arrays.toString(h));
            }
            LOGGER.debug("Successfully created BatchedCsvHeader.");
        }
    }
    
    /**
     * <p>
     * Returns a flag indicating whether the underlying stream has had
     * a terminal operation called on it, indicating that it can no longer
     * have operations built up on it.
     * </p><p>
     * The "terminal" flag if true does not indicate that the stream has reached
     * the end of its data, it just means that a terminating operation has been
     * invoked and that it can no longer support intermediate operation creation.
     * 
     * @return  true if terminal, false if not.
     */
    @Override
    public boolean isTerminal() {
        return this.isTerminal;
    }
    
    /**
     * Returns a flag indicating whether this {@link Stream} is 
     * currently batching its operations.
     * 
     * @return
     */
    public boolean isBatchOp() {
        return isBatchOp;
    }
    
    /**
     * Sets a flag indicating that whether this {@code BatchedCsvStream} is
     * currently batching its operations.
     * 
     * @param b
     */
    public void setBatchOp(boolean b) {
        this.isBatchOp = b;
    }

    /**
     * Returns the {@link BatchedCsvHeader}
     * @return
     */
    public BatchedCsvHeader getHeader() {
        return header;
    }

    /**
     * Returns the portion of the {@link Stream} <em>not containing</em>
     * the header. To obtain the header, refer to: {@link #getHeader()}
     * 
     * @param parallel                      flag indicating whether the underlying
     *                                      stream should be parallelized.
     * @return the stream continuation
     * @see Header
     * @see BatchedCsvHeader
     * @see #getHeader()
     */
    private Stream<String[]> continuation(boolean parallel) {
        if(it == null) {
            throw new IllegalStateException("You must first create a BatchCsvStream by calling batch(Stream, int, boolean, int)");
        }
        
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                parallel ? it : isArrayType ? getArraySequenceIterator(it) : getSequenceIterator(it),   // Return a sequencing iterator if not parallel
                                                                                                        // otherwise the Spliterator handles the sequencing
                                                                                                        // through the special SequencingConsumer
                Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE), 
                parallel);
    }
    
    /**
     * Returns a flag indicating whether the input field is an array
     * @return
     */
    private boolean isArrayType() {
        if(getHeader().headerValues.length < 3) {
            return false;
        }
        for(Object o : getHeader().headerValues[1].all()) {
            if(o.toString().toLowerCase().equals("sarr") || o.toString().toLowerCase().equals("darr")) {
                return isArrayType = true;
            }
        }
        return false;
    }
    
    /**
     * Called internally to return a sequencing iterator when this stream
     * is configured to be non-parallel because it will skip the BatchedSpliterator
     * code which internally does the sequencing. So we must provide it here when
     * not parallel.
     * 
     * @param toWrap    the original iterator to wrap
     * @return
     */
    private Iterator<String[]> getSequenceIterator(final Iterator<String[]> toWrap) {
        return new Iterator<String[]>() {
            private Iterator<String[]> delegate = toWrap;
            private int seq = 0;
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String[] next() {
                isTerminal = true;
                String[] value = delegate.next();
                String[] retVal = new String[value.length + 1];
                System.arraycopy(value, 0, retVal, 1, value.length);
                retVal[0] = String.valueOf(seq++);
                
                return retVal;
            }
            
        };
    }
    
    /**
     * Called internally to return a sequencing iterator when this stream
     * is configured to be non-parallel because it will skip the BatchedSpliterator
     * code which internally does the sequencing. So we must provide it here when
     * not parallel.
     * 
     * This method differs from {@link #getSequenceIterator(Iterator)} by converting
     * the parsed String[] to a single string in the 2 index.
     * 
     * @param toWrap    the original iterator to wrap
     * @return
     */
    private Iterator<String[]> getArraySequenceIterator(final Iterator<String[]> toWrap) {
        return new Iterator<String[]>() {
            private Iterator<String[]> delegate = toWrap;
            private int seq = 0;
            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public String[] next() {
                isTerminal = true;
                String[] value = delegate.next();
                String[] retVal = new String[2];
                retVal[0] = String.valueOf(seq++);
                retVal[1] = Arrays.toString(value).trim();
                
                return retVal;
            }
            
        };
    }

    /**
     * Returns the delegate underlying {@link Stream}.
     * @return stream
     */
    @SuppressWarnings({ "unchecked" })
    public Stream<String[]> stream() {
        return (Stream<String[]>)this.delegate;
    }
    
    /**
     * Initializes the new spliterator using the specified characteristics.
     * 
     * @param csv                   the Stream from which to create the spliterator
     * @param batchSize             the "chunk" length to be processed by each Threaded task
     * @param isParallel            if true, batching will take place, otherwise not
     * @param characteristics       overrides the default characteristics of:
     *                              {@link Spliterator#ORDERED},{@link Spliterator#NONNULL},
     *                              {@link Spliterator#IMMUTABLE}, and <em>{@link Spliterator#SUBSIZED}.
     *                              <p><b>WARNING:</b> This last characteristic [<b>SUBSIZED</b>] is <b>necessary</b> if batching is desired.</p></em>
     * @return
     */
    private static <T> BatchSpliterator batchedSpliterator(
        BatchedCsvStream<String[]> csv, int batchSize, boolean isParallel, int characteristics) {
        
        Spliterator<String[]> toWrap = csv.continuation(isParallel).spliterator();
        return new BatchSpliterator(
            characteristics, batchSize, toWrap.estimateSize()).setCSV(csv).setToWrap(toWrap);
    }
    
    /**
     * Called internally to create the {@link BatchSpliterator} the heart and soul
     * of this class.
     * @param csv                   the Stream from which to create the spliterator
     * @param batchSize             the "chunk" length to be processed by each Threaded task
     * @param isParallel            if true, batching will take place, otherwise not
     * @return
     */
    private static <T> BatchSpliterator batchedSpliterator(
        BatchedCsvStream<String[]> csv, int batchSize, boolean isParallel) {
        
        Spliterator<String[]> toWrap = csv.continuation(isParallel).spliterator();
        return new BatchSpliterator(
            toWrap.characteristics(), batchSize, toWrap.estimateSize()).setCSV(csv).setToWrap(toWrap);
    }

    /**
     * Factory method to create a {@code BatchedCsvStream}. If isParallel is false,
     * this stream will behave like a typical stream. See also {@link BatchedCsvStream#batch(Stream, int, boolean, int, int)}
     * for more fine grained setting of characteristics.
     * 
     * @param stream                JDK Stream
     * @param batchSize             the "chunk" length to be processed by each Threaded task
     * @param isParallel            if true, batching will take place, otherwise not
     * @param headerLength          number of header lines
     * @return
     */
    public static BatchedCsvStream<String[]> batch(Stream<String> stream, int batchSize, boolean isParallel, int headerLength) {
        //Notice the Type of the Stream becomes String[] - This is an important optimization for 
        //parsing the sequence number later. (to avoid calling String.split() on each entry)
        //Initializes and creates the CsvHeader here:
        BatchedCsvStream<String[]> csv = new BatchedCsvStream<>(stream, headerLength);
        Stream<String[]> s = !isParallel ? csv.continuation(isParallel) : 
            StreamSupport.stream(batchedSpliterator(csv, batchSize, isParallel), isParallel);
        csv.delegate = s;
        return csv;
    }
    
    /**
     * Factory method to create a {@code BatchedCsvStream}.
     *  
     * @param stream                JDK Stream
     * @param batchSize             the "chunk" length to be processed by each Threaded task
     * @param isParallel            if true, batching will take place, otherwise not 
     * @param headerLength          number of header lines
     * @param characteristics       stream configuration parameters (see {@link Spliterator#characteristics()})
     * @return
     */
    public static BatchedCsvStream<String[]> batch(Stream<String> stream, int batchSize, boolean isParallel, int headerLength, int characteristics) {
        //Notice the Type of the Stream becomes String[] - This is an important optimization for 
        //parsing the sequence number later. (to avoid calling String.split() on each entry MULTIPLE TIMES (for the eventual sort))
        //Initializes and creates the CsvHeader here:
        BatchedCsvStream<String[]> csv = new BatchedCsvStream<>(stream, headerLength);
        Stream<String[]> s = !isParallel ? csv.continuation(isParallel) : 
            StreamSupport.stream(batchedSpliterator(csv, batchSize, isParallel, characteristics), isParallel);
        csv.delegate = s;
        return csv;
    }
    
    /**
     * Implements the {@link MetaStream} {@link FunctionalInterface} enabling
     * retrieval of stream meta information.
     */
    public ValueList getMeta() {
        return getHeader();
    }
    
    //////////////////////////////////////////////////////////////
    //          Overridden Methods from Parent Class            //
    //////////////////////////////////////////////////////////////
    @Override
    public Iterator<T> iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator<T> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public boolean isParallel() {
        return delegate.isParallel();
    }

    @Override
    public Stream<T> sequential() {
        return delegate.sequential();
    }

    @Override
    public Stream<T> parallel() {
        return delegate.parallel();
    }

    @Override
    public Stream<T> unordered() {
        return delegate.unordered();
    }

    @Override
    public Stream<T> onClose(Runnable closeHandler) {
        return delegate.onClose(closeHandler);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @Override
    public Stream<T> filter(Predicate<? super T> predicate) {
        return delegate.filter(predicate);
    }

    @Override
    public <R> Stream<R> map(Function<? super T, ? extends R> mapper) {
        return delegate.map(mapper);
    }

    @Override
    public IntStream mapToInt(ToIntFunction<? super T> mapper) {
        return delegate.mapToInt(mapper);
    }

    @Override
    public LongStream mapToLong(ToLongFunction<? super T> mapper) {
        return delegate.mapToLong(mapper);
    }

    @Override
    public DoubleStream mapToDouble(ToDoubleFunction<? super T> mapper) {
        return delegate.mapToDouble(mapper);
    }

    @Override
    public <R> Stream<R> flatMap(Function<? super T, ? extends Stream<? extends R>> mapper) {
        return delegate.flatMap(mapper);
    }

    @Override
    public IntStream flatMapToInt(Function<? super T, ? extends IntStream> mapper) {
        return delegate.flatMapToInt(mapper);
    }

    @Override
    public LongStream flatMapToLong(Function<? super T, ? extends LongStream> mapper) {
        return delegate.flatMapToLong(mapper);
    }

    @Override
    public DoubleStream flatMapToDouble(Function<? super T, ? extends DoubleStream> mapper) {
        return delegate.flatMapToDouble(mapper);
    }

    @Override
    public Stream<T> distinct() {
        return delegate.distinct();
    }

    @Override
    public Stream<T> sorted() {
        return delegate.sorted();
    }

    @Override
    public Stream<T> sorted(Comparator<? super T> comparator) {
        return delegate.sorted(comparator);
    }

    @Override
    public Stream<T> peek(Consumer<? super T> action) {
        return delegate.peek(action);
    }

    @Override
    public Stream<T> limit(long maxSize) {
        return delegate.limit(maxSize);
    }

    @Override
    public Stream<T> skip(long n) {
        return delegate.skip(n);
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        delegate.forEach(action);
    }

    @Override
    public void forEachOrdered(Consumer<? super T> action) {
        delegate.forEachOrdered(action);
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        return delegate.toArray(generator);
    }

    @Override
    public T reduce(T identity, BinaryOperator<T> accumulator) {
        return delegate.reduce(identity, accumulator);
    }

    @Override
    public Optional<T> reduce(BinaryOperator<T> accumulator) {
        return delegate.reduce(accumulator);
    }

    @Override
    public <U> U reduce(U identity, BiFunction<U, ? super T, U> accumulator, BinaryOperator<U> combiner) {
        return delegate.reduce(identity, accumulator, combiner);
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<R, ? super T> accumulator, BiConsumer<R, R> combiner) {
        return delegate.collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(Collector<? super T, A, R> collector) {
        return delegate.collect(collector);
    }

    @Override
    public Optional<T> min(Comparator<? super T> comparator) {
        return delegate.min(comparator);
    }

    @Override
    public Optional<T> max(Comparator<? super T> comparator) {
        return delegate.max(comparator);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public boolean anyMatch(Predicate<? super T> predicate) {
        return delegate.anyMatch(predicate);
    }

    @Override
    public boolean allMatch(Predicate<? super T> predicate) {
        return delegate.allMatch(predicate);
    }

    @Override
    public boolean noneMatch(Predicate<? super T> predicate) {
        return delegate.noneMatch(predicate);
    }

    @Override
    public Optional<T> findFirst() {
        return delegate.findFirst();
    }

    @Override
    public Optional<T> findAny() {
        return delegate.findAny();
    }
    
    public static void main(String[] args) {
        Stream<String> stream = Stream.of(
            "timestamp,consumption",
            "datetime,float",
            "T,",
            "7/2/10 0:00,21.2",
            "7/2/10 1:00,16.4",
            "7/2/10 2:00,4.7",
            "7/2/10 3:00,4.7",
            "7/2/10 4:00,4.6",
            "7/2/10 5:00,23.5",
            "7/2/10 6:00,47.5",
            "7/2/10 7:00,45.4",
            "7/2/10 8:00,46.1",
            "7/2/10 9:00,41.5",
            "7/2/10 10:00,43.4",
            "7/2/10 11:00,43.8",
            "7/2/10 12:00,37.8",
            "7/2/10 13:00,36.6",
            "7/2/10 14:00,35.7",
            "7/2/10 15:00,38.9",
            "7/2/10 16:00,36.2",
            "7/2/10 17:00,36.6",
            "7/2/10 18:00,37.2",
            "7/2/10 19:00,38.2",
            "7/2/10 20:00,14.1");

        @SuppressWarnings("resource")
        BatchedCsvStream<String> csv = new BatchedCsvStream<>(stream, 3);
        System.out.println("Header: " + csv.getHeader());
        csv.continuation(false).forEach(l -> System.out.println("line: " + Arrays.toString(l)));
    }
}


