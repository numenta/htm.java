package org.numenta.nupic.util;

import java.util.Collection;
import java.util.stream.IntStream;

public class MutableNamedTuple extends NamedTuple {
    /** for serialization */
    private static final long serialVersionUID = 1L;
    
    public MutableNamedTuple() {}
    
    public MutableNamedTuple(String[] keys, Object[] objects) {
        super(keys, objects);
    }

    /**
     * Sets the value at the specified index to be the
     * indicated object.
     * 
     * @param index     the index at which to set the specified object
     * @param o         the new object to store
     */
    public void put(String key, Object o) {
        if(hasKey(key)) {
            // Swap the value for the specified key in the parent Tuple container
            int containerKeyIdx = IntStream.range(0, container.length).filter(i -> container[i].equals(key)).findFirst().getAsInt();
            container[containerKeyIdx + 1] = o;
            
            // Swap the value in the hashed buckets
            int hash = hashIndex(key);
            Entry e = entries[hash].find(key, hash);
            e.value = o;
        }else{
            String[] keys = keys();
            Collection<Object> vals = values();
            String[] newKeys = new String[keys.length + 1];
            System.arraycopy(keys, 0, newKeys, 0, keys.length);
            newKeys[newKeys.length - 1] = key;
            vals.add(o);
            remake(newKeys, vals.toArray());
        }
    }
    
    /**
     * Clears the contained data from this {@code MutableTuple}
     */
    public void clear() {
        for(int i = 0;i < container.length;i++) container[i] = null;
    }
}
