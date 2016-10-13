/* ---------------------------------------------------------------------
 * Numenta Platform for Intelligent Computing (NuPIC)
 * Copyright (C) 2016, Numenta, Inc.  Unless you have an agreement
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
package org.numenta.nupic.network;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.serialize.SerialConfig;
import org.numenta.nupic.serialize.SerializerCore;

/**
 * <p>
 * Offers 4 basic types of functionality:
 * </p>
 * <ul>
 *  <li>Obtain an instance of the low-level serializer</li>
 *  <li>Provide convenience methods for storing a {@link Network}</li>
 *  <li>Provide a method to <b>Check Point</b> a {@link Network} (snapshot it to the local file system).
 *  <li>Provide generic methods to store any HTM object which is {@link Persistable}</li>
 * </ul>
 * </p>
 * <p>
 * While the generic methods will work for the entire {@link Network} (since it is {@link Persistable}),
 * there are convenience methods to store a Network specifically. These come in the form of "store" and 
 * "load" methods:
 * <ul>
 *  <li>{@link #store(Network)}</li> 
 *  <li>{@link #storeAndGet(Network)}</li>
 *  <li>{@link #load()}</li>
 *  <li>{@link #load(String)}</li>
 * </ul>
 * </p>
 * <p>
 * The more generic persistence methods are of the form "write" and "read" as in:
 * <ul>
 *  <li>{@link #write(Persistable)}</li>
 *  <li>{@link #write(Persistable, String)}</li>
 *  <li>{@link #read(byte[])}</li>
 *  <li>{@link #read(String)}</li>
 * </ul>
 * </p>
 * <p>
 * To obtain an instance of the {@code PersistanceAPI}, simply call:
 * <pre>
 * PersistenceAPI api = Persistence.get();
 * </pre>
 * </p>
 * <p>
 * Although the PersistenceAPI is adequate for the majority of cases, you may also obtain an
 * instance of the underlying serializer (see {@link SerializerCore}) that the PersistenceAPI itself uses:
 * <pre>
 * SerializerCore core = Persistence.get().serializer();
 * </pre>
 * The core serializer can be used to serialize to and from an Input/Output stream, or to and from
 * byte arrays.
 * </p>
 * 
 * @author cogmission
 * @see SerializerCore
 */
public interface PersistenceAPI extends Serializable {
    /**
     * Factory method to return a configured {@link NetworkSerializer}
     * 
     * If the "returnNew" flag is true, this method returns a new instance of 
     * {@link NetworkSerializer} and stores it for subsequent invocations of this
     * method. If false, the previously stored NetworkSerializer is returned.
     * 
     * @param config        the SerialConfig storing file storage parameters etc.
     * @param returnNew     NetworkSerializers are expensive to instantiate so specify
     *                      if the previous should be re-used or if you want a new one.
     * @return      a NetworkSerializer
     * @see SerialConfig
     */
    public <T extends Persistable> SerializerCore serializer();
    /**
     * Convenience method to load a {@code Network} from the default or previously configured
     * location and serial file, and returns it. 
     * @return the deserialized Network
     * @see SerialConfig
     */
    public Network load();
    /**
     * Convenience method to load a {@code Network} from the specified serialized file name and
     * returns it.
     *  
     * @param fileName      the name of the serialization file.
     *    
     * @return  returns the specified Network
     * @see SerialConfig
     */
    public Network load(String fileName) throws IOException;
    /**
     * Convenience method to store the specified {@link Network} to the pre-configured 
     * (with {@link SerialConfig}) location and filename.
     * @param network   the {@code Network} to store
     */
    public void store(Network network);
    /**
     * Stores the specified {@link Network} at the pre-configured location, after
     * halting and shutting down the Network. To store the Network but keep it up
     * and running, please see the {@link #checkPointer()} method. 
     * 
     * The Network, may however be {@link #restart()}ed after this method is called.
     * 
     * @param <R> the type of the returned object
     * @param network       the {@link Network} to persist
     * @param returnBytes   flag indicating whether to return the interim byte array
     * 
     * @return the serialized Network in the format is either a byte[] or String (json),
     *          where byte[] is the default of type &lt;R&gt;
     */
    public <R> R storeAndGet(Network network);
    /**
     * Returns an {@link rx.Observable} operator that when subscribed to, invokes an operation
     * that stores the state of this {@code Network} while keeping the Network up and running.
     * The Network will be stored at the pre-configured location (in binary form only, not JSON).
     * 
     * @param <T> the type of the stored object
     * @param network   the {@link Network} to check point
     * @return the {@link CheckPointOp} operator of type &lt;T&gt;
     */
    public CheckPointOp<byte[]> checkPointer(Network network);
    /**
     * Reifies a {@link Persistable} from the specified file in the location and file name
     * configured by the {@link SerialConfig} passed in at construction time.
     * 
     * @return  the reified type &lt;R&gt;
     */
    public <R extends Persistable> R read();
    /**
     * Reifies a {@link Persistable} from the specified file in the location
     * configured by the {@link SerialConfig} passed in at construction time.
     * 
     * @param <R> the type of the returned serialized form
     * @param fileName  the name of the file from which to get the serialized object.
     * @return  the reified type &lt;R&gt;
     */
    public <R extends Persistable> R read(String fileName);    
    /**
     * Loads a {@code Persistable} from the specified serialized byte array and
     * returns the de-serialized Persistable.
     *  
     * @param <R> the type of the returned serialized form
     * @param serializedBytes             the name of the serialization file.
     *    
     * @return  the reified type &lt;R&gt;
     */
    public <R extends Persistable> R read(byte[] serializedBytes);
    /**
     * Persists the {@link Persistable} subclass to the file system using the 
     * pre-configured {@link SerialConfig} specified at the time this object was
     * instantiated, or the default SerialConfig.
     * 
     * @param <T> the type of the stored object
     * @param <R> the type of the returned serialized form
     * @param instance  the subclass of Persistable to persist.
     * @return  a byte array containing the serialized object of type &lt;R&gt;
     */
    public <T extends Persistable, R> R write(T instance);
    /**
     * Persists the {@link Persistable} subclass to the file system using the 
     * pre-configured {@link SerialConfig} specified at the time this object was
     * instantiated, or the default SerialConfig.
     * 
     * @param <T> the type of the stored object
     * @param <R> the type of the returned serialized form
     * @param instance  the subclass of Persistable to persist.
     * @param fileName  the name of the file to which the object is stored.
     * @return  a byte array containing the serialized object of type &lt;R&gt;
     */
    public <T extends Persistable, R> R write(T instance, String fileName);
    /**
     * (optional)
     * Sets the {@link SerialConfig} for detailed control. In common practice
     * this object is initialized with a default that is fine.
     * @param config    
     */
    public void setConfig(SerialConfig config);
    /**
     * Returns the {@link SerialConfig} in use
     * @return  the SerialConfig in current use
     */
    public SerialConfig getConfig();
    
    /////////////////////////////////////////
    //        Convenience Methods          //
    /////////////////////////////////////////
    /**
     * Returns the last check pointed bytes of the last check point operation.
     * 
     * @return  a byte array
     */
    public byte[] getLastCheckPoint();
    /**
     * Returns the name of the most recently checkpointed {@code Network} file.
     * @return  the name of the most recently checkpointed {@code Network} file.
     */
    public String getLastCheckPointFileName();
    /**
     * Returns a {@code List} of check pointed file names.
     * @return a {@code List} of check pointed file names.
     */
    public List<String> listCheckPointFiles();
    /**
     * Returns the checkpointed file previous to the specified file (older), or
     * null if one doesn't exist. The file name may be the entire filename (as
     * configured by the {@link SerialConfig} object which establishes both the
     * filename portion and the date portion formatting), or just the date
     * portion of the filename.
     * 
     * @param   checkPointFileName (can be entire name or just date portion)
     * 
     * @return  the full filename of the file checkpointed immediately previous
     *          to the file specified.
     */
    public String getPreviousCheckPoint(String checkPointFileName);
    /**
     * Convenience method which returns the store file fully qualified path. 
     * @return
     */
    public String currentPath();
    
}
