package org.numenta.nupic.examples.cortical_io;

import io.cortical.rest.model.Term;
import io.cortical.services.api.client.ApiException;
import javafx.application.Application;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;

import org.numenta.nupic.network.Network;



public class FoxEatsDemoView extends Region {
    private FoxEatsDemo demo;
    
    private String apiKey;
    
    private Pane logoPane;
    
    
    
    public FoxEatsDemoView(FoxEatsDemo demo, Application.Parameters params) {
        this.demo = demo;
        
        // Extract api key from arguments
        apiKey = params.getUnnamed().get(0).substring(2).trim();
        
        Button startButton = new Button("Start network");
        startButton.setOnAction(e -> { System.out.println("group x = " + logoPane.getLayoutX()); startNetwork(); });
        getChildren().add(startButton);
        
        logoPane = new CorticalLogoPane(this);
        logoPane.setOpacity(.9);
        getChildren().add(logoPane);
        
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
        String[] question = demo.feedNetwork(network, demo.inputIterator());
        
        // Returns the Term for the answer to what a fox eats.
        Term answer = demo.feedQuestion(network, question);
        
        // Print it to standard out. (For now...)
        System.out.println("What does a fox eat? Answer: " + answer.getTerm());
        
        // Cache fingerprints
        demo.writeCache();
    }
}
