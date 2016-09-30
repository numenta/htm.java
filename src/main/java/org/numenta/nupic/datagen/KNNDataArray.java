package org.numenta.nupic.datagen;

/**
 * Container holding a pairing of data and its class
 * 
 * @author cogmission
 */
public class KNNDataArray {
    private double[][] data;
    private int[] _class;
    
    public KNNDataArray(double[][] data, int[] _class) {
        this.data = data;
        this._class = _class;
    }
    
    public double[][] getDataArray() {
        return data;
    }
    
    public int[] getClassArray() {
        return _class;
    }
}
