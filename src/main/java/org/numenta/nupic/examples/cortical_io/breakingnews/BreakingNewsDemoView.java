package org.numenta.nupic.examples.cortical_io.breakingnews;

import io.cortical.fx.webstyle.CorticalLogoBackground;
import io.cortical.fx.webstyle.LabelledRadiusPane;
import io.cortical.fx.webstyle.RadiusFlipPane;
import io.cortical.fx.webstyle.SegmentedButtonBar;
import io.cortical.fx.webstyle.example.DualPanel;
import io.cortical.fx.webstyle.example.FingerprintPane;
import io.cortical.fx.webstyle.example.LogoTitlePane;
import io.cortical.fx.webstyle.example.TriplePanel;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.Pair;


public class BreakingNewsDemoView extends GridPane {
    enum Mode { AUTO, MANUAL };
    enum FlipState { ON_FRONT, ON_BACK };
    
    private Button runOneBtn;
    
    private LineChart<String, Number> chart;
    
    private XYChart.Series<String, Number> series;
    
    private Mode mode = Mode.AUTO;
    private FlipState state = FlipState.ON_FRONT;
    
    private ObjectProperty<Pair<TextArea, TextArea>> inputPaneProperty = new SimpleObjectProperty<>();
    private BooleanProperty startActionProperty = new SimpleBooleanProperty(false);
    private ObjectProperty<Mode> autoModeProperty = new SimpleObjectProperty<Mode>(mode);
    private IntegerProperty runOneProperty = new SimpleIntegerProperty(-1);
    private ObjectProperty<XYChart.Series<String, Number>> chartSeriesProperty = new SimpleObjectProperty<>();
    private BooleanProperty runDisableProperty = new SimpleBooleanProperty(true);
    private ObjectProperty<RadiusFlipPane> flipPaneProperty = new SimpleObjectProperty<>();
    private ObjectProperty<FlipState> flipStateProperty = new SimpleObjectProperty<FlipState>(state);
    private ObjectProperty<TriplePanel> fingerprintPanelProperty = new SimpleObjectProperty<TriplePanel>();
    private ObjectProperty<TextArea> leftActivityPanelProperty = new SimpleObjectProperty<>();
    private ObjectProperty<TextArea> rightActivityPanelProperty = new SimpleObjectProperty<>();
    private ObjectProperty<StringProperty> currentLabelProperty = new SimpleObjectProperty<>();
    private ObjectProperty<Label> queueDisplayProperty = new SimpleObjectProperty<>();
    
    public BreakingNewsDemoView() {
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
        header.setTitleText("Breaking News");
        header.setSubTitleText("a Twitter trend tracking demo...");
        
        HBox buttonBar = createSegmentedButtonBar();
        
        DualPanel inputPane = createInputPane();
        inputPane.panelHeightProperty().setValue(105);        
        
        LabelledRadiusPane chartPane = new LabelledRadiusPane("Trend");
        chartPane.getChildren().add(createChart(chartPane));
        
        SegmentedButtonBar log = createShowLogButton(vBox);
        BorderPane logButton = new BorderPane();
        logButton.prefWidthProperty().bind(vBox.widthProperty());
        logButton.setPrefHeight(10);
        logButton.setCenter(log);
        
        TriplePanel fpDisplay = createFingerprintDisplay();
        LabelledRadiusPane loggerPane = createActivityPane();
        loggerPane.prefWidthProperty().bind(fpDisplay.widthProperty());
        loggerPane.prefHeightProperty().bind(fpDisplay.heightProperty());
        
        RadiusFlipPane flip = new RadiusFlipPane(fpDisplay, loggerPane);
        fpDisplay.prefWidthProperty().bind(vBox.widthProperty());
        flipPaneProperty.set(flip);
        
        vBox.getChildren().addAll(header, buttonBar, inputPane, chartPane, logButton, flip);
        
        stack.getChildren().addAll(backGround, vBox);
        
        // Main Layout: 3 columns, 1 row
        add(h, 0, 0);
        add(stack, 1, 0);
        add(h2, 2, 0);
    }
    
    public SegmentedButtonBar createShowLogButton(VBox vBox) {
        Button logButton = new Button("Show Activity");
        logButton.getStyleClass().add("only");
        logButton.setStyle("-fx-font-size: 10; -fx-padding: 4 12 4 12; -fx-height: 10;");
        logButton.setPrefHeight(10);
        logButton.setPrefWidth(150);
        vBox.widthProperty().addListener((v, o, n) -> {
            logButton.setLayoutX(n.doubleValue() / 2.0 - 75);
        });
        logButton.setOnAction(e -> {
            flipStateProperty.set(state = state == FlipState.ON_FRONT ? FlipState.ON_BACK : FlipState.ON_FRONT);
            logButton.setText(state == FlipState.ON_FRONT ? "Show Activity" : "Show Fingerprints");
        });
        
        SegmentedButtonBar buttonBar3 = new SegmentedButtonBar();
        buttonBar3.getChildren().addAll(logButton);
        buttonBar3.setPrefHeight(10);
        
        return buttonBar3;
    }
    
    public TriplePanel createFingerprintDisplay() {
        // Left and Right panes of DualPanel
        LabelledRadiusPane left2 = createFingerprintPane("Fingerprint");
        LabelledRadiusPane middle2 = createFingerprintPane("Comparison");
        LabelledRadiusPane right2 = createFingerprintPane("Prediction");
        ((FingerprintPane)right2).setColorIndex(1);
        
        
        TriplePanel fpDisplay = new TriplePanel();
        fpDisplay.setSpacing(10);
        fpDisplay.panelHeightProperty().setValue(435);
        fpDisplay.setLeftPane(left2);
        fpDisplay.setMiddlePane(middle2);
        fpDisplay.setRightPane(right2);
        
        fingerprintPanelProperty.set(fpDisplay);
        
        return fpDisplay;
    }
    
    public DualPanel createInputPane() {
        DualPanel retVal = new DualPanel();
        
        LabelledRadiusPane left = new LabelledRadiusPane("Input Tweet");
        TextArea lArea = new TextArea();
        lArea.setWrapText(true);
        lArea.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 16));
        lArea.layoutYProperty().bind(left.labelHeightProperty().add(10));
        left.layoutBoundsProperty().addListener((v, o, n) -> {
            lArea.setLayoutX(10);
            lArea.setPrefWidth(n.getWidth() - 20);
            lArea.setPrefHeight(n.getHeight() - left.labelHeightProperty().get() - 20);
        });
        
        Label queueLabel = new Label("Processing Queue Size:");
        queueLabel.layoutXProperty().bind(lArea.widthProperty().subtract(queueLabel.getLayoutBounds().getWidth() + 330));
        queueLabel.setLayoutY(lArea.getLayoutY() - queueLabel.getLayoutBounds().getHeight() - 35);
        queueLabel.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
        queueLabel.setTextFill(Color.rgb(00, 70, 107));
        queueDisplayProperty.set(queueLabel);
        
        Label curLabel = new Label("Current Tweet:");
        curLabel.layoutXProperty().bind(lArea.widthProperty().subtract(curLabel.getLayoutBounds().getWidth() + 110));
        curLabel.setLayoutY(lArea.getLayoutY() - curLabel.getLayoutBounds().getHeight() - 35);
        curLabel.setFont(Font.font("Helvetica", FontWeight.BOLD, 12));
        curLabel.setTextFill(Color.rgb(00, 70, 107));
        currentLabelProperty.set(curLabel.textProperty());
        
        left.getChildren().addAll(lArea, queueLabel, curLabel);
        
        LabelledRadiusPane right = new LabelledRadiusPane("Scrubbed Tweet");
        TextArea rArea = new TextArea();
        rArea.setWrapText(true);
        rArea.setFont(Font.font("Helvetica", FontWeight.MEDIUM, 16));
        rArea.layoutYProperty().bind(right.labelHeightProperty().add(10));
        right.layoutBoundsProperty().addListener((v, o, n) -> {
            rArea.setLayoutX(10);
            rArea.setPrefWidth(n.getWidth() - 20);
            rArea.setPrefHeight(n.getHeight() - right.labelHeightProperty().get() - 20);
        });
        right.getChildren().add(rArea);
        
        retVal.setLeftPane(left);
        retVal.setRightPane(right);
        
        inputPaneProperty.set(new Pair<>(lArea, rArea));
        
        return retVal;
    }
    
    /**
     * Demonstrates the construction and usage of the {@link SegmentedButtonBar}
     * @return
     */
    public HBox createSegmentedButtonBar() {
        ToggleButton button1 = new ToggleButton("Start");
        button1.getStyleClass().addAll("first");
        button1.setOnAction(e -> {
            Platform.runLater(() -> {
                if(mode == Mode.MANUAL) {
                    runOneBtn.setDisable(false);
                }
                startActionProperty.set(true);
            });
        });
        ToggleButton button2 = new ToggleButton("Stop");
        button2.getStyleClass().addAll("last");
        button2.setOnAction(e -> {
            Platform.runLater(() -> {
                startActionProperty.set(false);
            });
        });
        ToggleButton button3 = new ToggleButton("Auto");
        button3.getStyleClass().addAll("first");
        button3.setOnAction(e -> {
            Platform.runLater(() -> {
                mode = Mode.AUTO;
                autoModeProperty.set(mode);
            });
        });
        ToggleButton button4 = new ToggleButton("Manual");
        button4.getStyleClass().addAll("last");
        button4.setOnAction(e -> {
            Platform.runLater(() -> {
                mode = Mode.MANUAL;
                autoModeProperty.set(mode);
            });
        });
        Button button5 = runOneBtn = new Button("Run One");
        button5.getStyleClass().addAll("only");
        button5.setDisable(true);
        button5.setOnAction(e -> {
            Platform.runLater(() -> {
                runOneProperty.set(runOneProperty.get() + 1);
            });
        });
        
        runDisableProperty.addListener((v, o, n) -> { button5.setDisable(n); });
        
        ToggleGroup group = new ToggleGroup();
        group.getToggles().addAll(button1, button2);
        group.selectToggle(button2);
        
        ToggleGroup group2 = new ToggleGroup();
        group2.getToggles().addAll(button3, button4);
        group2.selectToggle(button3);
        group2.selectedToggleProperty().addListener((v, o, n) -> {
            if(n == null) return;
            
            if(n.equals(button4)) {
                Platform.runLater(() -> {
                    mode = Mode.MANUAL;
                    group.selectToggle(button2);
                    startActionProperty.set(false);
                });
            }else{
                Platform.runLater(() -> {
                    mode = Mode.AUTO;
                    startActionProperty.set(false);
                    group.selectToggle(button2);
                    button5.setDisable(true);
                });
            }
        });
        
        HBox displayBox = new HBox();
        displayBox.setSpacing(20);
        displayBox.setAlignment(Pos.CENTER);
        
        SegmentedButtonBar  buttonBar = new SegmentedButtonBar();
        buttonBar.getChildren().addAll(button1, button2);
        
        SegmentedButtonBar buttonBar2 = new SegmentedButtonBar();
        buttonBar2.getChildren().addAll(button3, button4);
        
        SegmentedButtonBar buttonBar3 = new SegmentedButtonBar();
        buttonBar3.getChildren().addAll(button5);
        
        displayBox.getChildren().addAll(buttonBar, buttonBar2, buttonBar3);
        
        return displayBox;
    }
    
    public LineChart<String, Number> createChart(LabelledRadiusPane pane) {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        
        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Tweet Trend Analysis");
        chart.setCreateSymbols(false);
        chart.setLegendVisible(false);

        xAxis.setLabel("Time of Tweet");
        yAxis.setUpperBound(1.0);
        yAxis.setLowerBound(0.0);
        yAxis.setLabel("Anomaly\n  Score");
        yAxis.setForceZeroInRange(true);
        
        series = new XYChart.Series<>();
        series.setName("Tweet Data");
        chart.getData().add(series);
        chartSeriesProperty.set(series);
        
        Node line = series.getNode().lookup(".chart-series-line");
        line.setStyle("-fx-stroke: rgb(20, 164, 220)");
        
        chart.setPrefWidth(1200);
        chart.setPrefHeight(275);
        chart.setLayoutY(pane.labelHeightProperty().get() + 10);
        
        return chart;
    }
    
    /**
     * Creates the monitor view
     * @return
     */
    public LabelledRadiusPane createActivityPane() {
        LabelledRadiusPane retVal = new LabelledRadiusPane("Activity Monitor");
        HBox h = new HBox();
        h.setFillHeight(true);
        h.setSpacing(15);
        h.setPadding(new Insets(0, 10, 10, 15));
        h.prefWidthProperty().bind(retVal.widthProperty());
        h.layoutYProperty().bind(retVal.labelHeightProperty().add(20));
        
        TextArea area = new TextArea();
        area.prefWidthProperty().bind(h.widthProperty().subtract(30).divide(2));
        area.prefHeightProperty().bind(retVal.heightProperty().subtract(60));
        area.setLayoutY(retVal.labelHeightProperty().add(0).get());
        leftActivityPanelProperty.set(area);
        
        TextArea area2 = new TextArea();
        area2.prefWidthProperty().bind(h.widthProperty().subtract(60).divide(2));
        area2.prefHeightProperty().bind(retVal.heightProperty().subtract(60));
        area2.setLayoutY(retVal.labelHeightProperty().add(0).get());
        area2.textProperty().addListener((v, o, n) -> {
            area2.setScrollTop(Double.MAX_VALUE);
            area2.setScrollLeft(Double.MAX_VALUE);
        });
        rightActivityPanelProperty.set(area2);
        h.getChildren().addAll(area, area2);
        
        Label l = new Label("Output");
        l.setFont(Font.font("Helvetica", FontWeight.BOLD, 14));
        l.setTextFill(Color.rgb(00, 70, 107));
        l.layoutXProperty().bind(area.widthProperty().divide(2).add(area.getLayoutX()).subtract(l.getWidth()));
        l.setLayoutY(area.getLayoutY() - l.getHeight());
        
        Label l2 = new Label("Similar Tweets");
        l2.setFont(Font.font("Helvetica", FontWeight.BOLD, 14));
        l2.setTextFill(Color.rgb(00, 70, 107));
        area2.layoutBoundsProperty().addListener((v, o, n) -> {
            l2.setLayoutX(area.getWidth() + 60 + area2.getWidth() / 2 - l2.getWidth());
        });
        l2.setLayoutY(area2.getLayoutY() - l.getHeight());
        
        retVal.getChildren().addAll(h, l, l2);
        
        return retVal;
    }
    
    public FingerprintPane createFingerprintPane(String text) {
        FingerprintPane pane = new FingerprintPane();
        pane.setLabelText(text);
        return pane;
    }
    
    public ObjectProperty<XYChart.Series<String, Number>> chartSeriesProperty() {
        return chartSeriesProperty;
    }
    
    public BooleanProperty startActionProperty() {
        return startActionProperty;
    }
    
    public ObjectProperty<Mode> autoModeProperty() {
        return autoModeProperty;
    }
    
    public IntegerProperty runOneProperty() {
        return runOneProperty;
    }
    
    public BooleanProperty runDisableProperty() {
        return runDisableProperty;
    }
    
    public ObjectProperty<Pair<TextArea, TextArea>> inputPaneProperty() {
        return inputPaneProperty;
    }
    
    public ObjectProperty<RadiusFlipPane> flipPaneProperty() {
        return flipPaneProperty;
    }
    
    public ObjectProperty<FlipState> flipStateProperty() {
        return flipStateProperty;
    }
    
    public ObjectProperty<TriplePanel> fingerprintPanelProperty() {
        return fingerprintPanelProperty;
    }
    
    public ObjectProperty<TextArea> leftActivityPanelProperty() {
        return leftActivityPanelProperty;
    }
    
    public ObjectProperty<TextArea> rightActivityPanelProperty() {
        return rightActivityPanelProperty;
    }
    
    public ObjectProperty<StringProperty> currentLabelProperty() {
        return currentLabelProperty;
    }
    
    public ObjectProperty<Label> queueDisplayProperty() {
        return queueDisplayProperty;
    }
}
