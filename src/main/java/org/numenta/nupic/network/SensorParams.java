package org.numenta.nupic.network;

import java.util.function.Supplier;

import org.numenta.nupic.datagen.ResourceLocator;
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
 * </p>
 * 
 * @author David Ray
 */
public class SensorParams extends NamedTuple {
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
    
    private SensorParams(Supplier<Keys.Args> keySet, Object... values) {
        super(keySet.get().get(), values);
    }
    
    public static SensorParams create(Supplier<Keys.Args> keySet, Object... values) {
        return new SensorParams(keySet, values);
    }
    
    public static void main(String[] args) {
        Object[] n = { "rec-center-hourly", ResourceLocator.locate("rec-center-hourly") };
        SensorParams parms = SensorParams.create(Keys::uri, n);
        assert(parms != null);
    }
}


