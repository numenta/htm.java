package org.numenta.nupic.util;

/**
 * Implemented to be used as arguments in other operations.
 * see {@link ArrayUtils#retainLogicalAnd(int[], Condition[])};
 * {@link ArrayUtils#retainLogicalAnd(double[], Condition[])}.
 */
public interface Condition<T> {
    /**
     * Convenience adapter to remove verbosity
     * @author metaware
     *
     */
    public class Adapter<T> implements Condition<T> {
        public boolean eval(int n) { return false; }
        public boolean eval(double d) { return false; }
        public boolean eval(T t) { return false; }
    }
    public boolean eval(int n);
    public boolean eval(double d);
    public boolean eval(T t);
}