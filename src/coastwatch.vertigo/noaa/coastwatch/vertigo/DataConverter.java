/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>DataConverter</code> interface is implemented by classes
 * that convert one type of data into another, typically arrays.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface DataConverter<T, U> {

  /**
   * Converts one type of data into another.
   *
   * @param src the source data.
   * @param srcOffset the offset into the source data to start reading
   * @param dest the destination data.
   * @param destOffset the offset into the destination data to start writing.
   * @param length the length of data to convert.
   */
  void convert (
    T src,
    int srcOffset,
    U dest,
    int destOffset,
    int length
  );

  /**
   * Allocates source data of the specified length.
   *
   * @param length the length of data to allocate.
   *
   * @return the newly allocated data.
   */
  T allocateSrc (int length);
  
  /**
   * Allocates destination data of the specified length.
   *
   * @param length the length of data to allocate.
   *
   * @return the newly allocated data.
   */
  U allocateDest (int length);

} // DataConverter interface

