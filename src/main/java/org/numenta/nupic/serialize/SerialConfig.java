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
package org.numenta.nupic.serialize;


import java.io.File;
import java.io.Serializable;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.Parameters;
import org.numenta.nupic.algorithms.BitHistory;
import org.numenta.nupic.algorithms.Classification;
import org.numenta.nupic.model.Cell;
import org.numenta.nupic.model.Column;
import org.numenta.nupic.model.ComputeCycle;
import org.numenta.nupic.model.DistalDendrite;
import org.numenta.nupic.model.Persistable;
import org.numenta.nupic.model.Pool;
import org.numenta.nupic.model.ProximalDendrite;
import org.numenta.nupic.model.Segment;
import org.numenta.nupic.model.Synapse;
import org.numenta.nupic.network.Inference;
import org.numenta.nupic.network.Layer;
import org.numenta.nupic.network.ManualInput;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.network.Region;
import org.numenta.nupic.util.NamedTuple;
import org.numenta.nupic.util.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * <p>
 * Used to configure a {@link NetworkSerializer} with details regarding file formats, 
 * save locations, file opening conventions, and underlying serialization library,
 * among other things.
 * </p><p>
 * <b>This file however provides defaults which may confidently be used for full featured
 * use</b>, however more experienced users may want to tweak details (the first thing 
 * being whether checkpoints add new files or delete previous files maintaining only 
 * one file). For explanation of this see: {@link StandardOpenOptions} and the
 * {@link #SerialConfig(String, Scheme, List, StandardOpenOption...)} constructor
 * </p>
 * <p>
 * The check point file format is highly configurable using this class. The default format
 * contains a "name" portion and a "timestamp" portion which may be independently configured.
 * see {@link #setCheckPointFileName(String)} and {@link #setCheckPointTimeFormatString(String)}.
 * </p>
 * <p>
 * In addition, you may also call {@link #setOneCheckPointOnly(boolean)} to overwrite the 
 * checkpoint file if you would rather not maintain multiple checkpoints.
 * 
 * 
 * @see NetworkTest
 * @see JavaFstNetworkSerializationTest
 * @see JavaKryoNetworkSerializationTest
 * @see NetworkSerializer
 * @see NetworkSerializerImpl
 * 
 * @author cogmission
 */
public class SerialConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    protected static final Logger LOGGER = LoggerFactory.getLogger(SerialConfig.class);
    
    /** The known types we are serializing */
    @SuppressWarnings("unchecked")
    public static final Class<? extends Persistable>[] DEFAULT_REGISTERED_TYPES = new Class[] {
        Region.class, Layer.class, Cell.class, Column.class, Synapse.class,
        ProximalDendrite.class, DistalDendrite.class, Segment.class, Inference.class,
        ManualInput.class, BitHistory.class, Tuple.class, NamedTuple.class, Parameters.class,
        ComputeCycle.class, Classification.class, FieldMetaType.class, Pool.class, Persistable.class
    };

    /** The default format for the timestamp portion of the checkpoint file name */
    public static final String CHECKPOINT_FORMAT_STRING = "YYYY-MM-dd_HH-mm-ss.SSS";

    /** 
     * <p>
     * The default options for the regular {@link Network#store()} method which stores the
     * {@link Network} under the default file name "Network.ser".
     * </p><p>
     * Default options are:
     * <ul>
     *  <li>{@link StandardOpenOption#CREATE} - creates the file if it does not exist</li>
     *  <li>{@link StandardOpenOption#TRUNCATE_EXISTING} - Truncates the file to length 0, 
     *  before write to it again (basically never appends to the file which is necessary)</li>
     * </ul>
     * </p>
     */
    public static final StandardOpenOption[] PRODUCTION_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    /** 
     * <p>
     * The default options for {@link CheckPointer#checkPoint(rx.Observer)} method which quick
     * stores the {@link Network} state to a time-stamped file.
     * </p><p>
     * Default options are:
     * <ul>
     *  <li>{@link StandardOpenOption#CREATE} - creates the file if it does not exist</li>
     *  <li>{@link StandardOpenOption#TRUNCATE_EXISTING} - Truncates the file to length 0, 
     *  before write to it again (basically never appends to the file which is necessary)</li>
     * </ul>
     * </p>
     */
    public static final StandardOpenOption[] CHECKPOINT_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    /** Default directory to store the serialized file */
    public static final String SERIAL_DIR = "HTMNetwork";
    /** Default directory to store the serialized file */
    public static final String SERIAL_TEST_DIR = "HTMNetworkTest";
    /** Default serialized Network file name for the {@link Network#store()} method. */
    private static final String SERIAL_FILE = "Network.ser";
    /** Default checkpoint Network file name for the {@link CheckPointer#checkPoint(rx.Observer)} method. */
    private static final String CHECKPOINT_FILE = "Network_Checkpoint_";
    
    private String fileName;
    private String fileDir;
    private List<Class<? extends Persistable>> registry;
    private StandardOpenOption[] options = PRODUCTION_OPTIONS;
    private StandardOpenOption[] checkPointOptions = CHECKPOINT_OPTIONS;
    
    private String checkPointFileName = CHECKPOINT_FILE;
    
    private String checkPointFormatString = CHECKPOINT_FORMAT_STRING;
    
    /** Specifies that as a new CheckPoint file is written, the old one is deleted */
    private boolean oneCheckPointOnly;
    
     
    
    /**
     * Constructs a new {@code SerialConfig} which uses the default serialization
     * file name
     */
    public SerialConfig() {
        this(null);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * 
     * @param fileName  the file name to use
     */
    public SerialConfig(String fileName) {
        this(fileName, (String)null);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specific file name
     * and file directory.
     * 
     * @param fileName      the file name to use
     * @param fileDir       the file directory to use
     */
    public SerialConfig(String fileName, String fileDir) {
        this(fileName, fileDir, null);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * 
     * Pre-registering the "known" classes which will be serialized is highly 
     * recommended. These may be specified by the list named "registeredTypes".
     * 
     * Because the ID assigned is affected by the IDs registered before it, the order 
     * classes are registered is important when using this method. The order must be the 
     * same at deserialization as it was for serialization.
     * 
     * @param fileName          the file name to use
     * @param fileDir           the directory to use.
     * @param registeredTypes   list of classes indicating expected types to serialize
     */
    public SerialConfig(String fileName, String fileDir, List<Class<? extends Persistable>> registeredTypes) {
        this(fileName, fileDir, registeredTypes, PRODUCTION_OPTIONS);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * 
     * Pre-registering the "known" classes which will be serialized is highly 
     * recommended. These may be specified by the list named "registeredTypes".
     * 
     * Because the ID assigned is affected by the IDs registered before it, the order 
     * classes are registered is important when using this method. The order must be the 
     * same at deserialization as it was for serialization.
     * 
     * The {@link OpenOption}s argument specifies how the file should be opened (either 
     * create new, truncate, append, read, write etc). see ({@link StandardOpenOption})
     * 
     * @param fileName          the file name to use
     * @param registeredTypes   list of classes indicating expected types to serialize 
     * @param openOptions       A list of options to use for how to work with the serialization 
     *                          file.
     */
    public SerialConfig(String fileName, String fileDir, List<Class<? extends Persistable>> registeredTypes, StandardOpenOption... openOptions) {
        
        this.fileName = fileName == null ? SERIAL_FILE : fileName;
        this.fileDir = fileDir == null ? SERIAL_DIR : fileDir;
        
        if(registeredTypes == null) {
            LOGGER.debug("List of registered serialize class types was null. Using the default...");
        }
        this.registry = registeredTypes == null ? Arrays.asList(DEFAULT_REGISTERED_TYPES) : registeredTypes;

        if(openOptions == null) {
            LOGGER.debug("The OpenOptions were null. Using the default...");
        }
        
        this.options = openOptions == null ? PRODUCTION_OPTIONS : openOptions;
    }
    
    /**
     * The file name used to store the serialized object.
     * @return the file name to use
     */
    public String getFileName() {
        return fileName;
    }
    
    /**
     * Returns the directory within which files are saved.
     * @return  the save directory
     */
    public String getFileDir() {
        return fileDir;
    }
    
    /**
     * Return the absolute path to the serialize directory.
     * @return  the absolute path to the serialize directory.
     */
    public String getAbsoluteSerialDir() {
        return System.getProperty("user.home") + File.separator + fileDir;
    }
    
    /**
     * Returns the name portion of the checkpoint file name. The check pointed
     * file will have a name consisting of two parts, the "name" and the "timestamp".
     * To set the  
     * @return
     */
    public String getCheckPointFileName() {
        return checkPointFileName;
    }
    
    /**
     * Sets the name portion of the checkpointed file's name.
     * @param name  the name portion of the checkpointed file's name.
     */
    public void setCheckPointFileName(String name) {
        this.checkPointFileName = name;
    }
    
    /**
     * Sets the format string for the date portion of the checkpoint file name.
     * @return  the format string for the date portion of the checkpoint file name.
     */
    public String getCheckPointFormatString() {
        return this.checkPointFormatString;
    }
    
    /**
     * Sets the format string for the date portion of the checkpoint file name.
     * @param formatString  the format to use on the current timestamp.
     */
    public void setCheckPointTimeFormatString(String formatString) {
        if(formatString == null || formatString.isEmpty()) {
            throw new NullPointerException("Cannot use a null or empty format string.");
        }
        
        checkPointFormatString = formatString;
    }
    
    /**
     * @return the registry
     */
    public List<Class<? extends Persistable>> getRegistry() {
        return registry;
    }
    
    /**
     * Returns a list of the configured serialization file treatment
     * options.
     * @return  a list of OpenOptions
     */
    public StandardOpenOption[] getOpenOptions() {
        return options;
    }
    
    /**
     * Returns the NIO File options used to determine how to create files,
     * overwrite files etc.
     * @return  the NIO File options
     * @see StandardOpenOption
     */
    public StandardOpenOption[] getCheckPointOpenOptions() {
        return checkPointOptions;
    }
    
    /**
     * Sets the NIO File options used to determine how to create files,
     * overwrite files etc.
     * 
     * @param options   the NIO File options
     * @see StandardOpenOption
     */
    public void setCheckPointOpenOptions(StandardOpenOption[] options) {
        this.checkPointOptions = options;
    }
    
    /**
     * Specifies that as a new CheckPoint file is written, the old one is deleted
     * @param b     true to maintain at most one file, false to keep writing new files (default).
     */
    public void setOneCheckPointOnly(boolean b) {
        this.oneCheckPointOnly = b;
    }
    
    /**
     * Returns a flag indicating whether only one check point file will exist at a time, or not.
     * @return  the flag specifying this condition.
     */
    public boolean isOneCheckPointOnly() {
        return oneCheckPointOnly;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + ((registry == null) ? 0 : registry.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;
        if(obj == null)
            return false;
        if(getClass() != obj.getClass())
            return false;
        SerialConfig other = (SerialConfig)obj;
        if(fileName == null) {
            if(other.fileName != null)
                return false;
        } else if(!fileName.equals(other.fileName))
            return false;
        if(registry == null) {
            if(other.registry != null)
                return false;
        } else if(!registry.equals(other.registry))
            return false;
        return true;
    }
}

