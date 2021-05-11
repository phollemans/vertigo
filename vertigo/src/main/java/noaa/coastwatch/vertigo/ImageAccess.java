/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ImageAccess</code> class holds information about an access
 * to a rectangular region of image data at a given subsampling interval in
 * each of the X and Y directions in the image.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ImageAccess {

  // Variables
  // ---------

  /** The image tile covering the access region. */
  public ImageTile tile;
  
  /** The subsampling interval in the X direction. */
  public int strideX;
  
  /** The subsampling interval in the Y direction. */
  public int strideY;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new image access.
   *
   * @param tile the tile that covers the access region.
   * @param strideX the subsampling interval in the X direction.
   * @param strideY the subsampling interval in the Y direction.
   */
  public ImageAccess (
    ImageTile tile,
    int strideX,
    int strideY
  ) {

    this.tile = tile;
    this.strideX = strideX;
    this.strideY = strideY;

  } // ImageAccess

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the number of pixels in the X direction of image data specified
   * in the access.
   *
   * @return the access pixel count in X.
   */
  public int getWidth() {

    // Note that an image access with a stride will contain an
    // extra pixel at the end if the tile width is not an even multiple
    // of the stride, ie we would round up the value of tile.width/stride.
    // Rather than doing this:
    // int width = tile.width/strideX;

    int width = (tile.width/strideX) + (tile.width%strideX == 0 ? 0 : 1);
    return (width);

  } // getWidth
  
  /////////////////////////////////////////////////////////////////

  /**
   * Gets the number of pixels in the Y direction of image data specified
   * in the access.
   *
   * @return the access pixel count in Y.
   */
  public int getHeight() {

//    int width = tile.height/strideY;

    int height = (tile.height/strideY) + (tile.height%strideY == 0 ? 0 : 1);
    return (height);

  } // getHeight

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
  
    return ("ImageAccess[tile=" + tile + ",strideX=" + strideX + ",strideY=" + strideY + "]");

  } // toString

  /////////////////////////////////////////////////////////////////
  
} // ImageAccess class


