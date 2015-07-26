package org.numenta.nupic.examples.cortical_io.breakingnews;

import io.cortical.fx.webstyle.example.FingerprintPane;
import io.cortical.fx.webstyle.example.TriplePanel;
import io.cortical.rest.model.Metric;
import io.cortical.services.RetinaApis;
import io.cortical.twitter.Algorithm;
import io.cortical.twitter.Tweet;
import io.cortical.twitter.TweetUtilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.BoundingBox;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Pair;

import org.numenta.nupic.Parameters.KEY;
import org.numenta.nupic.examples.cortical_io.breakingnews.BreakingNewsDemoView.Mode;
import org.numenta.nupic.network.Network;
import org.numenta.nupic.research.TemporalMemory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Demonstration of Cortical.io trend tracking functionality to analyze
 * and display detection of new trends and the adherence of new tweets to
 * the current detected trend.
 */
public class BreakingNewsDemo extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(BreakingNewsDemo.class);
    
    private static final String RETINA_NAME = "en_associative";    
    private static final String RETINA_IP = "api.cortical.io";
    
    private String apiKey;
    
    private Stream<String> dataStream;
    
    private List<String> jsonList = new ArrayList<>();
    
    private Algorithm algo;
    
    private int cursor = 0;
    
    private boolean isStopped = true;
    
    private Mode mode;
    
    private BreakingNewsDemoView view;
    
    private LinkedBlockingQueue<Pair<String, Integer>> queue = new LinkedBlockingQueue<>();
    
    private int recordNum;
    
    private ScrollPane mainViewScroll;
    
    

    /**
     * Constructs a new BreakingNewsDemo
     */
    public BreakingNewsDemo() {}
    
    
    public void runAlgorithm(Algorithm algo, Stream<String> input) {
        input.forEach(s -> {
            runOne(algo, s);
        });
    }
    
    /**
     * Runs one computation cycle on the tweet processing apparatus.
     * 
     * @param algo          the {@link Algorithm} to run.
     * @param jsonEntry     the tweet in json string form.
     */
    public void runOne(Algorithm algo, String jsonEntry) {
        algo.compute(Tweet.fromJson(jsonEntry, recordNum++));
    }
    
    /**
     * Returns the HTM {@link org.numenta.nupic.Parameters} object
     * with optimized configurations.
     * 
     * @return
     */
    public org.numenta.nupic.Parameters getHTMParameters() {
        org.numenta.nupic.Parameters p = org.numenta.nupic.Parameters.getAllDefaultParameters();
        p.setParameterByKey(KEY.GLOBAL_INHIBITIONS, true);
        p.setParameterByKey(KEY.COLUMN_DIMENSIONS, new int[] { 16384 });
        p.setParameterByKey(KEY.INPUT_DIMENSIONS, new int[]{ 16384 });
        p.setParameterByKey(KEY.CELLS_PER_COLUMN, 32);
        p.setParameterByKey(KEY.NUM_ACTIVE_COLUMNS_PER_INH_AREA, 40.0);
        p.setParameterByKey(KEY.POTENTIAL_PCT, 0.8);
        p.setParameterByKey(KEY.SYN_PERM_CONNECTED,0.1);
        p.setParameterByKey(KEY.SYN_PERM_ACTIVE_INC, 0.0001);
        p.setParameterByKey(KEY.SYN_PERM_INACTIVE_DEC, 0.0005);
        p.setParameterByKey(KEY.MAX_BOOST, 1.0);
        
        p.setParameterByKey(KEY.LEARNING_RADIUS, 2048);
        p.setParameterByKey(KEY.CONNECTED_PERMANENCE, 0.5);
        p.setParameterByKey(KEY.MAX_NEW_SYNAPSE_COUNT, 20);
        p.setParameterByKey(KEY.INITIAL_PERMANENCE, 0.21);
        p.setParameterByKey(KEY.PERMANENCE_INCREMENT, 0.1);
        p.setParameterByKey(KEY.PERMANENCE_DECREMENT, 0.1);
        p.setParameterByKey(KEY.MIN_THRESHOLD, 9);
        p.setParameterByKey(KEY.ACTIVATION_THRESHOLD, 12);
        
        p.setParameterByKey(KEY.CLIP_INPUT, false);
        
        return p;
    }
    
    /**
     * Returns a {@link Stream} constructed from the specified path
     * 
     * @param path
     * @return
     */
    public Stream<String> getFileStream(String path) {
        try {
            return Files.lines(new File(path).toPath());
        }catch(IOException e) {
            LOGGER.error("Failed to load indicated file: " + path);
            try {
                InputStream is = getClass().getResourceAsStream("/BBCBreaking.txt");
                return new BufferedReader(new InputStreamReader(is)).lines();
            }catch(Exception ex) {
                LOGGER.error("Failed secondary attempt from jar.");
            }
            return Arrays.stream(new String[] { });
        }
    }
    
    public Stream<String> createDataStream() {
        if(dataStream != null) {
            dataStream.close();
        }
        
        dataStream = getFileStream("./src/main/resources/BBCBreaking.txt");
        dataStream = TweetUtilities.getSortedStream(dataStream);
        return dataStream;
    }
    
    public void start() {
        isStopped = false;
        
        if(jsonList == null || jsonList.isEmpty()) {
            jsonList = createDataStream().collect(Collectors.toList());
        }
        
        if(cursor == jsonList.size() - 1) {
            // Display "finished" state
            return;
        }
        
        if(mode == Mode.AUTO) {
            createRunner().start();
            
            (new Thread() {
                public void run() {
                    for(;cursor < jsonList.size();cursor++) {
                        if(isStopped) {
                            BreakingNewsDemo.this.stop();
                            break;
                        }
                        
                        queue.offer(new Pair<String, Integer>(jsonList.get(cursor), cursor));
                        
                        try { Thread.sleep(2000); }catch(Exception e) { e.printStackTrace(); }
                    }
                }
            }).start();
        }else{
            view.runDisableProperty().set(false);
        }
    }
    
    public void stop() {
        isStopped = true;
        view.runDisableProperty().set(true);
    }
    
    public Thread createRunner() {
        return new Thread(() -> {
           while(!isStopped) {
               while(queue.size() > 0) {
                   try {
                       Pair<String, Integer> entry = queue.take();
                       runOne(entry.getKey(), entry.getValue());
                   }catch(Exception e) {
                       e.printStackTrace();
                   }
               }
           }
        });
    }
    
    public void runOne(String json, int index) {
        // Execute the algorithm for the tweet
        runOne(algo, json);
        
        double result = algo.getAnomaly();
        Tweet tweet = algo.getCurrentTweet();
        int[] prediction = algo.getPrevPrediction();
        
        Platform.runLater(() -> {
            // Update the Tweet display
            updateTweetDisplay(tweet);
            
            // Update the chart display
            updateChart(tweet, result);
            
            // Show fingerprints
            popuplateFingerPrintDisplay(tweet, prediction);
            
            // Log Activity to Activity Monitor Display
            logActivity(tweet);
        });
    }
    
    /**
     * Updates the view of the current {@link Tweet} being processed
     * @param tweet
     */
    private void updateTweetDisplay(Tweet tweet) {
        view.inputPaneProperty().get().getKey().setText(tweet.getJson());
        view.inputPaneProperty().get().getValue().setText(tweet.getText());
        view.currentLabelProperty().get().set("Current Tweet: " + tweet.getRecordNum());
        view.queueDisplayProperty().get().textProperty().set("Processing Queue Size: " + queue.size());
    }
    
    /**
     * Updates the chart with a new series entry.
     * 
     * @param tweet
     * @param result
     */
    private void updateChart(Tweet tweet, double result) {
        String[] dateStr = TweetUtilities.getJsonNode(tweet.getJson()).get("created_at").asText().split("\\s");
        String day = dateStr[0].concat(", ").concat(dateStr[1]).concat(" ").concat(dateStr[2]);
        String time = dateStr[3];
        
        XYChart.Series<String, Number> series = view.chartSeriesProperty().get();
        if(series.getData().size() == 6) {
            series.getData().remove(0);
        }
        series.getData().add(new XYChart.Data<String, Number>(day +"\n" + time, result));
    }
    
    private void popuplateFingerPrintDisplay(Tweet tweet, int[] prediction) {
        TriplePanel panel = view.fingerprintPanelProperty().get();
        ((FingerprintPane)panel.getLeftPane()).setSDR(tweet.getFingerprints().get(0).getPositions());
        if(prediction != null && prediction.length > 0) {
            ((FingerprintPane)panel.getRightPane()).setSDR(prediction);
            ((FingerprintPane)panel.getMiddlePane()).setSDR(tweet.getFingerprints().get(0).getPositions());
            ((FingerprintPane)panel.getMiddlePane()).addSDR(prediction, 1);
        }
    }
    
    private void logActivity(Tweet tweet) {
        List<Tweet> sims = algo.getSimilarityHistory();
        if(sims != null && sims.size() > 0) {
            TextArea similarityArea = view.rightActivityPanelProperty().get();
            similarityArea.appendText("\n");
            similarityArea.appendText("==========================\n");
            similarityArea.appendText("[" + tweet.getRecordNum()+"] Similar Tweets:\n");
            for(Tweet t : sims) {
                String[] dStr = TweetUtilities.getJsonNode(t.getJson()).get("created_at").asText().split("\\s");
                String d = dStr[0].concat(", ").concat(dStr[1]).concat(" ").concat(dStr[2]);
                String ti = dStr[3];
                
                similarityArea.appendText("        [" + d + ", " + ti +"] " + t.getText() + "\n");
            }
        }
        
        TextArea activityArea = view.leftActivityPanelProperty().get();
        activityArea.appendText("\n");
        activityArea.appendText("==========================\n");
        activityArea.appendText("[" + tweet.getRecordNum()+"] Anomaly Score: " + tweet.getAnomaly()+"\n");
        Metric metric = algo.getSimilarities();
        if(metric != null) {
            activityArea.appendText("    Overlapping Left Right: " + metric.getOverlappingLeftRight() +"\n");
            activityArea.appendText("    Overlapping Right Left: " + metric.getOverlappingRightLeft() +"\n");
            activityArea.appendText("         Euclidean Distance: " + metric.getEuclideanDistance() +"\n");
        }
    }
    
    public Algorithm createAlgorithm() {
        org.numenta.nupic.Parameters p = getHTMParameters();
        Network network = Network.create("TestNetwork", p)
            .add(Network.createRegion("R1")
                .add(Network.createLayer("Layer 2/3", p)
                    .add(new TemporalMemory())));
               
        RetinaApis ra = new RetinaApis(RETINA_NAME, RETINA_IP, apiKey);
        
        algo = new StrictHackathonAlgorithm(ra, network);
        
        return algo;
    }
    
    public void configureView() {
        view = new BreakingNewsDemoView();
        view.autoModeProperty().addListener((v, o, n) -> { this.mode = n; });
        this.mode = view.autoModeProperty().get();
        view.startActionProperty().addListener((v, o, n) -> { 
            if(n) {
                if(mode == Mode.AUTO) cursor++;
                start();
            }else{
                stop();
            }
        });
        view.runOneProperty().addListener((v, o, n) -> { 
            this.cursor += n.intValue();
            runOne(jsonList.get(cursor), cursor);
        });
        view.flipStateProperty().addListener((v, o, n) -> {
            view.flipPaneProperty().get().flip();
        });
        
        view.setPrefSize(1370, 1160);
        view.setMinSize(1370, 1160);
        
        mainViewScroll  = new ScrollPane();
        mainViewScroll.setViewportBounds(new BoundingBox(0, 0, 1370, 1160));
        
        mainViewScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
        mainViewScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        mainViewScroll.setContent(view);
        mainViewScroll.viewportBoundsProperty().addListener((v, o, n) -> {
            view.setPrefSize(Math.max(1370, n.getMaxX()), Math.max(1160, n.getMaxY()));//1370, 1160);
        });
        
        createDataStream();
        createAlgorithm();
    }
    
    public void showView(Stage stage) {
        Scene scene = new Scene(mainViewScroll, 1370, 1160, Color.WHITE);
        stage.setScene(scene);
        stage.setMinWidth(1370);
        stage.show();
        
        Rectangle2D primScreenBounds = Screen.getPrimary().getVisualBounds(); 
        stage.setX((primScreenBounds.getWidth() - stage.getWidth()) / 2); 
        stage.setY((primScreenBounds.getHeight() - stage.getHeight()) / 4);
    }
    
    @Override
    public void start(Stage stage) throws Exception {
        Application.Parameters params = getParameters();
        List<String> paramList = params.getUnnamed();
        
        // Check for the existence of a proper API Key
        if(paramList.size() < 1 || !paramList.get(0).startsWith("-K")) {
            throw new IllegalStateException("Demo must be started with arguments [-K]<your-api-key>");
        }
        
        this.apiKey = paramList.get(0).substring(2);
        
        configureView();
        
        showView(stage);
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}
