package org.numenta.nupic.util;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.numenta.nupic.util.Tuple.Builder;

/**
 * Enables the use of {@link Tuple}s as {@link Collectors} for 
 * use with Java 8 Streams (see {@link Stream})
 * @author cogmission
 * @see Tuple.Builder
 */
public class TupleCollector 
    implements Collector<Object, Tuple.Builder, Tuple> {

    @Override
    public Supplier<Builder> supplier() {
        return Tuple::builder;
    }

    @Override
    public BiConsumer<Builder, Object> accumulator() {
        return (builder, o) -> builder.add(o);
    }

    @Override
    public BinaryOperator<Builder> combiner() {
        return (left, right) -> {
            left.addAll(right);
            return left;
        };
    }

    @Override
    public Function<Builder, Tuple> finisher() {
        return Tuple.Builder::build;
    }

    @Override
    public Set<java.util.stream.Collector.Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
    }

}
