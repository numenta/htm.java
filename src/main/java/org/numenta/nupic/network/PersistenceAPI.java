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

import org.numenta.nupic.Persistable;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;

public interface PersistenceAPI extends Persistable {
    /**
     * Stores this {@link Network} at the pre-configured location, after
     * halting and shutting down the Network. To store the Network but keep it up
     * and running, please see the {@link #checkpoint()} method. 
     * 
     * The Network, may however be {@link #restart()}ed after this method is called.
     * 
     * @return the serialized Network in the format specified (byte[] is the default).
     */
    public <T> T store();
    /**
     * Returns the bytes of the last checkpointed {@code Network}
     * @return      the bytes of the last checkpointed {@code Network}    
     */
    public byte[] getCheckPoint();
    /**
     * Stores the state of this {@code Network} while keeping the Network up and running.
     * The Network will be stored at the pre-configured location (in binary form only, not JSON).
     */
    public CheckPointer<byte[]> checkPointer();
    /**
     * Loads a {@code Network} from the default or previously configured location and
     * serial file, and returns it.
     * @return the deserialized Network
     */
    public static Network load() { return null; }
    /**
     * Loads a {@code Network} from the specified serialized file name and
     * returns it.
     *  
     * @param serializedBytes             the name of the serialization file.
     *    
     * @return  returns the specified Network
     */
    public static Network load(byte[] serializedBytes) { return null; }
    /**
     * Loads a {@code Network} from the specified serialized file name and
     * returns it.
     *  
     * @param fileName      the name of the serialization file.
     *    
     * @return  returns the specified Network
     */
    public static Network load(String fileName) throws IOException { return null; }
    /**
     * Returns the name of the most recently checkpointed {@code Network} file.
     * @return  the name of the most recently checkpointed {@code Network} file.
     */
    public String getLastCheckPointFileName();
    /**
     * Restarts this {@code Network}. The network will run from the previous save point
     * of the stored Network.
     * 
     * @see {@link #restart(boolean)} for a start at "saved-index" behavior explanation. 
     */
    public void restart();
    /**
     * Restarts this {@code Network}. If the "startAtIndex" flag is true, the Network
     * will start from the last record number (plus 1) at which the Network was saved -
     * continuing on from where it left off. The Network will achieve this by rebuilding
     * the underlying Stream (if necessary, i.e. not for {@link ObservableSensor}s) and skipping 
     * the number of records equal to the stored record number plus one, continuing from where it left off.
     * 
     * @param startAtIndex  flag indicating whether to start this {@code Network} from
     *                      its previous save point.
     */
    public void restart(boolean startAtIndex);
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
    public static <T extends Persistable> NetworkSerializer<T> serializer(SerialConfig config, boolean returnNew) { return null; }
    /**
     * Returns the new {@link Publisher} created after halt or deserialization
     * of this {@code Network}, when a new Publisher must be created.
     * 
     * @return      the new Publisher created after deserialization or halt.
     * @see #getPublisherSupplier()
     */
    public Publisher getPublisher();
}
