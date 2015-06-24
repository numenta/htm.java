package org.numenta.nupic.examples.napi;

import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;


public class SegmentedButtonBarApp extends Application {
    @FXML
    SegmentedButtonBar buttonBar;
    
    @Override public void start(Stage stage) throws Exception {
        BorderPane root = new BorderPane();
        root.setId("background");
 
        buttonBar = FXMLLoader.load(getClass().getResource("SegmentedButtonBar.fxml"));
 
        root.setTop(buttonBar);
 
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.setTitle("Segmented Button Bar");
        stage.show();
    }
 
    public static void main(String[] args) {
        launch(args);
    }
    
}
