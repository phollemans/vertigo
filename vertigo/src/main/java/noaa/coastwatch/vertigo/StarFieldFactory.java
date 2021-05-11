/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javafx.scene.Group;
import javafx.scene.shape.Sphere;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Material;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>StarField</code> class creates a realistic-looking sphere of stars
 * of different apparent brightnesses and colours centered at the origin.
 */
public class StarFieldFactory {

  private static final Logger LOGGER = Logger.getLogger (StarFieldFactory.class.getName());

  private static final double MIN_MAG = -1.5;
  private static final double MAX_MAG = 4;
  private static final double CELESTIAL_RADIUS = 60000;
  private static final double STAR_MAX_RADIUS = 120;
  private static final String STAR_FILE = "bright_stars.csv";

  /////////////////////////////////////////////////////////////////

  protected StarFieldFactory () { }

  /////////////////////////////////////////////////////////////////

  public static StarFieldFactory getInstance() { return (new StarFieldFactory()); }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a group containing a field of stars.
   *
   * @param kilometerSize the size of one kilometer in model units.
   *
   * @return the star field as a group of nodes.
   */
  public Group createField (double kilometerSize) {

    double magScale = MAX_MAG - MIN_MAG;
    double starMaxRadius = 120*kilometerSize;
    double celestialRadius = 60000*kilometerSize;
    float[] pointCoords = new float[3];
    Group stars = new Group();
    
//    Material starMaterial = new PhongMaterial (Color.WHITE);

    double b = 2.898e6; // Wien's displacement constant

    try (var stream = getClass().getResourceAsStream (STAR_FILE)) {

      BufferedReader reader = new BufferedReader (new InputStreamReader (stream));
      String line;
      while ((line = reader.readLine()) != null) {

        // In the CSV file, the fields are stored as follows:
        // 0: ID number
        // 1: Name
        // 2: Magnitude
        // 3: Color index
        // 4: X coordinate
        // 5: Y coordinate
        // 6: Z coordinate
        // 7: Constellation

        String[] fields = line.split (",");

        // Discard any stars outside out magnitude limits
        double mag = Double.parseDouble (fields[2]);
        if (mag < MIN_MAG || mag > MAX_MAG) continue;

        // Compute the star position in model space
        double x = Double.parseDouble (fields[4]);
        double y = Double.parseDouble (fields[5]);
        double z = Double.parseDouble (fields[6]);
        double dist = Math.sqrt (x*x + y*y + z*z);
        double modelX = x/dist * celestialRadius;
        double modelY = -z/dist * celestialRadius;
        double modelZ = y/dist * celestialRadius;
        
        // We use radius here as a proxy for the star's visual brightness
//        double bright = 1.0 - (mag - MIN_MAG)/magScale;
        double radius = (1.0 - (mag - MIN_MAG)/magScale)*starMaxRadius;

        // Compute the colour using the color index converted to wavelength
        Color color;
        if (fields[3].length() != 0) {
          double bv = Double.parseDouble (fields[3]);
          double temp = 4600*((1/(0.92*bv + 1.7)) + (1/(0.92*bv + 0.62)));
          double wave = b/temp;
          int[] rgb = waveToRGB (wave);
          color = Color.rgb (rgb[0], rgb[1], rgb[2]);
          color = Color.hsb (color.getHue(), 0.25, 1.0);
        } // if
        else {
          color = Color.hsb (0, 0, 1.0);
        } // else
        Material starMaterial = new PhongMaterial (color);

        // Create the star as a very simple sphere
        var star = new Sphere (radius, 1);
//        var star = new javafx.scene.shape.Box (radius*2, radius*2, radius*2);
        star.setTranslateX (modelX);
        star.setTranslateY (modelY);
        star.setTranslateZ (modelZ);
        star.setMaterial (starMaterial);
        stars.getChildren().add (star);
      
      } // while

    } // try
    catch (Exception e) {
      LOGGER.log (Level.WARNING, "Error creating star field", e);
    } // catch

    LOGGER.fine ("Created star field with " + stars.getChildren().size() + " stars");

    return (stars);

  } // createField

  /////////////////////////////////////////////////////////////////

  /** Converts a wavelength to an RGB colour value. */
  private static int[] waveToRGB (double wave) {

    double factor;
    double red, green, blue;
    double gamma = 0.8;
    double intensityMax = 255;

    if ((wave >= 380) && (wave < 440)){
      red = - (wave - 440) / (440 - 380);
      green = 0.0;
      blue = 1.0;
    } // if

    else if ((wave >= 440) && (wave < 490)){
      red = 0.0;
      green = (wave - 440) / (490 - 440);
      blue = 1.0;
    } // else if
    
    else if ((wave >= 490) && (wave < 510)){
      red = 0.0;
      green = 1.0;
      blue = - (wave - 510) / (510 - 490);
    } // else if
    
    else if ((wave >= 510) && (wave < 580)){
      red = (wave - 510) / (580 - 510);
      green = 1.0;
      blue = 0.0;
    } // else if
    
    else if ((wave >= 580) && (wave < 645)){
      red = 1.0;
      green = - (wave - 645) / (645 - 580);
      blue = 0.0;
    } // else if
    
    else if ((wave >= 645) && (wave < 781)){
      red = 1.0;
      green = 0.0;
      blue = 0.0;
    } // else if
    
    else {
      red = 0.0;
      green = 0.0;
      blue = 0.0;
    } // else

    // Let the intensity fall off near the vision limits

    if ((wave >= 380) && (wave < 420)) {
      factor = 0.3 + 0.7*(wave - 380) / (420 - 380);
    } // if
    else if ((wave >= 420) && (wave < 701)) {
      factor = 1.0;
    } // else if
    else if ((wave >= 701) && (wave < 781)) {
      factor = 0.3 + 0.7*(780 - wave) / (780 - 700);
    } // else if
    else {
      factor = 0.0;
    } // else

    int[] rgb = new int[3];

    // Don't want 0^x = 1 for x != 0
    rgb[0] = red == 0.0 ? 0 : (int) Math.round (intensityMax * Math.pow (red * factor, gamma));
    rgb[1] = green == 0.0 ? 0 : (int) Math.round (intensityMax * Math.pow (green * factor, gamma));
    rgb[2] = blue == 0.0 ? 0 : (int) Math.round (intensityMax * Math.pow (blue * factor, gamma));

    return (rgb);

  } // waveToRGB

  /////////////////////////////////////////////////////////////////

} // StarFieldFactory
