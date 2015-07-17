package org.numenta.nupic.examples.cortical_io;

import io.cortical.fx.webstyle.CorticalLogoBackground;
import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.fx.webstyle.SegmentedButtonBar;
import io.cortical.fx.webstyle.example.LogoTitlePane;
import io.cortical.rest.model.Term;
import io.cortical.services.api.client.ApiException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import org.numenta.nupic.network.Network;



public class FoxEatsDemoView extends GridPane {
    private FoxEatsDemo demo;
    
    private String apiKey;
    
    private TextArea lArea;
    
    
    
    public FoxEatsDemoView(FoxEatsDemo demo, Application.Parameters params) {
        this.demo = demo;
        
        // Extract api key from arguments
        apiKey = params.getUnnamed().get(0).substring(2).trim();
        
        setBackground(new Background(new BackgroundFill(Color.WHITE, null, null)));
        
        // LeftMargin And RightMargin
        HBox h = new HBox();
        h.prefWidthProperty().bind(widthProperty().divide(20));
        h.setFillHeight(true);
        HBox h2 = new HBox();
        h2.prefWidthProperty().bind(widthProperty().divide(20));
        h2.setFillHeight(true);
        
        // StackPane: Center panel, z:0 CorticalLogoPane, z:1 VBox w/main content
        StackPane stack = new StackPane();
        stack.prefWidthProperty().bind(widthProperty().multiply(9.0/10.0));
        stack.prefHeightProperty().bind(heightProperty());
        
        //////////////////////////////
        //    Z:0 background logo   //
        //////////////////////////////
        CorticalLogoBackground backGround = new CorticalLogoBackground(stack);
        backGround.setOpacity(0.2);
        
        //////////////////////////////
        // Z:1 Main Content in VBox //
        //////////////////////////////
        VBox vBox = new VBox();
        vBox.setSpacing(20);
        vBox.prefWidthProperty().bind(stack.widthProperty());
        vBox.prefHeightProperty().bind(new SimpleDoubleProperty(100.0));
        
        LogoTitlePane header = new LogoTitlePane();
        header.setTitleText("What Does A Fox Eat?");
        header.setSubTitleText("an example of using Numenta's Hierarchical Temporal Memory with Cortical.io's Semantic Folding...");
        
        HBox buttonBar = createSegmentedButtonBar();
        
        LabelledRadiusPane inputPane = getDisplayPane();
        vBox.getChildren().addAll(header, buttonBar, inputPane);
        
        stack.getChildren().addAll(backGround, vBox);
        
        // Main Layout: 3 columns, 1 row
        add(h, 0, 0);
        add(stack, 1, 0);
        add(h2, 2, 0);
        
        Platform.runLater(() -> {
            Thread t = new Thread() {
                public void run() { try{ 
                    Thread.sleep(100);}catch(Exception e) {}
                    Platform.runLater(() -> {
                        getScene().getWindow().setWidth(getScene().getWindow().getWidth() + 10);
                        vBox.layoutBoundsProperty().addListener((v, o, n) -> {
                            inputPane.setPrefHeight(vBox.getLayoutBounds().getHeight() - inputPane.getLayoutY() - 50);
                        });                       
                    });
                }
            };
            t.start();
        });
       
    }
    
    public LabelledRadiusPane getDisplayPane() {
        LabelledRadiusPane left = new LabelledRadiusPane("Input Tweet");
        lArea = new TextArea();
        lArea.setWrapText(true);
        lArea.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 16));
        lArea.layoutYProperty().bind(left.labelHeightProperty().add(10));
        left.layoutBoundsProperty().addListener((v, o, n) -> {
            lArea.setLayoutX(10);
            lArea.setPrefWidth(n.getWidth() - 20);
            lArea.setPrefHeight(n.getHeight() - left.labelHeightProperty().get() - 20);
        });
        lArea.textProperty().addListener((v, o, n) -> {
            lArea.setScrollTop(Double.MAX_VALUE);
            lArea.setScrollLeft(Double.MAX_VALUE);
        });
        left.getChildren().add(lArea);
        
        String smallTabs = "\t\t\t\t\t\t\t";
        String bigTabs = "\t\t\t\t\t\t\t\t";
        demo.getPhraseEntryProperty().addListener((v, o, n) -> {
            Platform.runLater(() -> {
                lArea.appendText("\n" + n[0] + (n[0].length() > 4 ? smallTabs : bigTabs) + n[1] + "\t\t\t\t\t\t\t\t" + n[2]);
            });
        });
        
        demo.getPhraseEndedProperty().addListener((v, o, n) -> {
            Platform.runLater(() -> {
                lArea.appendText("\n\nWhat does a fox eat?");
            });
        });
        
        return left;
    }
    
    /**
     * Demonstrates the construction and usage of the {@link SegmentedButtonBar}
     * @return
     */
    public SegmentedButtonBar createSegmentedButtonBar() {
        Button button1 = new Button("Start");
        button1.getStyleClass().addAll("only");
        button1.setOnAction(e -> {
            Platform.runLater(() -> { startNetwork(); });
        });
        
        SegmentedButtonBar  buttonBar = new SegmentedButtonBar();
        buttonBar.getChildren().addAll(button1);
        
        return buttonBar;
    }
    
    private void startNetwork() {
        demo.setDataFilePath("foxeat.csv");
        demo.setInputData(demo.readInputData("foxeat.csv"));
        
        demo.loadCache();
        
        // Test api connection by executing dummy query
        boolean success = demo.connectionValid(apiKey);
        if(!success) {
            throw new RuntimeException(new ApiException());
        }

        // Create the Network
        Network network = demo.createNetwork();

        // Returns the last line of the file which is has the question terms: "fox, eats, <something>"
        demo.feedNetwork(network, demo.inputIterator());
        
        demo.setCallBack((sa) -> {
            Platform.runLater(() -> {
                // Returns the Term for the answer to what a fox eats.
                Term answer = demo.feedQuestion(network, sa);
                
                // Print it to standard out. (For now...)
                lArea.appendText("\t\t\t\t\t\tAnswer: \t" + answer.getTerm());
                
                // Cache fingerprints
                demo.writeCache();
            });
            return null;
        });
        
        
    }
}
