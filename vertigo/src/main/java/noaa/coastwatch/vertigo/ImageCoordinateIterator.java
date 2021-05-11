/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ImageCoordinateIterator</code> interface is for classes that
 * loop through a series of image coordinates in an iterative fashion.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ImageCoordinateIterator {

  /**
   * Determines if there is another coordinate in the series.
   *
   * @return true if another coordinate exists in the series or false if not.
   */
  boolean hasNext();
  
  /** Updates the iterator state to the next coordinate in the series. */
  void next();
  
  /**
   * Accesses the X value of the current coordinate.
   *
   * @return the current X value.
   */
  int getX();

  /**
   * Accesses the Y value of the current coordinate.
   *
   * @return the current Y value.
   */
  int getY();
  
  /**
   * Resets the iterator to the initial state.  Calls to {@link #hasNext} and
   * {@link #next} will behave as if the iterator was just created.
   */
  void reset();
  
} // ImageCoordinateIterator interface

