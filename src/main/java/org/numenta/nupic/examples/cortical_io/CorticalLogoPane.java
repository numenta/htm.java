package org.numenta.nupic.examples.cortical_io;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/**
 * Acts like a background panel or watermark one can add to the 
 * background of an application view. The opacity of the background
 * logo can be adjusted by setting the opacity directly on this object.
 * (See the {@link CorticalLogoPane#setOpacity(double)} method which is 
 * inherited from {@link Node}).
 * 
 * The logo is drawn "programmatically", but the code could be used to
 * generate an image file if that is desired...
 *  
 * @author cogmission
 * @see FoxEatsDemo
 * @see FoxEatsDemoView
 */
public class CorticalLogoPane extends Pane {
    /** The Cortical.io lighter blue */
    private static final Color LIGHT = Color.rgb(44, 90, 159);
    /** The Cortical.io darker blue */
    private static final Color DARK = Color.rgb(29, 59, 96);
    
    boolean w_scaleInitted = false;
    boolean h_scaleInitted = false;
    
    /**
     * Constructs a new logo pane with the Cortical.io logo.
     * 
     * @param parent    the {@link Region} parent containing this panel
     *                  as its background. (Needed in order to position
     *                  and resize the logo dynamically).
     */
    public CorticalLogoPane(Region parent) {
        Line l = new Line(101.0, 21, 150.0, 101.0);
        l.setStroke(Color.rgb(29, 59, 96));
        l.setStrokeWidth(1.0);
        l.toBack();
        
        Line l2 = new Line(198.0, 179.0, 149.0, 100.0);
        l2.setStroke(Color.rgb(29, 59, 96));
        l2.setStrokeWidth(1.0);
        l2.toBack();
        
        Polygon up1 = new Polygon();
        up1.getPoints().addAll(new Double[] {
           100.0, 20.0, 
           50.0, 100.0,
           150.0, 100.0
        });
        up1.setFill(DARK);
        
        Polygon up2 = new Polygon();
        up2.getPoints().addAll(new Double[] {
           100.0, 20.0, 
           200.0, 20.0,
           150.0, 101.0
        });
        up2.setFill(DARK);
        
        Polygon up3 = new Polygon();
        up3.getPoints().addAll(new Double[] {
           200.0, 20.0, 
           149.0, 100.0,
           248.0, 100.0
        });
        up3.setFill(LIGHT);
        
        Polygon dwn1 = new Polygon();
        dwn1.getPoints().addAll(new Double[] {
           50.0, 100.0, 
           150.0, 100.0,
           99.0, 180.0
        });
        dwn1.setFill(LIGHT);
        
        Polygon dwn2 = new Polygon();
        dwn2.getPoints().addAll(new Double[] {
           149.0, 100.0, 
           99.0, 180.0,
           199.0, 180.0
        });
        dwn2.setFill(DARK);
        
        Polygon dwn3 = new Polygon();
        dwn3.getPoints().addAll(new Double[] {
           148.0, 100.0, 
           248.0, 100.0,
           199.0, 180.0
        });
        dwn3.setFill(DARK);
        
        Circle c1 = new Circle(148, 100, 42);
        Circle c2 = new Circle(148, 100, 30);
        Shape s1 = Shape.subtract(c1, c2);
        s1.setFill(Color.WHITE);
        
        Rectangle topV = new Rectangle(142, 30, 13, 30);
        topV.setFill(Color.WHITE);
        
        Rectangle topH = new Rectangle(122, 29, 51, 13);
        topH.setFill(Color.WHITE);
        
        Rectangle bottomV = new Rectangle(142, 140, 13, 30);
        bottomV.setFill(Color.WHITE);
        
        Rectangle bottomH = new Rectangle(122, 158, 51, 13);
        bottomH.setFill(Color.WHITE);
        
        getChildren().add(up1);
        getChildren().add(up2);
        getChildren().add(l);
        getChildren().add(l2);
        getChildren().add(up3);
        getChildren().add(dwn1);
        getChildren().add(dwn2);
        getChildren().add(dwn3);
        
        toBack();
        
        getChildren().add(s1);
        getChildren().add(topV);
        getChildren().add(topH);
        getChildren().add(bottomV);
        getChildren().add(bottomH);
        
        for(Node n : getChildren()) {
            n.translateXProperty().set(-50);
            n.translateYProperty().set(-20);
        }
        
        setManaged(false);
        
        parent.heightProperty().addListener((v, o, n) -> {
            if(h_scaleInitted) return;
            double biph = getBoundsInParent().getHeight();
            setLayoutY((parent.getScene().getHeight() / 2) - (biph / 2));
            h_scaleInitted = true;
        });
        
        parent.widthProperty().addListener((v, o, n) -> {
            double ratio = n.doubleValue() / (!w_scaleInitted ? 198 : o.doubleValue());
            
            setScaleX(getScaleX() * ratio);
            setScaleY(getScaleY() * ratio);
            double bipw = getBoundsInParent().getWidth();
            double biph = getBoundsInParent().getHeight();
            setLayoutX((parent.getScene().getWidth() / 2) - (bipw / 2));
            setLayoutY((parent.getScene().getHeight() / 2) - (biph / 2));
            
            w_scaleInitted = true;
        });
    }
    
}
