/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;
import java.nio.IntBuffer;
import java.util.function.DoubleToIntFunction;
import java.util.function.BooleanSupplier;

import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelFormat;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>ComponentTileWriter</code> class writes data to tiles that
 * originate from three primitive double data sources, one each for the red,
 * green, and blue components of the output pixels.  Three functions
 * are used to convert double values to byte values.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ComponentTileWriter implements ImageTileWriter {

  // Variables
  // ---------

  /** The array of three data sources for red, green, blue. */
  private ImageDataSource<double[]>[] sourceArray;

  /** The array of three functions to use for converting double to byte. */
  private DoubleToIntFunction[] functionArray;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new component tile writer.
   *
   * @param sourceArray the array of three data sources for red, green, and
   * blue color components.
   * @param functionArray the array of three functions to use for converting
   * double values in the source data to byte values in the RGB pixel values.
   */
  public ComponentTileWriter (
    ImageDataSource<double[]>[] sourceArray,
    DoubleToIntFunction[] functionArray
  ) {
  
    this.sourceArray = sourceArray;
    this.functionArray = functionArray;
  
  } // ComponentTileWriter

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

    // TODO: If we ever want to use this, we need to make it more like the
    // code in ColorTileWriter.
    
//    int imageWidth = (int) image.getWidth();
//    int imageHeight = (int) image.getHeight();
//    int strideX = tile.width / imageWidth;
//    int strideY = tile.height / imageHeight;
//    ImageAccess access = new ImageAccess (tile, strideX, strideY);
//
//    ImageAccessResult[] resultArray = new ImageAccessResult[3];
//    for (int component = 0; component < 3; component++) {
//      resultArray[component] = sourceArray[component].access (access, cancelled);
//      if (isTrue (cancelled)) break;
//    } // for
//
//    if (!isTrue (cancelled)) {
//
//      double[] rowData = new double[imageWidth];
//      int[] colorData = new int[imageWidth];
//
//      PixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
//      PixelWriter writer = image.getPixelWriter();
//
//      RowIterator iter = new RowIterator (imageWidth);
//      for (int y = 0; y < imageHeight; y++) {
//
//        for (int x = 0; x < imageWidth; x++) colorData[x] = 0xff000000;
//
//        for (int component = 0; component < 3; component++) {
//
//          iter.setRow (y);
//          sourceArray[component].getMany (resultArray[component], iter, rowData);
//
//          for (int x = 0; x < imageWidth; x++) {
//            if (Double.isNaN (rowData[x]))
//              colorData[x] = 0x00000000;
//            else {
//              int byteValue = functionArray[component].applyAsInt (rowData[x]);
//              colorData[x] = colorData[x] | (byteValue << (2-component)*8);
//            } // else
//          } // for
//
//        } // for
//
//        if (isTrue (cancelled)) break;
//
//        writer.setPixels (0, y, imageWidth, 1, pixelFormat, colorData, 0, imageWidth);
//
//      } // for
//
//    } // if

  } // write

  /////////////////////////////////////////////////////////////////

} // ComponentTileWriter class


