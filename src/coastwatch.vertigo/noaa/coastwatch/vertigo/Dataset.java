/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.io.IOException;

/**
 * The <code>Dataset</code> class provides coordinates and data values from
 * a source of geographic horizontal 2D data slices.  Each slice has
 * an associated set of image coordinates and data values, and is accessed
 * by specifying a variable name from the dataset, and time/level indices,
 * which may be zero if the variable has no vertical or temporal extents.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface Dataset {

  /**
   * Gets the variable names in this dataset.
   *
   * @return the list of variable names.  The names can be used to retrieve
   * the other information about the variable.
   */
  List<String> getVariables() throws IOException;

  /**
   * Gets the time steps available for a variable in this dataset.
   *
   * @param varName the variable to get time steps.
   *
   * @return the list of times.  The index of each time in the list can be
   * used to retrieve variable data values.  The list may have zero length
   * if there are no time steps.
   */
  List<Date> getTimes (
    String varName
  ) throws IOException;

  /**
   * Gets the vertical levels available for a variable in this dataset.
   *
   * @param varName the variable to get vertical levels.
   *
   * @return the list of level values.  The index of each level in the list
   * can be used to retrieve variable data values.  The list may have zero
   * length if there are no levels.
   */
  List<Double> getLevels (
    String varName
  ) throws IOException;

  /**
   * Gets the 2D (x,y) dimensions for a variable in this dataset.
   *
   * @param varName the variable to get dimensions.
   *
   * @return the dimensions as [width, height].
   */
  int[] getDimensions (
    String varName
  ) throws IOException;

  /**
   * Gets the attributes available for a variable in this dataset.  Attributes
   * are names and values that describe the variable.
   *
   * @param varName the variable to get attributes.
   *
   * @return the map of attribute names to values.
   */
  Map<String, Object> getAttributes (
    String varName
  ) throws IOException;

  /**
   * Gets the image data source for a time and level of a variable in this
   * dataset.
   *
   * @param varName the variable to get the data.
   * @param timeIndex the index from the list of time steps for the 2D data
   * slice or 0 if there are no time steps.
   * @param levelIndex the index from the list of levels for the 2D data
   * slice or 0 if there are no levels.
   *
   * @return the image data source.
   */
  ImageDataSource<double[]> getDataSource (
    String varName,
    int timeIndex,
    int levelIndex
  ) throws IOException;
  
  /**
   * Gets the 2D image coordinate source for a variable in this dataset.
   *
   * @param varName the variable to get the coordinates.
   *
   * @return the image coordinate source.
   */
  ImageCoordinateSource getCoordinateSource (
    String varName
  ) throws IOException;

} // Dataset interface
