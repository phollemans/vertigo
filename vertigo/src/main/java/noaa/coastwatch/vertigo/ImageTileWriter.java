/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.scene.image.WritableImage;
import java.util.function.BooleanSupplier;
import java.io.IOException;

/**
 * The <code>ImageTileWriter</code> interface is implemented by classes
 * that provide image data in rectangular regions from a data source.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ImageTileWriter {

  /**
   * Writes a tile of image data to the writable image.
   *
   * @param tile the rectangular region represented by the image.
   * @param image the image to write image data (modified).
   * @param startX the starting x location in the image to write data to.
   * @param startY the starting y location in the image to write data to.
   * @param width the width of the image to write.
   * @param height the height of the image to write.
   * @param cancelled the method to check regularly to discover if the
   * write operation has been cancelled, or null to not check.
   *
   * @throws IOException if an error occurred writing the image.
   */
  void write (
    ImageTile tile,
    WritableImage image,
    int startX,
    int startY,
    int width,
    int height,
    BooleanSupplier cancelled
  ) throws IOException;

} // ImageTileWriter interface
