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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.util.Tuple;


/**
 * Meta data describing the fields, types and reset
 * rules usually specified by a file header, but may 
 * be manually or programmatically set.
 * 
 * @author David Ray
 */
public class Header implements ValueList, Serializable {
    private static final long serialVersionUID = 1L;
    
    private ValueList rawTupleList;
    /** Name of each field */
    private List<String> fieldNames;
    /** Field data types */
    private List<FieldMetaType> fieldMeta;
    /** Processing flags and hints */
    private List<SensorFlags> sensorFlags;
    
    private boolean isChanged;
    private boolean isLearn = true;
    
    private int[] resetIndexes;
    private int[] seqIndexes;
    @SuppressWarnings("unused")
    private int[] tsIndexes;
    private int[] learnIndexes;
    @SuppressWarnings("unused")
    private int[] categoryIndexes;
    
    private List<String> sequenceCache;
    
    /**
     * Constructs a new {@code Header} using the specified
     * {@link ValueList}
     * 
     * @param input     3 rows of data describing the input
     */
    public Header(ValueList input) {
        if(input.size() != 3) {
            throw new IllegalArgumentException("Input did not have 3 rows");           
        }
        this.rawTupleList = input;
        this.fieldNames = input.getRow(0).all().stream().map(o -> o.toString()).collect(Collectors.toList());
        this.fieldMeta = input.getRow(1).all().stream().map(FieldMetaType::fromString).collect(Collectors.toList());
        this.sensorFlags = input.getRow(2).all().stream().map(SensorFlags::fromString).collect(Collectors.toList());
    
        initIndexes();
    }
    
    /**
     * Retrieves the header line specified.
     */
    @Override
    public Tuple getRow(int index) {
        return rawTupleList.getRow(index);
    }
    
    /**
     * Returns the number of configuration lines contained.
     * WARNING: Must be size == 3
     */
    @Override
    public int size() {
        return rawTupleList.size();
    }
    
    /**
     * Returns the header line containing the field names.
     * @return
     */
    public List<String> getFieldNames() {
        return fieldNames;
    }
    
    /**
     * Returns the header line containing the field types.
     * @return
     */
    public List<FieldMetaType> getFieldTypes() {
        return fieldMeta;
    }
    
    /**
     * Returns the header line ({@link List}) containing the
     * control flags (in the 3rd line) which designate control
     * operations such as turning learning on/off and resetting
     * the state of an algorithm.
     * 
     * @return
     */
    public List<SensorFlags> getFlags() {
        return sensorFlags;
    }
    
    /**
     * Returns a flag indicating whether any watched column
     * has changed data.
     * 
     * @return
     */
    public boolean isReset() {
        return isChanged;
    }
    
    /**
     * Returns a flag indicating whether the current input state
     * is set to learn or not.
     * 
     * @return
     */
    public boolean isLearn() {
        return isLearn;
    }
    
    /**
     * Processes the current line of input and sets flags based on 
     * sensor flags formed by the 3rd line of a given header.
     * 
     * @param input
     */
    void process(String[] input) {
        isChanged = false;
        
        if(resetIndexes.length > 0) {
            for(int i : resetIndexes) {
                if(Integer.parseInt(input[i].trim()) == 1) {
                    isChanged = true; break;
                }else{
                    isChanged = false;
                }
            }
        }
        
        if(learnIndexes.length > 0) {
            for(int i : learnIndexes) {
                if(Integer.parseInt(input[i].trim()) == 1) {
                    isLearn = true; break;
                }else{
                    isLearn = false;
                }
            }
        }
        
        // Store lines in cache to detect when current input is a change.
        if(seqIndexes.length > 0) {
            boolean sequenceChanged = false;
            if(sequenceCache.isEmpty()) {
                for(int i : seqIndexes) {
                    sequenceCache.add(input[i]);
                }
            }else{
                int idx = 0;
                for(int i : seqIndexes) {
                    if(!sequenceCache.get(idx).equals(input[i])) {
                        sequenceCache.set(idx, input[i]);
                        sequenceChanged = true;
                    }
                }
            }
            isChanged |= sequenceChanged;
        }
    }
    
    /**
     * Initializes the indexes of {@link SensorFlags} types to aid
     * in line processing.
     */
    private void initIndexes() {
        int idx = 0;
        List<Integer> tList = new ArrayList<>();
        List<Integer> rList = new ArrayList<>();
        List<Integer> cList = new ArrayList<>();
        List<Integer> sList = new ArrayList<>();
        List<Integer> lList = new ArrayList<>();
        for(SensorFlags sf : sensorFlags) {
            switch(sf) {
                case T: tList.add(idx);break;
                case R: rList.add(idx);break;
                case C: cList.add(idx);break;
                case S: sList.add(idx);break;
                case L: lList.add(idx);break;
                default:
            }
            idx++;
        }
        
        // Add + 1 to each to offset for Sensor insertion of sequence number in all row headers.
        resetIndexes = rList.stream().mapToInt((Integer i) -> i.intValue() + 1).toArray();
        seqIndexes = sList.stream().mapToInt((Integer i) -> i.intValue() + 1).toArray();
        categoryIndexes = cList.stream().mapToInt((Integer i) -> i.intValue() + 1).toArray();
        tsIndexes = tList.stream().mapToInt((Integer i) -> i.intValue() + 1).toArray();
        learnIndexes = lList.stream().mapToInt((Integer i) -> i.intValue() + 1).toArray();
        
        if(seqIndexes.length > 0) {
            sequenceCache = new ArrayList<>();
        }
    }
}


