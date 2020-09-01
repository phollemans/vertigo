/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.function.BooleanSupplier;

import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>ColorTileWriter</code> class writes tiles using data that
 * originates from an <code>ImageDataSource</code> object and a converter
 * from data values to colors.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ColorTileWriter<T> implements ImageTileWriter {

  // Variables
  // ---------
  
  /** The source of data. */
  private ImageDataSource<T> source;

  /** The converter from data values to ARGB color integers. */
  private DataConverter<T, int[]> converter;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new colormap tile writer.
   *
   * @param source the source of data.
   * @param converter the converter from data values to ARGB color integers.
   */
  public ColorTileWriter (
    ImageDataSource<T> source,
    DataConverter<T, int[]> converter
  ) {
  
    this.source = source;
    this.converter = converter;
  
  } // ColorTileWriter

  /////////////////////////////////////////////////////////////////

  @Override
  public void write (
    ImageTile tile,
    WritableImage image,
    int startX,
    int startY,
    int width,
    int height,
    BooleanSupplier cancelled
  ) throws IOException {

    // The requested image here has a certain size that is less than or
    // equal to the tile we've been given to access.  We need to map data
    // pixels from the source into the image.  We do that by first detecting
    // what resolution (ie: stride) from the data we're going to need, then
    // perform the data access.
    int strideX = tile.width / width;
    int strideY = tile.height / height;
    ImageAccess access = new ImageAccess (tile, strideX, strideY);
    ImageAccessResult result = source.access (access, cancelled);

    if (!isTrue (cancelled)) {

      // Now that the data access is done, we have to convert over the data
      // pixels into an image.  We know that pixel (0,0) is going to carry
      // over into the image.  But the data access width x height may be slightly
      // larger than the image width x height, depending on how the stride
      // parameter matched up with the data access bounds. So we need to index
      // the data and image pixel arrays separately in that case.  In the
      // majority of cases, the image size and data size are equal, so we
      // handle that separately to help increase performance.

      int imagePixels = width*height;
      int[] colorData = new int[imagePixels];

      int dataWidth = access.getWidth();
      int dataHeight = access.getHeight();
      int dataPixels = dataWidth * dataHeight;
      T imageData = converter.allocateSrc (dataPixels);

      PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
      PixelWriter writer = image.getPixelWriter();

      source.getAll (result, imageData);

      // This is the easy case, when the data dimensions are the same as the
      // image dimensions.
      if (width == dataWidth && height == dataHeight) {
        converter.convert (imageData, 0, colorData, 0, imagePixels);
      } // if
      
      // This is the slightly more complicated case.  Since the data dimensions
      // and the image dimensions are not equal, we have to convert the data
      // to colors row by row, taking into acount that the offsets into the
      // data arrays are not the same for the start of each row.
      else {
        for (int y = 0; y < height; y++) {
          int dataOffset = y*dataWidth;
          int imageOffset = y*width;
          converter.convert (imageData, dataOffset, colorData, imageOffset, width);
        } // for
      } // else

      // In either case, the pixels are transferred into the image in a
      // large block.
      writer.setPixels (startX, startY, width, height, pixelFormat, colorData, 0, width);

    } // if

  } // write

  /////////////////////////////////////////////////////////////////

} // ColorTileWriter class


