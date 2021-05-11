/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;

/**
 * The <code>ImageTiling</code> class holds data that represents a coverage
 * of an image with a set of rectangular regions.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ImageTiling {

  /** The overall image width. */
  public int width;

  /** The overall image height. */
  public int height;
  
  /** The width of rectangular tiles. */
  public int tileWidth;

  /** The height of rectangular tiles. */
  public int tileHeight;
  
  /** The list of tile in the tiling. */
  private List<ImageTile> tileList;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new tiling.
   *
   * @param width the overall image width.
   * @param height the overall image height.
   * @param tileWidth the width of rectangular tiles.
   * @param tileHeight the height of rectangular tiles.
   */
  public ImageTiling (
    int width,
    int height,
    int tileWidth,
    int tileHeight
  ) {
  
    this.width = width;
    this.height = height;
    this.tileWidth = tileWidth;
    this.tileHeight = tileHeight;
    
    int xTiles = width / tileWidth;
    if (xTiles*tileWidth < width) xTiles++;
    int yTiles = height / tileHeight;
    if (yTiles*tileHeight < height) yTiles++;

    tileList = new ArrayList<>();
    for (int xTile = 0; xTile < xTiles; xTile++) {
      for (int yTile = 0; yTile < yTiles; yTile++) {

        int minX = xTile*tileWidth;
        int minY = yTile*tileHeight;
        int maxX = Math.min (minX + tileWidth - 1, width-1);
        int maxY = Math.min (minY + tileHeight - 1, height-1);

        int thisWidth = maxX - minX + 1;
        int thisHeight = maxY - minY + 1;

        ImageTile tile = new ImageTile (minX, minY, thisWidth, thisHeight);
        tileList.add (tile);

      } // for
    } // for
  
  } // ImageTiling

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the number of tiles in the tiling.
   *
   * @return the tile count.
   */
  public int getTiles() { return (tileList.size()); }
    
  /////////////////////////////////////////////////////////////////

  /**
   * Gets the image tile at the specified index.
   *
   * @param index the tile index in the range [0..tiles-1].
   *
   * @return the tile at the specified index.
   */
  public ImageTile getTile (
    int index
  ) {

    return (tileList.get (index));
  
  } // getTile

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
  
    return ("ImageTiling[width=" + width + ",height=" + height + ",tileWidth=" + tileWidth + ",tileHeight=" + tileHeight + "]");

  } // toString

  /////////////////////////////////////////////////////////////////

} // ImageTiling class
