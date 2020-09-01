/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;
import java.util.function.BooleanSupplier;

/**
 * The <code>ImageDataSource</code> interface is used by classes that produce
 * data of some type for each (x,y) position in image coordinates.
 * The source is used in a bulk access model as follows:
 * <ul>
 *   <li>Perform a call to the source {@link #access} method to retrieve
 *   a specific rectangle and stride of data.  This produces a result object
 *   for the data access.</li>
 *   <li>Perform a series of calls to {@link #get} or {@link #getMany}
 *   to retrieve data from the result.</li>
 * </ul>
 * Note that the image coordinates supplied in calls to {@link #get} or
 * {@link getMany} are relative to the access result, not the overall
 * image data.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ImageDataSource<T> {

  /**
   * Signals the source to retrieve data within the specified access region
   * and subsampling interval.
   *
   * @param access the data access region.
   * @param cancelled the method to periodically check for cancellation
   * of the access, or null to not check.
   *
   * @return the result object to use in calls to {@link #get} and
   * {@link #getMany} for retrieving data within the access region, or null
   * if the access was cancelled.
   *
   * @throws IOException if the access failed.
   */
  ImageAccessResult access (
    ImageAccess access,
    BooleanSupplier cancelled
  ) throws IOException;

  /**
   * Retrieves the data at the specified image coordinates.
   *
   * @param result the result object from a call to {@link #access}.
   * @param x the x coordinate relative to the image access.
   * @param y the y coordinate relative to the image access.
   * @param data the output data to fill with a value.
   */
  void get (
    ImageAccessResult result,
    int x,
    int y,
    T data
  );

  /**
   * Retrieves the data at a series of image coordinates.
   *
   * @param result the result object from a call to {@link #access}.
   * @param iter the iterator for (x,y) coordinates relative to the image access.
   * @param data the output data to fill with values, must be large
   * enough to hold data for all image coordinates returned by the
   * iterator.
   */
  void getMany (
    ImageAccessResult result,
    ImageCoordinateIterator iter,
    T data
  );

  /**
   * Retrieves all the data in an access result (default not implemented).
   *
   * @param result the result object from a call to {@link #access}.
   * @param data the output data to fill with values, must be large
   * enough to hold data for all data values in the result.  Values are
   * filled in row major order (ie: all values along a given y, then the next
   * y value, etc).
   */
  default void getAll (
    ImageAccessResult result,
    T data
  ) {
  
    throw new UnsupportedOperationException();
  
  } // getAll

} // ImageDataSource interface

