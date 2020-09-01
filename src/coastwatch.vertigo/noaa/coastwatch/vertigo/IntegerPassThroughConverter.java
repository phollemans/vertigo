/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>IntegerPassThroughConverter</code> class passes integer data
 * through without conversion.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class IntegerPassThroughConverter implements DataConverter<int[], int[]> {

  /////////////////////////////////////////////////////////////////

  @Override
  public void convert (
    int[] src,
    int srcOffset,
    int[] dest,
    int destOffset,
    int length
  ) {

    for (int i = 0; i < length; i++) dest[destOffset + i] = src[srcOffset + i];

  } // convert

  /////////////////////////////////////////////////////////////////

  @Override
  public int[] allocateSrc (int length) { return (new int[length]); }
  
  /////////////////////////////////////////////////////////////////

  @Override
  public int[] allocateDest (int length) { return (new int[length]); }

  /////////////////////////////////////////////////////////////////

} // IntegerPassThroughConverter class


