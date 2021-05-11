/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * An <code>ImageTile</code> represents a rectangular region in an image.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ImageTile {

  /** The minimum X coordinate. */
  public int minX;

  /** The minimum X coordinate. */
  public int minY;
  
  /** The rectangular region width. */
  public int width;

  /** The rectangular region height. */
  public int height;
  
  /////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new tile object.
   *
   * @param minX the minimum X coordinate.
   * @param minY the minimum X coordinate.
   * @param width the rectangular region width.
   * @param height the rectangular region height.
   */
  public ImageTile (
    int minX,
    int minY,
    int width,
    int height
  ) {

    this.minX = minX;
    this.minY = minY;
    this.width = width;
    this.height = height;

  } // ImageTile

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
  
    return ("ImageTile[minX=" + minX + ",minY=" + minY + ",width=" + width + ",height=" + height + "]");

  } // toString

  /////////////////////////////////////////////////////////////////

} // ImageTile class
