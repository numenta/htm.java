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

import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.numenta.nupic.network.NetworkSerializer.Scheme;


/**
 * <p>
 * Used to configure a {@link NetworkSerializer} with details regarding file formats, 
 * save locations, file opening conventions, and underlying serialization library,
 * among other things.
 * </p><p>
 * <b>This file however provides defaults which may confidently be used for full featured
 * use</b>, however more experienced users may want to tweak details (the first thing 
 * being whether checkpoints add new files or delete previous files maintaining only 
 * one file). For explanation of this see: {@link 
 * </p>
 * 
 * @see NetworkTest
 * @see JavaFstNetworkSerializationTest
 * @see JavaKryoNetworkSerializationTest
 * @see NetworkSerializer
 * @see NetworkSerializerImpl
 * 
 * @author cogmission
 */
public class SerialConfig {
    public static final String CHECKPOINT_FORMAT_STRING = "YYYY-MM-dd_HH-mm-ss.SSS";
    
    
    public static final StandardOpenOption[] PRODUCTION_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    public static final StandardOpenOption[] CHECKPOINT_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    private static final String SERIAL_FILE = "Network.ser";
    private static final String CHECKPOINT_FILE = "Network_Checkpoint_";
    
    private String fileName;
    private Scheme scheme;
    private List<Class<?>> registry;
    private StandardOpenOption[] options;
    private StandardOpenOption[] checkPointOptions = CHECKPOINT_OPTIONS;
    
    private String checkPointFileName = CHECKPOINT_FILE;
    
    private String checkPointFormatString = CHECKPOINT_FORMAT_STRING;
    
    /** Specifies that as a new CheckPoint file is written, the old one is deleted */
    private boolean oneCheckPointOnly;
    
    
    /**
     * Constructs a new {@code SerialConfig} which uses the default serialization
     * file name and the specified {@link Scheme}
     * 
     * @param scheme        indicates the underlying serialization scheme to use
     */
    public SerialConfig(Scheme scheme) {
        this(SERIAL_FILE, scheme);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * and underlying serialization scheme specified.
     * 
     * @param fileName  the file name to use
     * @param scheme    indicates the underlying serialization scheme to use
     */
    public SerialConfig(String fileName, Scheme scheme) {
        this(fileName, scheme, null);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * and underlying serialization scheme specified.
     * 
     * Pre-registering the "known" classes which will be serialized is highly 
     * recommended. These may be specified by the list named "registeredTypes".
     * 
     * Because the ID assigned is affected by the IDs registered before it, the order 
     * classes are registered is important when using this method. The order must be the 
     * same at deserialization as it was for serialization.
     * 
     * @param fileName          the file name to use
     * @param scheme            indicates the underlying serialization scheme to use
     * @param registeredTypes   list of classes indicating expected types to serialize
     */
    public SerialConfig(String fileName, Scheme scheme, List<Class<?>> registeredTypes) {
        this(fileName, scheme, registeredTypes, PRODUCTION_OPTIONS);
    }
    
    /**
     * Constructs a new {@code SerialConfig} which will use the specified file name
     * and underlying serialization scheme specified.
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
     * @param scheme            indicates the underlying serialization scheme to use
     * @param registeredTypes   list of classes indicating expected types to serialize 
     * @param openOptions       A list of options to use for how to work with the serialization 
     *                          file.
     */
    public SerialConfig(String fileName, Scheme scheme, 
        List<Class<?>> registeredTypes, StandardOpenOption... openOptions) {
        
        if(fileName == null) {
            throw new IllegalArgumentException("fileName cannot be null.");
        }else if(scheme == null) {
            throw new IllegalArgumentException("scheme cannot be null.");
        }
        
        this.fileName = fileName;
        this.scheme = scheme;
        this.registry = registeredTypes;
        this.options = openOptions;
    }
    
    /**
     * @return the fileName
     */
    public String getFileName() {
        return fileName;
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
     * @return the scheme
     */
    public Scheme getScheme() {
        return scheme;
    }
    
    /**
     * @return the registry
     */
    public List<Class<?>> getRegistry() {
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
        result = prime * result + ((scheme == null) ? 0 : scheme.hashCode());
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
        if(scheme != other.scheme)
            return false;
        return true;
    }
}
