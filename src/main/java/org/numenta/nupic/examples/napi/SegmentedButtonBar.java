package org.numenta.nupic.examples.napi;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;


public class SegmentedButtonBar extends HBox {
    
    /**
     * Constructs a custom {@code SegmentedButtonBar}
     */
    public SegmentedButtonBar() {
        setId("buttonbar");
        getStyleClass().setAll("segmented-button-bar");
        
        ToggleButton sampleButton = new ToggleButton("Tasks");
        sampleButton.getStyleClass().addAll("first");
        
        ToggleButton sampleButton2 = new ToggleButton("Administrator");
        ToggleButton sampleButton3 = new ToggleButton("Search");
        ToggleButton sampleButton4 = new ToggleButton("Line");
        ToggleButton sampleButton5 = new ToggleButton("Process");
        sampleButton5.getStyleClass().addAll("last", "capsule");
        
        getChildren().addAll(sampleButton, sampleButton2, sampleButton3, sampleButton4, sampleButton5);
    }
    
    /**
     * Adds a {@link Button} to the custom button bar
     * @param b
     */
    public void addButton(ButtonBase b) {
        getChildren().add(b);
    }
    
    /**
     * Adds one or more {@link Button}s to the custom button bar
     * @param buttons
     */
    public void addButtons(ButtonBase... buttons) {
        getChildren().addAll(buttons);
    }
    
    /**
     * Returns the button whose text matches the specified text.
     * 
     * @param buttonText
     * @return
     */
    public ButtonBase getButton(String buttonText) {
        for(Node n : getChildren()) {
            if(((Button)n).getText().equals(buttonText)) return (Button)n;
        }
        return null;
    }
    
    /**
     * Removes the button matching the text specified, and
     * returns it if it is found, otherwise returns null.
     * 
     * @param buttonText
     * @return
     */
    public ButtonBase removeButton(String buttonText) {
        ButtonBase buttonToRemove = null;
        int buttonIndex = -1;
        int idx = -1;
        for(Node n : getChildren()) {
            ++idx;
            if(((ButtonBase)n).getText().equals(buttonText)) {
                buttonToRemove = (Button)n;
                buttonIndex = idx;
                break;
            }
        }
        
        if(buttonIndex > -1) {
            getChildren().remove(buttonIndex);
        }
        
        return buttonToRemove;
    }
    
    /**
     * Returns the button if it is removed, otherwise returns null.
     * @param b
     * @return
     */
    public ButtonBase removeButton(ButtonBase b) {
        if(getChildren().remove(b)) {
           return b; 
        }
        
        return null;
    }
    
}
