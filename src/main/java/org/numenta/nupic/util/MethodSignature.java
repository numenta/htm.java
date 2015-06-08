package org.numenta.nupic.util;

/**
 * <p>
 * {@link Tuple} type that allows the setting of method parameters
 * while setting those parameters on this object - in the same statement.
 * </p><p>
 * This class is handy to avoid instantiating new {@link Tuple}s during highly
 * iterative processes.
 * </p>
 * 
 * @author David Ray
 * @see MutableTuple
 * @see NamedTuple
 * @see Tuple
 * @see #setParam(Object, int)
 */
public class MethodSignature extends MutableTuple {
    public MethodSignature(int maxFields) {
        super(maxFields);
    }
    
    /**
     * <p>
     * Special setter which allows the setting of fields within
     * this object while specifying the parameters of a method's 
     * call site. It does this by returning the value that the 
     * {@link #setParam(Object, int)} method is called with.
     * </p><p>
     * <pre>
     *      public void someMethod(A a, B b) {}
     *      
     *      MethodSignature ms = new MethodSignature(2);
     *      
     *      A a = new A();
     *      B b = new B();
     *      someMethod(ms.setParam(a, 0), ms.setParam(b, 1));
     *      
     *      -- OR --
     *      
     *      public void someMethod(MethodSignature ms) {}
     *      someMethod(new MethodSignature(2).setParams(value1, value2));
     *      
     *      -- OR --
     *      
     *      public void someMethod(Tuple t) {}
     *      someMethod(new MethodSignature(2).setParams(value1, value2));
     *      
     *      -- OR --
     *      // In a loop...
     *      MethodSignature ms = new MethodSignature(2);
     *      for(int i = 0;i < [some length];i++) {
     *          someMethod(ms.setParams(newVal1, getNewVal2()));
     *      }
     * </pre>
     * 
     * @param t         the value 
     * @param index
     * @return
     */
    public <T> T setParam(T t, int index) {
        super.set(index, t);
        return t;
    }
    
    /**
     * Allows the setting of contents within this object in the call
     * site of a method which takes an instance of this {@link MethodSignature}
     * object or its super class antecedents ({@link MutableTuple}, {@link Tuple})
     * 
     * @param objects
     * @return
     * @see #setParam(Object, int)
     */
    public MethodSignature setParams(Object... objects) {
        System.arraycopy(objects, 0, container, 0, objects.length);
        return this;
    }
}
