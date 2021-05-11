/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import java.util.logging.Logger;
import java.io.IOException;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>TiledImageTextureFactory</code> class creates level of detail
 * texture images using a main image to extract textures from and an image
 * tiling.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class TiledImageTextureFactory implements TextureFactory {

  private static final Logger LOGGER = Logger.getLogger (TiledImageTextureFactory.class.getName());

  // Variables
  // ---------

  /** The image tiling to use for individual tiles. */
  private ImageTiling tiling;
  
  /** The tile writer used to transfer data into texture images. */
  private ImageTileWriter tileWriter;

  /** The minimum camera distance for each level of texture detail. */
  private double[] dmin;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new texture factory.
   *
   * @param tiling the image tiling to use for individual tiles.  The tile
   * indices within the tiling should be passed as the index values in the
   * {@link #create} method.
   * @param tileWriter the tile writer to used for transferring data into
   * texture images from the main image.
   * @param dmin the array of minimum camera distance values for each level
   * of texture detail for use in the {@link #getLevelForDist} method.  The
   * level will be selected for a given distance such that dmin[level] is
   * less than the camera distance and dmin[level+1] is greater than the camera
   * distance, as much as possible (until the distance becomes too small and
   * no greater texture with a higher level of detail is available).
   */
  public TiledImageTextureFactory (
    ImageTiling tiling,
    ImageTileWriter tileWriter,
    double[] dmin
  ) {

    this.tiling = tiling;
    this.tileWriter = tileWriter;
    this.dmin = dmin;

  } // TiledImageTextureFactory

  /////////////////////////////////////////////////////////////////

  @Override
  public int getLevels() { return (dmin.length); }

  /////////////////////////////////////////////////////////////////

  @Override
  public Image create (
    int index,
    double aspect,
    int level,
    BooleanSupplier cancelled
  ) {

    ImageTile tile = tiling.getTile (index);
    int stride = 1 << level;

    // Apply the aspect ratio as follows.  If according to the tiling, a
    // texture should be 256 x 256 but has an aspect ratio of 1:2
    // (width:height), we would save some time creating it by making it
    // 128 x 256 -- ie: we reduce the number of pixels in the dimension
    // that is smaller in aspect ratio by a factor dictated by the ratio.
    int strideX, strideY;
    if (!Double.isNaN (aspect)) {
      double tileAspect = (double) tile.width / tile.height;
      aspect = aspect / tileAspect;
      if (aspect < 1) {
        strideX = (int) Math.min (stride/aspect, tile.width);
        strideY = stride;
      } // if
      else {
        strideX = stride;
        strideY = (int) Math.min (stride*aspect, tile.height);
      } // else
    } // if
    else {
      strideX = strideY = stride;
    } // else

    // Retrieve the texture image and place it into a slightly larger image
    // to get ready for the padding step performed next.
    int imageWidth = tile.width/strideX;
    int imageHeight = tile.height/strideY;
    int paddedImageWidth = imageWidth + 2;
    int paddedImageHeight = imageHeight + 2;
    WritableImage image = new WritableImage (paddedImageWidth, paddedImageHeight);

    try { tileWriter.write (tile, image, 1, 1, imageWidth, imageHeight, cancelled); }
    catch (IOException e) { throw new RuntimeException (e); }
    if (isTrue (cancelled)) image = null;

    // What we do here is pad the texture image with a border of pixels
    // because when the texture is placed into a mesh, the graphics card
    // wraps the image coordinates around to the other side when it encounters
    // 0 or 1 in the texture coordinates (pixel interpolation).  So padding
    // the image with duplicates and setting the texture coordinates to the
    // edges of the actual texture rectangle avoids the opposite edges of the
    // texture from being incorporated into the interpolation.
    if (image != null) {
    
      PixelReader pixelReader = image.getPixelReader();
      PixelWriter pixelWriter = image.getPixelWriter();
      
      // Top row
      int srcy = 1;
      int dsty = srcy - 1;
      int srcx = 1;
      int dstx = 1;
      pixelWriter.setPixels (dstx, dsty, imageWidth, 1, pixelReader, srcx, srcy);

      // Top left and right pixels
      pixelWriter.setPixels (0, dsty, 1, 1, pixelReader, 1, srcy);
      pixelWriter.setPixels (paddedImageWidth-1, dsty, 1, 1, pixelReader, paddedImageWidth-2, srcy);

      // Bottom row
      srcy = paddedImageHeight-2;
      dsty = srcy+1;
      pixelWriter.setPixels (dstx, dsty, imageWidth, 1, pixelReader, srcx, srcy);

      // Bottom left and right pixels
      pixelWriter.setPixels (0, dsty, 1, 1, pixelReader, 1, srcy);
      pixelWriter.setPixels (paddedImageWidth-1, dsty, 1, 1, pixelReader, paddedImageWidth-2, srcy);

      // Left edge
      srcx = 1;
      dstx = srcx - 1;
      srcy = 1;
      dsty = 1;
      pixelWriter.setPixels (dstx, dsty, 1, imageHeight, pixelReader, srcx, srcy);
      
      // Right edge
      srcx = paddedImageWidth - 2;
      dstx = srcx + 1;
      pixelWriter.setPixels (dstx, dsty, 1, imageHeight, pixelReader, srcx, srcy);

    } // if

    return (image);
  
  } // create

  /////////////////////////////////////////////////////////////////

  @Override
  public int getLevelForDist (
    double dist
  ) {
  
    int level;
    if (dmin.length == 0) level = 0;
    else {
      level = dmin.length-1;
      while (dist < dmin[level] && level > 0) level--;
    } // else

    return (level);

  } // getLevelForDist

  /////////////////////////////////////////////////////////////////

} // TiledImageTextureFactory class
