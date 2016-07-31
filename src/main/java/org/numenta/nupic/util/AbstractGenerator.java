/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
 * with Numenta, Inc., for a separate license for this software code, the
 * following terms and conditions apply:
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License version 3 as
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
package org.numenta.nupic.util;

import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Java version of a Python Generator. This generator has no "send"
 * method but does have yield. Values are consumed by either a forEach
 * loop or by calling {@link #next()} directly.
 * <p>
 * Typical Usage:
 * </p>
 * <p>
 * Define a class which overrides {@code AbstractGenerator} as such:
 * </p>
 * <b>For terminable generator...</b>
 * <pre>
 * class MyGenerator extends AbstractGenerator<Integer> {
 *     public void exec() {
 *         int i = 0;
 *         while(i < 50) {
 *             // Do some work then call yield
 *             yield(new Integer(i++));  // Execution "pauses" here after yield() is executed...
 *             
 *             // Do some more work...   // Execution continues here after call to next()...
 *         }
 *     }
 *      
 *     public boolean isConsumed() { 
 *         return false; // "false" makes this an "infinite" generator.
 *     }
 * }
 * 
 * MyGenerator generator = new MyGenerator();
 * while(generator.hasNext()) {
 *     System.out.println(generator.next());
 * }
 * </pre>
 * 
 * -----------------
 * 
 * <b>For infinite generator...</b>
 * <pre>
 * class MyGenerator extends AbstractGenerator<Integer> {
 *     public void exec() {
 *         int i = 0;
 *         while(true) {
 *             if(isHalted()) {
                   break;
               }
               
 *             // Do some work then call yield
 *             yield(new Integer(i++));  // Execution "pauses" here after yield() is executed...
 *             
 *             // Do some more work...   // Execution continues here after call to next()...
 *         }
 *     }
 *      
 *     public boolean isConsumed() { 
 *         return false; // "false" makes this an "infinite" generator.
 *     }
 * }
 * 
 * MyGenerator generator = new MyGenerator();
 * while(generator.hasNext()) {
 *     System.out.println(generator.next());
 * }
 * </pre>
 * 
 * @author cogmission
 *
 * @param <T> the return type of the generator
 */
public abstract class AbstractGenerator<T> implements Iterable<T>, Iterator<T>, Serializable  {
    /** Default serial id*/
    protected static final long serialVersionUID = 1L;

    /** The object of type &lt;T&gt; returned from {@link #yield(Object)}*/
    private T returnVal;
    
    /** The {@link Runnable} code body containing the call to {@link #exec} */
    private Runnable body;
    
    /** Signals the {@link #yield} point of execution halting */
    private CyclicBarrier barrier = new CyclicBarrier(2);
    
    /** Set to true during generator initialization */
    private volatile boolean running;
    
    /** Transfers the execution result from the inner thread to the caller */
    private LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>();
    
    /** The inner execution thread */
    private Thread mainThread;
    
    
    /**
     * Constructs a new {@code AbstractGenerator}
     */
    public AbstractGenerator() {
        body = () -> {
            while(running) {
                try {
                    exec();
                }catch(Exception e) {
                    //e.printStackTrace();
                    System.out.println("Was interrupted");
                }
            }
        };
    }
    
    /**
     * Override this method in a subclass to execute the
     * body of code which will do the processing during
     * every iteration. This method contains the call to
     * {@link #yield(Object)} which stores the processed
     * state for retrieval on the next call to {@link #next()}.
     */
    public abstract void exec();
    
    /**
     * Overridden to identify when this generator has
     * concluded its processing. For infinite generators
     * simply return "false" here.
     * 
     * @return  a flag indicating whether the last iteration
     *          was the last processing cycle.  
     */
    public abstract boolean isConsumed();
    
    /**
     * Called during the execution of the {@link #exec()}
     * method to signal the availability of the result from
     * one iteration of processing.
     *  
     * @param t     the object of type &lt;T&gt; to return
     */
    protected void yield(T t) {
        returnVal = t;
        try {
            queue.offer(t);
            barrier.await();
        }catch(Exception e) {
            // Halted execution ends here
        }
    }
    
    /**
     * Returns a flag indicating whether another iteration
     * of processing may occur.
     * 
     * @return  true if so, false if not
     */
    public boolean hasNext() {
        return !isConsumed();
    }
    
    /**
     * Halts the main thread
     */
    protected void halt() {
        running = false;
        mainThread.interrupt();
    }
    
    /**
     * Used by the main {@link #exec} loop to query if
     * the execution is to be stopped.
     * @return      true if halt requested, false if not
     */
    protected boolean haltRequested() {
        return !running;
    }
    
    /**
     * Returns the object of type &lt;T&gt; which is the
     * result of one iteration of processing.
     * 
     * @return   the object of type &lt;T&gt; to return
     */
    public T next() {
        if(!running) {
            running = true;
            mainThread = new Thread(body);
            mainThread.setDaemon(true);
            mainThread.start();
        } else {
            try { barrier.await(); }catch(Exception e) { e.printStackTrace(); }
        }
        
        try { returnVal = queue.take(); }catch(Exception e) { e.printStackTrace(); }
        
        return returnVal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Iterator<T> iterator() {
        return this;
    }
    
}
