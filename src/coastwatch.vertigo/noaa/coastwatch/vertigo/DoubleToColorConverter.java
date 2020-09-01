/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.DoubleToIntFunction;

/**
 * The <code>DoubleToColorConverter</code> class converts primitive double
 * array data values to int ARGB values using a color palette and mapping
 * function.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class DoubleToColorConverter implements DataConverter<double[], int[]> {

  private static final double LOG10 = Math.log (10);

  // Variables
  // ---------
  
  /** The colormap that contains the colors to use for image pixels. */
  private int[] colorMap;

  /** The function that maps double values to integer indices in the colormap. */
  private DoubleToIntFunction function;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a linear function color converter.
   *
   * @param min the minimum data value to convert.
   * @param max the maximum data value to convert.
   * @param map the colors for mapping.  The color at map[0] is used for missing
   * data, and map[1..colors-1] as the valid values.
   */
  public static DoubleToColorConverter linearInstance (
    double min,
    double max,
    int[] map
  ) {
  
    int colors = map.length-1;
    DoubleToIntFunction func = value -> {
      int intValue;
      if (Double.isNaN (value)) {
        intValue = 0;
      } // if
      else {
        double norm = (value - min) / (max - min);
        if (norm < 0) norm = 0;
        else if (norm > 1) norm = 1;
        intValue = (int) Math.round (norm * (colors-1)) + 1;
      } // else
      return (intValue);
    };
  
    var converter = new DoubleToColorConverter (map, func);
    return (converter);

  } // linearInstance

  /////////////////////////////////////////////////////////////////

  /** Computes the log base 10 of a number. */
  private static double log10 (double value) { return (Math.log (value) / LOG10); }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a logarithmic function color converter.
   *
   * @param min the minimum data value to convert.
   * @param max the maximum data value to convert.
   * @param map the colors for mapping.  The color at map[0] is used for missing
   * data, and map[1..colors-1] as the valid values.
   */
  public static DoubleToColorConverter logInstance (
    double min,
    double max,
    int[] map
  ) {
  
    int colors = map.length-1;
    double slope = 1.0 / (log10 (max) - log10 (min));
    double inter = -slope * log10 (min);
    DoubleToIntFunction func = value -> {
      int intValue;
      if (Double.isNaN (value)) {
        intValue = 0;
      } // if
      else {
        double norm = slope*log10 (value) + inter;
        if (norm < 0) norm = 0;
        else if (norm > 1) norm = 1;
        intValue = (int) Math.round (norm * (colors-1)) + 1;
      } // else
      return (intValue);
    };

    var converter = new DoubleToColorConverter (map, func);
    return (converter);

  } // logInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new converter.
   *
   * @param colorMap the colormap that contains the colors to use for image
   * pixels as ARGB values.
   * @param function the function that maps double values to integer indices in the colormap.
   */
  public DoubleToColorConverter (
    int[] colorMap,
    DoubleToIntFunction function
  ) {
  
    this.colorMap = colorMap;
    this.function = function;
  
  } // DoubleToColorConverter

  /////////////////////////////////////////////////////////////////

  @Override
  public void convert (
    double[] src,
    int srcOffset,
    int[] dest,
    int destOffset,
    int length
  ) {

    for (int i = 0; i < length; i++) {
      dest[destOffset + i] = colorMap[function.applyAsInt (src[srcOffset + i])];
    } // for

  } // convert

  /////////////////////////////////////////////////////////////////

  @Override
  public double[] allocateSrc (int length) { return (new double[length]); }
  
  /////////////////////////////////////////////////////////////////

  @Override
  public int[] allocateDest (int length) { return (new int[length]); }

  /////////////////////////////////////////////////////////////////

} // DoubleToColorConverter class

