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

import java.util.function.Supplier;

import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.util.NamedTuple;

/**
 * <p>
 * Implementation of named parameter tuples that is tightly
 * keyed to only known keys in order to assist with proper
 * formation and auto creation within a {@link Network}.
 * </p>
 * <p>
 * To retrieve a {@code Keys.Args} from this {@code SensorParams}
 * for the purpose of construction follow this pattern for usage:
 * <p>
 * <pre>
 *  Object[] n = { "rec-center-hourly", ResourceLocator.locate("rec-center-hourly") };
    SensorParams parms = SensorParams.create(Keys::uri, n); // May be (Keys::path, Keys::obs) also
 * </pre>
 * 
 * 
 * @author David Ray
 */
public class SensorParams extends NamedTuple {
    
    private static final long serialVersionUID = 1L;

    /**
     * Convenience class to use as handle way to specify an expected
     * String array of key values for each of the 3 known input configuration
     * types.
     */
    public static class Keys {
        public enum Args {
            u(new String[] { "FILE", "URI" }),
            p(new String[] { "FILE", "PATH" }),
            o(new String[] { "NAME", "ONSUB" });
            private String[] arr;
            private Args(String[] s) {
                this.arr = s;
            }
            public String[] get() { return arr; }
        }
        public static Args uri() {
            return Args.u;
        }
        public static Args path() {
            return Args.p;
        }
        public static Args obs() {
            return Args.o;
        }
    }
    
    /**
     * Takes a String array of keys (via {@link Supplier#get()} and a varargs 
     * array of their values, creating key/value pairs. In this case, the keys are 
     * all predetermined to be one of the {@link Keys.Args} types which specify the 
     * keys which are to be used.
     * 
     * @param keySet       a Supplier yielding a particular set of String keys
     * @param values       the values correlated with the specified keys.
     */
    private SensorParams(Supplier<Keys.Args> keySet, Object... values) {
        super(keySet.get().get(), values);
    }
    
    /**
     * Takes a String array of keys (via {@link Supplier#get()} and a varargs 
     * array of their values, creating key/value pairs. In this case, the keys are 
     * all predetermined to be one of the {@link Keys.Args} types which specify the 
     * keys which are to be used.
     * 
     * @param keys         a String array of keys
     * @param values       the values correlated with the specified keys.
     */
    private SensorParams(String[] keys, Object... values) {
        super(keys, values);
    }
    
    /**
     * Factory method to create a {@code SensorParams} object which indicates
     * information used to connect a {@link Sensor} to its source.
     * 
     * @param keySet       a Supplier yielding a particular set of String keys
     * @param values       the values correlated with the specified keys.
     * 
     * @return  a SensorParams configuration
     */
    public static SensorParams create(Supplier<Keys.Args> keySet, Object... values) {
        return new SensorParams(keySet, values);
    }
    
    /**
     * Factory method to create a {@code SensorParams} object which indicates
     * information used to connect a {@link Sensor} to its source.
     * 
     * @param keys         a String array of keys
     * @param values       the values correlated with the specified keys.
     * 
     * @return  a SensorParams configuration
     */
    public static SensorParams create(String[] keys, Object... values) {
        return new SensorParams(keys, values);
    }
    
    public static void main(String[] args) {
        Object[] n = { "rec-center-hourly", ResourceLocator.locate("rec-center-hourly") };
        SensorParams parms = SensorParams.create(Keys::uri, n);
        assert(parms != null);
    }
}


