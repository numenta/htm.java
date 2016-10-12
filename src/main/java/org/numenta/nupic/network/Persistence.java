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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.serialize.SerialConfig;
import org.numenta.nupic.serialize.SerializerCore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used to get a reference to a {@link PersistenceAPI} implementation via static
 * methods {@link Persistence#get()} and {@link Persistence#get(SerialConfig)}, where 
 * the {@link SerialConfig} object is used to determine file handling details such as:
 * <ul>
 *  <li>General {@link Network} storage file name</li>
 *  <li>The check pointed storage file name. (has two parts; name, and date - this is for the name)
 *  <li>The check pointed storage file name date extension. (the date part of the name)
 *  <li>The general storage and check pointed file directory</li> (always somewhere under the user's home directory)
 * </ul>
 * Note: there is a default constructor on {@link SerialConfig} which indicates the use of defaults which
 * are just fine for most circumstances; which is why these {@code Persistence} factory methods can be called
 * without a SerialConfig.
 * 
 * Normal usage is as follows:
 * <pre>
 * PersistenceAPI api = Peristence.get();
 * api.load()...
 * api.store()...
 * api.read()...
 * api.write()...
 * api.checkPointer(Network).checkPoint(Observer)...
 * ...
 * </pre>
 * </p>
 * <p>
 * And for low-level access only:
 * <pre>
 * SerializerCore core = api.serializer();
 * </pre>
 * </P
 * @author cogmission
 * @see SerialConfig
 * @see PersistenceAPI
 */
public class Persistence {
    private static PersistenceAPI access;
    
    public static PersistenceAPI get() {
        return get(new SerialConfig());
    }
    
    public static PersistenceAPI get(SerialConfig config) {
        if(access == null) {
            access = new PersistenceAccess(config);
        }
        return access;
    }
    
    
    /**
     * Implementation of {@link PersistenceAPI}
     * 
     * @author cogmission
     * @see Persistence
     */
    static class PersistenceAccess implements PersistenceAPI {
        private static final long serialVersionUID = 1L;

        protected static final Logger LOGGER = LoggerFactory.getLogger(PersistenceAPI.class);
        
        /** Time stamped serialization file format */
        public static DateTimeFormatter CHECKPOINT_TIMESTAMP_FORMAT = DateTimeFormat.forPattern(SerialConfig.CHECKPOINT_FORMAT_STRING);
        private DateTimeFormatter checkPointFormatter = CHECKPOINT_TIMESTAMP_FORMAT;
        
        /** Indicates the underlying file settings */
        private SerialConfig serialConfig;
        
        /** Stores the bytes of the last serialized object or null if there was a problem */
        private static AtomicReference<byte[]> lastBytes = new AtomicReference<byte[]>(null);
        /** 
         * All instances in this classloader will share the same atomic reference to the last 
         * checkpoint file name holder which is perfectly fine.
         */
        private static AtomicReference<String> lastCheckPointFileName = new AtomicReference<String>(null);
        
        private SerializerCore defaultSerializer = new SerializerCore();
        
        private ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
        private Lock writeMonitor = rwl.writeLock();
        private Lock readMonitor = rwl.readLock();
        
        
        /**
         * Returns a {@link PersistenceAPI} implementation which uses the specified
         * {@link SerialConfig}
         * @param config    the file convention configurations.
         */
        public PersistenceAccess(SerialConfig config) {
            this.serialConfig = config == null ? new SerialConfig() : config;
            this.checkPointFormatter = DateTimeFormat.forPattern(config.getCheckPointFormatString());
        }
        
        /**
         * (optional)
         * Sets the {@link SerialConfig} for detailed control. In common practice
         * this object is initialized with a default that is fine.
         * @param config    
         */
        public void setConfig(SerialConfig config) {
            this.serialConfig = config;
            this.checkPointFormatter = DateTimeFormat.forPattern(config.getCheckPointFormatString());
        }
        
        /**
         * Returns the {@link SerialConfig} in use
         * @return  the SerialConfig in current use
         */
        public SerialConfig getConfig() {
            return serialConfig;
        }
        
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
        public SerializerCore serializer() {
            if(defaultSerializer == null) {
                defaultSerializer = new SerializerCore(SerialConfig.DEFAULT_REGISTERED_TYPES);
            }
            return defaultSerializer;
        }
        
        /**
         * Reifies a {@link Persistable} from the specified file in the location and file name
         * configured by the {@link SerialConfig} passed in at construction time.
         * 
         * @return  the reified type &lt;R&gt;
         */
        public <R extends Persistable> R read() {
            LOGGER.debug("PersistenceAccess reify() [serial config file name=" + serialConfig.getFileName() +"] called ...");
            return read(serialConfig.getFileName());
        }
        
        /**
         * Reifies a {@link Persistable} from the specified file in the location
         * configured by the {@link SerialConfig} passed in at construction time, using
         * the file name specified.
         * 
         * @param fileName  the name of the file from which to get the serialized object.
         * @return  the reified type &lt;R&gt;
         */
        public <R extends Persistable> R read(String fileName) {
            LOGGER.debug("PersistenceAccess reify(" + fileName + ") called ...");
            byte[] bytes;
            try {
                bytes = readFile(fileName);
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            return read(bytes);
        }
        
        /**
         * Loads a {@code Network} from the specified serialized byte array and
         * returns the de-serialized Network.
         *  
         * @param serializedBytes             the name of the serialization file.
         *    
         * @return  returns the specified Network
         */
        public <R extends Persistable> R read(byte[] serializedBytes) {
            LOGGER.debug("PersistenceAccess reify(byte[]) called ...");
            
            R r =  serializer().deSerialize(serializedBytes);
            return r.postDeSerialize();
        }
        
        /**
         * Persists the {@link Persistable} subclass
         * @param instance
         * @return
         */
        @SuppressWarnings("unchecked")
        public <T extends Persistable, R> R write(T instance) {
            LOGGER.debug("PersistenceAccess persist(T) called ...");
            instance.preSerialize();
            
            byte[] bytes = serializer().serialize(instance);
            
            try {
                writeFile(serialConfig, bytes);
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            
            return (R)bytes;
        }
        
        @SuppressWarnings("unchecked")
        public <T extends Persistable, R> R write(T instance, String fileName) {
            LOGGER.debug("PersistenceAccess persist(T, " + fileName + ") called ...");
            instance.preSerialize();
            
            byte[] bytes = serializer().serialize(instance);
            
            try {
                writeFile(fileName, bytes, serialConfig.getOpenOptions());
            }catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            
            return (R)bytes;
        }
        
        /**
         * Loads a {@code Network} from the default or previously configured location and
         * serial file, and returns it. 
         * @return the deserialized Network
         * @see SerialConfig
         */
        @Override
        public Network load() {
            LOGGER.debug("PersistenceAccess load() called ...");
            
            String defaultFileName = serialConfig.getFileName();
            byte[] bytes;
            try {
                bytes = readFile(defaultFileName);
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            Network network = serializer().deSerialize(bytes);
            return network.postDeSerialize();
        }
        
        /**
         * Loads a {@code Network} from the specified serialized file name and
         * returns it.
         *  
         * @param fileName      the name of the serialization file.
         *    
         * @return  returns the specified Network
         */
        @Override
        public Network load(String fileName) throws IOException {
            LOGGER.debug("PersistenceAccess load(" + fileName + ") called ...");
            
            byte[] bytes = readFile(fileName);
            Network network = serializer().deSerialize(bytes);
            return network; 
        }
        
        /**
         * Stores the specified {@link Network} to the pre-configured (with {@link SerialConfig})
         * location and filename.
         * @param network
         */
        @Override
        public void store(Network network) {
            storeAndGet(network);
        }
        
        /**
         * Stores the specified {@link Network} at the pre-configured location, after
         * halting and shutting down the Network. To store the Network but keep it up
         * and running, please see the {@link #checkPointer()} method. 
         * 
         * The Network, may however be {@link #restart()}ed after this method is called.
         * 
         * @param network       the {@link Network} to persist
         * @param returnBytes   flag indicating whether to return the interim byte array
         * 
         * @return the serialized Network in the format is either a byte[] or String (json),
         *          where byte[] is the default.
         */
        @SuppressWarnings("unchecked")
        @Override
        public <R> R storeAndGet(Network network) {
            // Make sure any serialized Network is first halted.
            network.preSerialize();
            
            byte[] bytes = defaultSerializer.serialize(network);
            
            try {
                writeFile(serialConfig, bytes);
            } catch(IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
            
            return (R)bytes;
        }
        
        /**
         * Returns an {@link rx.Observable} operator that when subscribed to, invokes an operation
         * that stores the state of this {@code Network} while keeping the Network up and running.
         * The Network will be stored at the pre-configured location (in binary form only, not JSON).
         * 
         * @param network   the {@link Network} to check point.
         * @return  the {@link CheckPointOp} operator 
         */
        @Override
        public CheckPointOp<byte[]> checkPointer(Network network) {
            network.setCheckPointFunction(getCheckPointFunction(network));
            return network.getCheckPointOperator();
        }
        
        /**
         * Returns a {@link Function} to set on the specified network as a callback
         * with privileged access.
         * 
         * This {@code Function} writes the state of the specified {@link Network} to the
         * pre-configured check point file location using the format specified in the 
         * {@link SerialConfig} specified during construction or later set on this object.
         * 
         * @param network       the {@link Network} to check point
         * @return  a Function which checkpoints
         */
        @SuppressWarnings("unchecked")
        <T extends Persistable, R> Function<T, R> getCheckPointFunction(Network network) {
            return (T t) -> {
                t.preSerialize();
                
                String oldCheckPointFileName = lastCheckPointFileName.getAndSet(
                    serialConfig.getAbsoluteSerialDir() + File.separator + serialConfig.getCheckPointFileName() + 
                        checkPointFormatter.print(new DateTime()));

                byte[] bytes = defaultSerializer.serialize(network);
                try {
                    writeFile(lastCheckPointFileName.get(), bytes, serialConfig.getCheckPointOpenOptions());
                }catch(IOException io) {
                    throw new RuntimeException(io);
                }
                
                if(serialConfig.isOneCheckPointOnly() && oldCheckPointFileName != null) {
                    try {
                        Files.deleteIfExists(new File(oldCheckPointFileName).toPath());
                    }catch(IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                
                return (R)bytes;
            };
        }
        
        /**
         * Returns the last check pointed bytes of the last check point operation.
         * 
         * @return  a byte array
         */
        @Override
        public byte[] getLastCheckPoint() {
            return lastBytes.get();
        }
        
        /**
         * Returns the name of the most recently checkpointed {@code Network} file.
         * @return  the name of the most recently checkpointed {@code Network} file.
         */
        @Override
        public String getLastCheckPointFileName() {
            return lastCheckPointFileName.get();
        }
        
        /**
         * Returns a {@code List} of check pointed file names.
         * @return a {@code List} of check pointed file names.
         */
        @Override
        public List<String> listCheckPointFiles() {
            List<String> chkPntFiles = null;
            
            try {
                readMonitor.lock();
                
                String path = System.getProperty("user.home") + File.separator + serialConfig.getFileDir();
                File customDir = new File(path);
                
                final DateTimeFormatter f = checkPointFormatter;
                chkPntFiles = Arrays.stream(customDir.list((d,n) -> {
                    // Return only checkpoint files before the specified checkpoint name.
                    return n.indexOf(serialConfig.getCheckPointFileName()) != -1;
                })).sorted((o1,o2) -> {
                    // Sort the list so that the most recent previous can be selected.
                    try {
                        String n1 = o1.substring(serialConfig.getCheckPointFileName().length());
                        String n2 = o2.substring(serialConfig.getCheckPointFileName().length());
                        return f.parseDateTime(n1).compareTo(f.parseDateTime(n2));
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }
                }).collect(Collectors.toList());
            }catch(Exception e) {
                throw new RuntimeException(e);
            }finally{
                readMonitor.unlock();
            }
            
            return chkPntFiles;
        }
        
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
        @Override
        public String getPreviousCheckPoint(String checkPointFileName) {
            final DateTimeFormatter f = checkPointFormatter;
            
            String defaultNamePortion = serialConfig.getCheckPointFileName();
            
            if(checkPointFileName.indexOf(defaultNamePortion) != -1) {
                checkPointFileName = checkPointFileName.substring(defaultNamePortion.length());
            }
            
            final String cpfn = checkPointFileName;
            
            String[] chkPntFiles = listCheckPointFiles().stream()
                .map(n -> n.substring(defaultNamePortion.length()))
                .filter(n -> f.parseDateTime(n).isBefore(f.parseDateTime(cpfn)))
                .toArray(size -> new String[size]);
            
            if(chkPntFiles != null && chkPntFiles.length > 0) {
                return defaultNamePortion.concat(chkPntFiles[chkPntFiles.length - 1]);
            }
            
            return null;
        }
        
        /**
         * Convenience method which returns the store file fully qualified path. 
         * @return  the fully qualified store path
         */
        public String currentPath() {
            return System.getProperty("user.home") + File.separator + serialConfig.getFileDir() +
                File.separator + serialConfig.getFileName();
        }
            
        /**
         * Creates the serialization file.
         * 
         * @param config            the {@link SC} specifying the file storage file
         * 
         * @return the File object
         * @throws IOException      if there is a problem locating the specified directories or 
         *                          creating the file.
         */
        File ensurePathExists(SerialConfig config) throws IOException {
            return ensurePathExists(config, config.getFileName());
        }
        
        /**
         * <p>
         * For Writing:</p>
         * Creates the serialization file.
         * 
         * @param config            the {@link SC} specifying the file storage file.\
         * @param fileName          the name of the file to retrieve
         * 
         * @return the File object
         * @throws IOException      if there is a problem locating the specified directories or 
         *                          creating the file.
         */
        File ensurePathExists(SerialConfig config, String fileName) throws IOException {
            File serializedFile = null;
        
            try {
                writeMonitor.lock();
                
                String path = System.getProperty("user.home") + File.separator + serialConfig.getFileDir();
                File customDir = new File(path);
                // Make sure container directory exists
                customDir.mkdirs();
    
                // Check to make sure the fileName doesn't already include the full path.
                serializedFile = new File(fileName.indexOf(customDir.getAbsolutePath()) != -1 ?
                    fileName : customDir.getAbsolutePath() + File.separator +  fileName);
                if(!serializedFile.exists()) {
                    serializedFile.createNewFile();
                }
            }catch(Exception io) {
                throw io;
            }finally{
                writeMonitor.unlock();
            }
            
            return serializedFile;
        }
        
        /**
         * <p>
         * For Reading:</p>
         * Returns the File corresponding to "fileName" if this framework is successful in locating
         * the specified file, otherwise it throws an {@link IOException}.
         * 
         * @param fileName          the name of the file to search for.
         * @return  the File if the operation is successful, otherwise an exception is thrown
         * @throws IOException      if the specified file is not found, or there's a problem loading it.
         */
        File testFileExists(String fileName) throws IOException, FileNotFoundException {
            try {
                readMonitor.lock();
                
                String path = System.getProperty("user.home") + File.separator + serialConfig.getFileDir();
                File customDir = new File(path);
                // Make sure container directory exists
                customDir.mkdirs();
                
                File serializedFile = new File(fileName.indexOf(customDir.getAbsolutePath()) != -1 ?
                    fileName : customDir.getAbsolutePath() + File.separator +  fileName);
                if(!serializedFile.exists()) {
                    throw new FileNotFoundException("File \"" + fileName + "\" was not found.");
                }
                
                return serializedFile;
            }catch(IOException io) {
                throw io;
            }finally{
                readMonitor.unlock();
            }
        }
        
        /**
         * Writes the file contained in "bytes" to disk using the {@link SerialConfig} 
         * specified which in turn specifies file name and location specifics.
         *          
         * @param config            the SerialConfig to use for file name and location        
         * @param bytes             the bytes to write
         * @throws IOException      if there is a problem writing the file
         */
        void writeFile(SerialConfig config, byte[] bytes) throws IOException {
            writeFile(config.getFileName(), bytes, config.getOpenOptions());
        }
      
        /**
         * Writes the file specified by "fileName" using the pre-configured location 
         * specified by the {@link SerialConfig}.
         *   
         * @param fileName          the file name to use
         * @param bytes             the content to write
         * @param options           the file handling rules to use
         * @throws IOException      if there is a problem writing the file
         */
        void writeFile(String fileName, byte[] bytes, StandardOpenOption... options) throws IOException {
            try {
                //writeMonitor.lock();
                Path path = ensurePathExists(serialConfig, fileName).toPath();
                Files.write(path, bytes, options);
            } catch(Exception e) {
               lastBytes.set(null);
               throw e;
            } finally {
                //writeMonitor.unlock();
            }
            
            lastBytes.set(bytes);
        }
        
        /**
         * Reads the file located at the path specified and returns the content
         * in the form of a byte array.
         * 
         * @param filePath          the fully qualified file path
         * 
         * @return a byte array containing the object
         * @throws IOException  if there is a problem reading the file
         */
        byte[] readFile(String filePath) throws IOException {
            Path path = testFileExists(filePath).toPath();
            return readFile(path);
        }
        
        /**
         * Reads the file located at the path specified and returns the content
         * in the form of a byte array.
         * 
         * @param path          the {@link Path} object
         * @return  a byte array containing the object
         * @throws IOException  if there is a problem reading the file
         */
        byte[] readFile(Path path) throws IOException {
            
            byte[] bytes = null;
            try {
                //readMonitor.lock();
                bytes = Files.readAllBytes(path);
            } finally {
                //readMonitor.unlock();
            }
            
            return bytes;
        }
    }
}
