package org.numenta.nupic;


public class Constants {
    public static final String ADAPTIVE = "adaptive";
    
    public enum KNN { 
        ADAPTIVE("adaptive");
        
        private String description;
        private KNN(String desc) { this.description = desc; }
        /** {@inheritDoc} */
        public String toString() { return description; }
    }
    
}
