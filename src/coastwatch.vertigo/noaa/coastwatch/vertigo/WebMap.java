/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Date;

/**
 * The <code>WebMap</code> class provides coordinates and image tiles from
 * a tiled web map.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface WebMap {

  /**
   * Gets the time steps available for the web map.
   *
   * @return the list of times, possibly zero length.  The index of each time
   * in the list can be used to retrieve a data source.
   */
  List<Date> getTimes();

  /**
   * Gets the 2D (x,y) dimensions for the web map.
   *
   * @return the dimensions as [width, height].
   */
  int[] getDimensions();

  /**
   * Gets the image data source for a time index in this web map.
   *
   * @param timeIndex the index from the list of time steps for the web map.
   * The index is ignored if the web map has no time steps.
   *
   * @return the image data source.
   */
  ImageDataSource<int[]> getDataSource (
    int timeIndex
  );
  
  /**
   * Gets the 2D image coordinate source for this web map.
   *
   * @return the image coordinate source.
   */
  ImageCoordinateSource getCoordinateSource();

} // WebMap class
