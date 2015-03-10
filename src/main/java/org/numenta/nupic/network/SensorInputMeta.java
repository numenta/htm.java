package org.numenta.nupic.network;

import java.util.List;
import java.util.stream.Collectors;

import org.numenta.nupic.FieldMetaType;
import org.numenta.nupic.ValueList;
import org.numenta.nupic.util.Tuple;


/**
 * Meta data describing the fields, types and reset
 * rules.
 * 
 * @author David Ray
 */
public class SensorInputMeta implements ValueList {
    private ValueList rawTupleList;
    /** Name of each field */
    private List<String> fieldNames;
    /** Field data types */
    private List<FieldMetaType> fieldMeta;
    /** Processing flags and hints */
    private List<SensorFlags> sensorFlags; 
    
    public SensorInputMeta(ValueList input) {
        if(input.size() != 3) {
            throw new IllegalArgumentException("Input did not have 3 rows");           
        }
        this.rawTupleList = input;
        this.fieldNames = input.getRow(0).all().stream().map(o -> o.toString()).collect(Collectors.toList());
        this.fieldMeta = input.getRow(1).all().stream().map(FieldMetaType::fromString).collect(Collectors.toList());
        this.sensorFlags = input.getRow(2).all().stream().map(SensorFlags::fromString).collect(Collectors.toList());
    }
    public Tuple getRow(int index) {
        return rawTupleList.getRow(index);
    }
    public List<String> getFieldNames() {
        return fieldNames;
    }
    public List<FieldMetaType> getFieldTypes() {
        return fieldMeta;
    }
    public List<SensorFlags> getFlags() {
        return sensorFlags;
    }
    @Override
    public int size() {
        return rawTupleList.size();
    }
}


