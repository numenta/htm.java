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

import org.numenta.nupic.Persistable;
import org.numenta.nupic.network.sensor.ObservableSensor;
import org.numenta.nupic.network.sensor.Publisher;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;

/**
 * <p>
 * <b>NOTE: The static methods are in this interface for reference only! Do not call them from here, but rather call
 * them from the only class that implements them ({@link Network}). The methods are here so all relevant functions
 * can be viewed in one central location to get a feel for the functional extent of the API.</b>
 * </p>
 * <p>
 * A reference interface for encapsulating the functionality specific to the persistence 
 * of HTM.java {@link Network}s. To serialize sub-NAPI objects, please refer to {@link NetworkSerializer} obtained
 * by calling {@link Network#serializer(SerialConfig, boolean)}. 
 * </p><p>
 * <b>The entire persistence functionality consists of 3 major parts</b>:
 * <ul>
 *  <li><b>The {@link Persistable} interface.</b> An extension of {@link Serializable} with to important
 *  methods:
 *      <ul>
 *          <li>{@link Persistable#preSerialize()} which executes prior to serialization to do any necessary
 *          preparation work before serializing.</li>
 *          <li>{@link Persistable#postDeSerialize()} which executes immediately following de-serialization
 *          to do any fix-up or finish work on objects after reification. </li>
 *          <li>All serializable objects within HTM.java, implement this interface (which in turn is an extension 
 *          of {@link Serializable}).</li>
 *      </ul> 
 *  </li>
 *  <hr>
 *  <li><b>This {@link PersistenceAPI} (implemented by {@link Network}</b> the only implementor of it), is the 
 *  top-level api through which objects may be saved and/or serialized and deserialized. This is the level 
 *  at which 95% of users should be concerned with as it contains all the methods and functionality necessary 
 *  to serialize a {@link Network}, save it to disk and to checkpoint it to quickly save its state.</li>
 *  <p>
 *  Typical use cases and their idioms for usage are as follows:
 *      <ul>
 *          <li><b>Store / Restart</b>
 *              <pre>
 *                  // Example assumes the use of a FileSensor... For {@link ObservableSensor}s insert step "C" 
 *                  {@link Network} network = ...
 *                  network.observe().subscribe(new Observer<Inference>() {...})
 *                  network.start();
 *                  ...
 *   A.)            network.store();  // Halts the Network (see {@link Network#halt()}), and persists it to disk. 
 *                  ...
 *   B.)            network.observe().subscribe(() -> { }) // {@link rx.Observable} listeners are not serializable 
 *                                                            so they must be rebuilt.
 *                                                            
 *   C.) (Optional) Publisher publisher = network.getPublisher(); // Only if using an {@link ObservableSensor}
 *              
 *   D.)            network.restart();
 *              </pre>
 *          </li>
 *          <li><b>Store / De-serialize / Restart</li></b>
 *              <pre>
 *                  // Example assumes the use of a FileSensor... For {@link ObservableSensor}s insert step "D"  
 *                  {@link Network} network = ...
 *                  network.observe().subscribe(new Observer<Inference>() {...})
 *                  network.start();
 *                  ...
 *   A.)            network.store();  // Halts the Network (see {@link Network#halt()}), and persists it to disk. 
 *                  ...
 *   B.)            Network <b>serializedNetwork</b> = Network.load();
 *                  ...
 *   C.)            serializedNetwork.observe().subscribe(() -> { }) // {@link rx.Observable} listeners are not serializable 
 *                                                            so they must be rebuilt.
 *                                                            
 *   D.) (Optional) Publisher publisher = network.getPublisher(); // Only if using an {@link ObservableSensor}
 *              
 *   E.)            network.restart();
 *              </pre>
 *          </li>
 *          <li><b>Checkpoint</b>
 *              <pre>
 *                  {@link Network} network = ...
 *                  
 *                  network.observe().subscribe(new Observer<Inference>() { 
 *                      public void onCompleted() {}
 *                      public void onError(Throwable e) { e.printStackTrace(); }
 *                      public void onNext(Inference inference) {
 *                          // Do work on Inference...
 *                      }
 *                  });
 *                  
 *                  network.start();
 *                  ...
 *                  
 *                  // BELOW: The byte[] of the checkPoint() operation will be returned to the Observer
 *                  ...
 *   A.)            network.checkPoint(new Observer<byte[]>() {...});  // saves to .../<user home dir>/HTMNetwork/Network_Checkpoint_2016-04-05_17-20-26.189
 *                  ...
 *                  network.checkPoint(new Observer<byte[]>() {...});  // saves to .../<user home dir>/HTMNetwork/Network_Checkpoint_2016-04-05_17-24-21.143
 *                  ...
 *                  network.checkPoint(new Observer<byte[]>() {...});  // saves to .../<user home dir>/HTMNetwork/Network_Checkpoint_2016-04-05_17-33-48.637
 *                  ...
 *              </pre>
 *              <b>Note:</b> The type of Observer is "byte[]" for the above checkPoint method.
 *              
 *              <b>Note:</b> Only call "Publisher publisher = network.getPublisher()" if you are using an {@link ObservableSensor} on your network.
 *              
 *              The underlying framework is signaled by the addition of one or more Observers to its list of check point Observers; to 
 *              fire off a check point. Following this, all currently queued Observers are notified and passed the byte[] of the 
 *              current {@link Network} serialization; after which the current queue is cleared in preparation for another round of
 *              check pointing.
 *              
 *              <b>Note:</b> The Filename and timestamp portion of the checkpoint filename are independently configurable using the 
 *                           {@link SerialConfig} object as thus:
 *              <pre>
 *                  
 *                  Network.serializer(config, true/false); // True = force return of a new {@link NetworkSerializer}, false = return stored.
 *         </li>  
 *      </ul>
 *  </li>  
 *  <hr> 
 *  <li><b>The {@link NetworkSerializer} interface</b> which is the workhorse underneath the {@link PersistenceAPI} which deals with the details of serialization
 *  and writing to/reading from disk. In addition the {@code NetworkSerializer} implements the {@link Serializer} interface from the <a href="https://github.com/EsotericSoftware/kryo">Kryo</a> 
 *  library; so that even though HTM.java uses FST (<a href="https://ruedigermoeller.github.io/fast-serialization/">Fast-Serialization</a>) as its serialization library, it also can be used by frameworks that require
 *  a {@link Kryo} interface (such as <a href="https://flink.apache.org">Flink</a>)</li>
 * </ul>
 * @author cogmission
 * @see Network#serializer(SerialConfig, boolean)
 * @see SerialConfig
 */
public interface PersistenceAPI extends Persistable {
    /**
     * Stores this {@link Network} at the pre-configured location, after
     * halting and shutting down the Network. To store the Network but keep it up
     * and running, please see the {@link #checkPoint()} method. 
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
     * Loads a {@code Network} from the specified serialized byte array and
     * returns the de-serialized Network.
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
     * <b>The new {@link Publisher} can only be retrieved after a call to {@link Network#load()},
     * {@link Network#load(byte[])}, {@link Network#load(String)}, {@link Network#restart()} or
     * {@link NetworkSerializer#deSerialize(Class, byte[])}
     * 
     * @return      the new Publisher created after deserialization or halt.
     * @see #getPublisherSupplier()
     */
    public Publisher getPublisher();
}
