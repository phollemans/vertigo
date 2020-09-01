/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import javafx.scene.image.Image;

/**
 * The <code>TextureFactory</code> interface is used by classes that create
 * <code>Image</code> texture objects at various resolution levels.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface TextureFactory extends LevelOfDetailFactory {

  /**
   * Creates a mesh object of a given index and level of detail.
   *
   * @param index the index of the object within a group to create.
   * @param aspect the approximate aspect ratio width:height that the
   * texture will appear on-screen, or Double.NaN if unknown.
   * @param level the level of detail in the range [0..levels-1].
   * @param cancelled the method to periodically check for cancellation
   * of the object creation, or null to not check.
   *
   * @return the object or null if the creation was cancelled.
   */
  Image create (
    int index,
    double aspect,
    int level,
    BooleanSupplier cancelled
  );

} // TextureFactory interface

