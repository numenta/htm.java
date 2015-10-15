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
package org.numenta.nupic.network.sensor;

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.datagen.ResourceLocator;
import org.numenta.nupic.encoders.MultiEncoder;

/**
 * Simple abstraction to hold the contents of a list of csv records indicated by 
 * a specified path. There are methods to retrieve the header and the
 * body separately. The header size is assumed to be 3.
 * 
 * Additionally, there is a specialized iterator which returns the
 * {@link MultiEncoder}'s required Map of input field names to objects.
 * Instead of hashing a new entry for every row of the input file or stream
 * we simply access this class' internal array by overriding {@link Map#get(Object)}
 * and {@link Iterator#next()} and accessing this class' internal data structures -
 * thus adhering to the Map and Iterator interface without the overhead of mappings
 * and hashing.
 * 
 * @author David Ray
 *
 */
public class CSVSource  implements MetaSource {
    private List<String[]> header;
    private List<String[]> body;
    private List<List<String[]>> file;
    private TObjectIntMap<String> fieldIndexMap = new TObjectIntHashMap<>();
    private FieldMetaType[] fieldTypes;

    private String datePattern;
    private DateTimeFormatter format;

    public static final int HEADER_SIZE = 3;
    public static final int HEADER_IDX = 0;
    public static final int BODY_IDX = 1;

    /**
     * Constructs a new CSVFile object using the specified file name.
     * The file indicated is expected to be on the application's classpath.
     * 
     * @param fileName
     */
    public CSVSource(String fileName) {
        this(fileName, null);
    }

    /**
     * Constructs a new CSVFile object using the specified file name.
     * The file indicated is expected to be on the application's classpath.
     * Additionally, this constructor prepares a {@link DateTimeFormatter} 
     * using the specified format pattern, which must be set if the encoding
     * is to encode a DateTime field.
     * 
     * 
     * @param fileName
     * @param datePattern
     */
    public CSVSource(String fileName, String datePattern) {
        String s = ResourceLocator.path(fileName);
        this.file = createSource(new File(s));
        this.header = file.get(HEADER_IDX);
        this.body = file.get(BODY_IDX);
        this.datePattern = datePattern;
        this.format = DateTimeFormat.forPattern(this.datePattern);

        // Convenience mapping for fast field lookup
        for(int i = 0;i < this.header.get(0).length;i++) {
            fieldIndexMap.put(this.header.get(0)[i], i);
        }

        fieldTypes = new FieldMetaType[this.header.get(0).length];
        for(int i = 0;i < fieldTypes.length;i++) {
            fieldTypes[i] = FieldMetaType.fromString(this.header.get(1)[i]);
        }
    }

    /**
     * Returns a List of string array lists of size 2. 
     * The zero'th index being the header (of size 3),
     * and the first index of size = FILE_SIZE.
     * 
     * @param f
     * @return
     */
    public List<List<String[]>> createSource(File f) {
        List<String[]> body = new ArrayList<>();
        List<String[]> header = new ArrayList<>();
        List<List<String[]>> file = new ArrayList<>();

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(f));
            String line = null;
            int headerIdx = 0;
            while((line = br.readLine()) != null) {
                if(headerIdx++ < HEADER_SIZE) {
                    header.add(line.split("[\\s]*\\,[\\s]*"));
                }else{
                    body.add(line.split("[\\s]*\\,[\\s]*"));
                }
            }
        }
        catch(Exception e) { e.printStackTrace(); }
        finally{
            try { br.close(); }catch(Exception ignore){}
        }

        file.add(header);
        file.add(body);

        return file;
    }

    /**
     * Returns the List of string arrays comprising the header
     * @return
     */
    public List<String[]> getHeader() {
        return header;
    }

    /**
     * Returns the List of string arrays comprising the body
     * @return
     */
    public List<String[]> getBody() {
        return body;
    }

    /**
     * Returns specialized iterator which avoids resetting map entries, thus
     * supporting very fast iteration and resource savings without the need to
     * rehash every single row.
     */
    @Override
    public Iterator<Map<String, Object>> multiIterator() {
        return new Iterator<Map<String, Object>>() {
            int idx = -1;
            int size = body.size();

            @SuppressWarnings("serial")
            Map<String, Object> map = new HashMap<String, Object>() {

                /**
                 * Overridden to access this class' internal array instead of having 
                 * to rehash map entries on every record access and store.
                 */
                @Override
                public Object get(Object name) {
                    int typeIndex = fieldIndexMap.get(name);
                    // Return Date Time type
                    if(fieldTypes[typeIndex] == FieldMetaType.DATETIME) { 
                        if(format == null) {
                            throw new IllegalStateException(
                                "DateField requires pattern configuration on construction.");
                        }

                        return format.parseDateTime(body.get(idx)[typeIndex]);
                    }else if(fieldTypes[typeIndex] == FieldMetaType.FLOAT || 
                        fieldTypes[typeIndex] == FieldMetaType.INTEGER) { // Return any numeric type
                        
                        return Double.parseDouble(body.get(idx)[fieldIndexMap.get(name)]);
                    }
                    // Return String type (i.e. category)
                    return body.get(idx)[fieldIndexMap.get(name)];
                }
            };

            @Override
            public boolean hasNext() {
                return idx < size - 1;
            }

            @Override
            public Map<String, Object> next() {
                idx++;
                return map;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not supported");
            }

        };
    }


}
