/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;

import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.ValueAxis;
import javafx.scene.control.Label;

import javafx.geometry.Pos;

import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>ColorScaleLegendFactory</code> class creates color scale legends
 * for images that show double values as colors.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class ColorScaleLegendFactory {

  private static final Logger LOGGER = Logger.getLogger (ColorScaleLegendFactory.class.getName());

  /** The converter between data values and colors. */
  private ObjectProperty<DoubleToColorConverter> converterProp;

  /** The label for the axis. */
  private StringProperty labelProp;

  /** The hint for log scale, true if the color scale should show log scale tick values. */
  private BooleanProperty logScaleHintProp;

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the converter from double values to colors (default null).
   *
   * @return the converter property.
   */
  public ObjectProperty<DoubleToColorConverter> converterProperty () { return (converterProp); }

  /**
   * Gets the label for the axis (default "").
   *
   * @return the label property.
   */
  public StringProperty labelProperty () { return (labelProp); }

  /**
   * Gets the log scale hint value, true to display a log scale (default false).
   *
   * @return the log scale hint property.
   */
  public BooleanProperty logScaleHintProperty () { return (logScaleHintProp); }

  /////////////////////////////////////////////////////////////////

  public ColorScaleLegendFactory () {
  
    converterProp = new SimpleObjectProperty<> (this, "converter", null);
    labelProp = new SimpleStringProperty (this, "label", "");
    logScaleHintProp = new SimpleBooleanProperty (this, "logScaleHint", false);
  
  } // ColorScaleLegendFactory

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a legend to display a color scale using this factory's current
   * settings.
   *
   * @return the color scale legend.
   */
  public Region createLegend () {
  
    int width = 300;
    int height = 15;

    // Convert a series of data values to colors
    double min = converterProp.getValue().getMin();
    double max = converterProp.getValue().getMax();
    double[] values = new double[width];
    if (logScaleHintProp.getValue()) {
      double m = (Math.log10 (max) - Math.log10 (min))/(width-1);
      for (int i = 0; i < width; i++) values[i] = Math.pow (10, m*i + Math.log10 (min));
    } // if
    else {
      double m = (max-min)/(width-1);
      for (int i = 0; i < width; i++) values[i] = m*i + min;
    } // else

    int[] colors = new int[width];
    converterProp.getValue().convert (values, 0, colors, 0, width);

    // Create the color bar for the scale
    var image = new WritableImage (width, height);
    var writer = image.getPixelWriter();
    for (int x = 0; x < width; x++) {
      int color = colors[x];
      for (int y = 0; y < height; y++) writer.setArgb (x, y, color);
    } // for
    var view = new ImageView (image);

    // Create a vertical stack: color bar, axis, label
    var legend = new VBox();
    legend.setAlignment (Pos.CENTER);

    var pane = new BorderPane();
    pane.setCenter (view);
//    pane.setStyle (
//      "-fx-border-color: rgb(160,160,160);" +
//      "-fx-border-width: 1;" +
//      "-fx-border-style: solid;"
//    );
    pane.setStyle ("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,1.0), 10, 0.0, 0, 0);");

    pane.setMinWidth (0);
    pane.setPrefWidth (300);
    view.fitWidthProperty().bind (pane.widthProperty().subtract (2));
    view.fitWidthProperty().bind (pane.widthProperty().add (1));
    legend.getChildren().add (pane);

    ValueAxis<Number> axis = null;
    if (logScaleHintProp.getValue()) {
      try {
        axis = new LogarithmicAxis (min, max);
      } // try
      catch (IllegalLogarithmicRangeException e) {
        LOGGER.log (Level.WARNING, "Cannot create log scale axis", e);
      } // catch
    } // if
    else {
      double tickUnit = getLinearTickInterval (min, max, 6);
      axis = new NumberAxis (min, max, tickUnit);
      int base = (int) Math.round (tickUnit / Math.pow (10, Math.floor (Math.log10 (tickUnit))));
      int minorTickCount = (base == 5 ? 5 : base == 2 ? 4 : 2);
      axis.setMinorTickCount (minorTickCount);
    } // else
    axis.setAnimated (false);
    axis.getStyleClass().add ("legend-axis");
    legend.getChildren().add (axis);

    var label = new Label (labelProp.getValue());
    label.getStyleClass().add ("legend-label");
    legend.getChildren().add (label);

    return (legend);
  
  } // createLegend

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an appropriate tick mark interval given the tick
   * specifications.  This method works well for linear enhancement
   * scales.
   *
   * @param min the data value minimum.
   * @param max the data value maximum.
   * @param desired the approximate number of desired ticks.
   *
   * @return the tick interval.
   */
  private static double getLinearTickInterval (
    double min,
    double max,
    int desired
  ) {

    int[] bases = new int[] {1, 2, 5, 10, 20, 50};
    double range = max-min;
    int exp = (int) Math.floor (Math.log10 (range)) - 1;
    double ticks = range / (bases[0]*Math.pow(10,exp)) + 1;
    int iticks = 0;
    for (int i = 1; i < bases.length; i++) {
      double newticks = range / (bases[i]*Math.pow(10,exp)) + 1;
      if (Math.abs (newticks - desired) < Math.abs (ticks - desired)) {
        ticks = newticks;
        iticks = i;
      } // if
    } // for
    double interval = bases[iticks]*Math.pow(10,exp);

    return (interval);

  } // getLinearTickInterval

  /////////////////////////////////////////////////////////////////

} // ColorScaleLegendFactory


