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

package org.numenta.nupic.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Immutable tuple which adds associative lookup functionality.
 * 
 * @author David Ray
 */
public class NamedTuple extends Tuple {
    Bucket[] entries;
    String[] keys;
    
    int hash;
    int thisHashcode;
    
    private static final String[] EMPTY_KEYS = {};
    
    /**
     * Constructs and new {@code NamedTuple}
     * 
     * @param keys      
     * @param objects
     */
    public NamedTuple(String[] keys, Object... objects) {
        super(interleave(keys, objects));
        
        if(keys.length != objects.length) {
            throw new IllegalArgumentException("Keys and values must be same length.");
        }
        
        this.keys = keys;
        
        entries = new Bucket[keys.length * 2];
        for(int i = 0;i < entries.length;i++) {
            entries[i] = new Bucket(i);
        }
        
        for(int i = 0;i < keys.length;i++) {
            addEntry(keys[i], objects[i]);
        }
        
        this.thisHashcode = hashCode();
    }
    
    /**
     * Returns a array copy of this {@code NamedTuple}'s keys.
     * @return
     */
    public String[] keys() {
        if(keys == null || keys.length < 1) return EMPTY_KEYS;
        
        return Arrays.copyOf(keys, keys.length);
    }
    
    /**
     * Returns a Collection view of the values of this {@code NamedTuple}
     * @return
     */
    public Collection<Object> values() {
        List<Object> retVal = new ArrayList<>();
        for(int i = 1;i < all().size();i+=2) {
            retVal.add(all().get(i));
        }
        return retVal;
    }
    
    /**
     * Returns the Object corresponding with the specified
     * key.
     * 
     * @param key   the identifier with the same corresponding index as 
     *              its value during this {@code NamedTuple}'s construction.
     * @return
     */
    public Object get(String key) {
        if(key == null) return null;
        
        int hash = hashIndex(key);
        Entry e = entries[hash].find(key, hash);
        return e == null ? null : e.value;
    }
    
    /**
     * Returns a flag indicating whether the specified key
     * exists within this {@code NamedTuple}
     * 
     * @param key
     * @return
     */
    public boolean hasKey(String key) {
        int hash = hashIndex(key);
        Entry e = entries[hash].find(key, hash);
        return e != null;
    }
    
    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0;i < entries.length;i++) {
            sb.append(entries[i].toString());
        }
        return sb.toString();
    }
    
    /**
     * Creates an {@link Entry} with the hashed key value, checking 
     * for duplicates (which aren't allowed during construction).
     * 
     * @param key       the unique String identifier
     * @param value     the Object corresponding to the specified key
     */
    private void addEntry(String key, Object value) {
        int hash = hashIndex(key);
        Entry e;
        if((e = entries[hash].find(key, hash)) != null && e.key.equals(key)) {
            throw new IllegalStateException(
                "Duplicates Not Allowed - Key: " + key + ", reinserted.");
        }
        
        Entry entry = new Entry(key, value, hash);
        entries[hash].add(entry);
    }
    
    /**
     * Creates and returns a hash code conforming to a number
     * between 0 - n-1, where n = #Buckets
     * 
     * @param key   String to be hashed.
     * @return
     */
    private int hashIndex(String key) {
        return Math.abs(key.hashCode()) % entries.length;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        if(hash == 0) {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + Arrays.hashCode(entries);
            hash = result;
        }
        return hash;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(getClass() != obj.getClass())
            return false;
        if(!super.equals(obj))
            return false;
        NamedTuple other = (NamedTuple)obj;
        if(this.thisHashcode != other.thisHashcode)
            return false;
        return true;
    }

    /**
     * Encapsulates the hashed key/value pair in a linked node.
     */
    private final class Entry {
        String key;
        Object value;
        int hash;
        Entry prev;
        
        /**
         * Constructs a new {@code Entry}
         * 
         * @param key
         * @param value
         * @param hash
         */
        public Entry(String key, Object value, int hash) {
            this.key = key;
            this.value = value;
            this.hash = hashIndex(key);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return new StringBuilder("key=").append(key)
                .append(", value=").append(value)
                    .append(", hash=").append(hash).toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + hash;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Entry other = (Entry)obj;
            if(hash != other.hash)
                return false;
            if(key == null) {
                if(other.key != null)
                    return false;
            } else if(!key.equals(other.key))
                return false;
            if(value == null) {
                if(other.value != null)
                    return false;
            } else if(!value.equals(other.value))
                return false;
            return true;
        }
    }
    
    /**
     * Rudimentary (light-weight) Linked List implementation for storing
     * hash {@link Entry} collisions.
     */
    private final class Bucket {
        Entry last;
        int idx;
        
        /**
         * Constructs a new {@code Bucket}
         * @param idx   the identifier of this bucket for debug purposes.
         */
        public Bucket(int idx) {
            this.idx = idx;
        }
        
        /**
         * Adds the specified {@link Entry} to this Bucket.
         * @param e
         */
        private void add(Entry e) {
            if(last == null) {
                last = e;
            }else{
                e.prev = last;
                last = e;
            }
        }
        
        /**
         * Searches for an {@link Entry} with the specified key,
         * and returns it if found and otherwise returns null.
         * 
         * @param key       the String identifier corresponding to the
         *                  hashed value
         * @param hash      the hash code.
         * @return
         */
        private Entry find(String key, int hash) {
            if(last == null) return null;
            
            Entry found = last;
            while(found.prev != null && !found.key.equals(key)) {
                found = found.prev;
                if(found.key.equals(key)) {
                    return found;
                }
            }
            return found.key.equals(key) ? found : null;
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("Bucket: ").append(idx).append("\n");
            Entry l = last;
            while(l != null) {
                sb.append("\t").append(l.toString()).append("\n");
                l = l.prev;
            }
          
            return sb.toString();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + idx;
            result = prime * result + ((last == null) ? 0 : last.hashCode());
            return result;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;
            if(obj == null)
                return false;
            if(getClass() != obj.getClass())
                return false;
            Bucket other = (Bucket)obj;
            if(idx != other.idx)
                return false;
            if(last == null) {
                if(other.last != null)
                    return false;
            } else if(!last.equals(other.last))
                return false;
            return true;
        }
    }

    /**
     * Returns an array containing the successive elements of each
     * argument array as in [ first[0], second[0], first[1], second[1], ... ].
     * 
     * Arrays may be of zero length, and may be of different sizes, but may not be null.
     * 
     * @param first     the first array
     * @param second    the second array
     * @return
     */
    static <F, S> Object[] interleave(F first, S second) {
        int flen, slen;
        Object[] retVal = new Object[(flen = Array.getLength(first)) + (slen = Array.getLength(second))];
        for(int i = 0, j = 0, k = 0;i < flen || j < slen;) {
            if(i < flen) {
                retVal[k++] = Array.get(first, i++);
            }
            if(j < slen) {
                retVal[k++] = Array.get(second, j++);
            }
        }
        
        return retVal;
    }
}
