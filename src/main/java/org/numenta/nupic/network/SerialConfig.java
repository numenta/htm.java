package org.numenta.nupic.network;

import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.numenta.nupic.network.NetworkSerializer.Scheme;

public class SerialConfig {
    public static final StandardOpenOption[] PRODUCTION_OPTIONS = new StandardOpenOption[] { 
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING 
    };
    
    private static final String SERIAL_FILE = "Network.ser";
    
    private String fileName;
    private Scheme scheme;
    private List<Class<?>> registry;
    private StandardOpenOption[] options;
    
    
    
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
