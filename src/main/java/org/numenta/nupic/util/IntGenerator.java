package org.numenta.nupic.util;

/**
 * Generates a range of integers.
 * 
 * @author cogmission
 */
public class IntGenerator implements Generator<Integer> {
    /** serial version */
    private static final long serialVersionUID = 1L;
    
    protected int _i;
    protected int lower;
    protected int upper;
    
    public IntGenerator(int lower, int upper) {
        this.lower = lower;
        this._i = this.lower;
        this.upper = upper;
    }
    
    /**
     * Returns the value returned by the last call to {@link #next()}
     * or the initial value if no previous call to {@code #next()} was made.
     * @return
     */
    public int get() {
        return _i;
    }
    
    /**
     * Returns the configured size or distance between the initialized
     * upper and lower bounds.
     * @return
     */
    public int size() {
        return upper - lower;
    }
    
    /**
     * Returns the state of this generator to its initial state so 
     * that it can be reused.
     */
    public void reset() {
        this._i = lower;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Integer next() {
        int retVal = _i;
        _i = ++_i > upper ? upper : _i;
        return retVal;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() { return _i < upper; }
    
    /**
     * Returns a {@link Generator} which returns integers between
     * the values specified (lower inclusive, upper exclusive)
     * @param lower     the lower bounds or start value
     * @param upper     the upper bounds (exclusive)
     * @return
     */
    public static IntGenerator of(int lower, int upper) {
        return new IntGenerator(lower, upper);
    }
}

