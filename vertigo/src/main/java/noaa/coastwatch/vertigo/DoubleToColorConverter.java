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

  // Variables
  // ---------
  
  /** The colormap that contains the colors to use for image pixels. */
  private int[] colorMap;

  /** The function that maps double values to integer indices in the colormap. */
  private DoubleToIntFunction function;

  /** The domain bounds of the function. */
  private double min, max;

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the minimum data value in the mapping function domain.
   *
   * @return the minimum value.
   *
   * @since 0.6
   */
  public double getMin() { return (min); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the maximum data value in the mapping function domain.
   *
   * @return the maximum value.
   *
   * @since 0.6
   */
  public double getMax() { return (max); }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a linear function color converter.
   *
   * @param min the minimum data value to convert.
   * @param max the maximum data value to convert.
   * @param map the colors for mapping.  The color at map[0] is used for missing
   * data, and map[1..colors-1] as the valid values.
   *
   * @return the function instance.
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
  
    var converter = new DoubleToColorConverter (min, max, func, map);
    return (converter);

  } // linearInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a logarithmic function color converter.
   *
   * @param min the minimum data value to convert.
   * @param max the maximum data value to convert.
   * @param map the colors for mapping.  The color at map[0] is used for missing
   * data, and map[1..colors-1] as the valid values.
   *
   * @return the function instance.
   */
  public static DoubleToColorConverter logInstance (
    double min,
    double max,
    int[] map
  ) {
  
    int colors = map.length-1;
    double slope = 1.0 / (Math.log10 (max) - Math.log10 (min));
    double inter = -slope * Math.log10 (min);
    DoubleToIntFunction func = value -> {
      int intValue;
      if (Double.isNaN (value)) {
        intValue = 0;
      } // if
      else {
        double norm = slope*Math.log10 (value) + inter;
        if (norm < 0) norm = 0;
        else if (norm > 1) norm = 1;
        intValue = (int) Math.round (norm * (colors-1)) + 1;
      } // else
      return (intValue);
    };

    var converter = new DoubleToColorConverter (min, max, func, map);
    return (converter);

  } // logInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new converter.
   *
   * @param min the function domain minimum.
   * @param max the function domain maximum.
   * @param function the function that maps double values to integer indices in the colormap.
   * @param colorMap the colormap that contains the colors to use for image
   * pixels as ARGB values.
   */
  public DoubleToColorConverter (
    double min,
    double max,
    DoubleToIntFunction function,
    int[] colorMap
  ) {
  
    this.min = min;
    this.max = max;
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

