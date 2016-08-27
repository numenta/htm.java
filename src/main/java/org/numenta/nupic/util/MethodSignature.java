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
public class MethodSignature extends MutableNamedTuple {
    
    private static final long serialVersionUID = 1L;

    public MethodSignature() {
        
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
     *      someMethod(ms.addParam(a, "A"), ms.addParam(b, "B"));
     *      
     *      -- OR --
     *      
     *      public void someMethod(MethodSignature ms) {}
     *      someMethod(new MethodSignature(2).setParams(new String[] { "name1", "name2" }, value1, value2));
     *      
     *      -- OR --
     *      
     *      public void someMethod(Tuple t) {}
     *      someMethod(new MethodSignature(2).setParams(new String[] { "name1", "name2" }, value1, value2)));
     * </pre>
     * 
     * @param t         the value 
     * @param name      the name of the parameter
     * 
     * @return
     */
    public <T> T addParam(T t, String name) {
        super.put(name, t);
        return t;
    }
    
    /**
     * Allows the setting of contents within this object in the call
     * site of a method which takes an instance of this {@link MethodSignature}
     * object.
     * 
     * @param paramNames    the parameter names
     * @param objects       the parameter values  
     * @return  this {@code MethodSignature}
     * @see #setParam(Object, int)
     */
    public MethodSignature setParams(String[] paramNames, Object... objects) {
        remake(paramNames, objects);
        return this;
    }
}
