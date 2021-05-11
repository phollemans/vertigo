/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import javafx.scene.shape.TriangleMesh;

/**
 * The <code>MeshFactory</code> interface is used by classes that create
 * <code>TriangleMesh</code> objects of various levels of detail.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface MeshFactory extends LevelOfDetailFactory {

  /**
   * Creates a mesh object of a given index and level of detail.
   *
   * @param index the index of the object within a group to create.
   * @param level the level of detail in the range [0..levels-1].
   * @param cancelled the method to periodically check for cancellation
   * of the object creation, or null to not check.
   *
   * @return the object or null if the creation was cancelled.
   */
  TriangleMesh create (
    int index,
    int level,
    BooleanSupplier cancelled
  );

  /**
   * Sets up the texture points for a mesh.  Any previous texture points
   * are discarded.
   *
   * @param mesh the mesh to modify.
   * @param index the index of the mesh within the group.
   * @param level the level of detail in the range [0..levels-1].
   * @param textureWidth the width in pixels of the texture that the mesh
   * will be using.
   * @param textureHeight the width in pixels of the texture that the mesh
   * will be using.
   */
  void setTexturePoints (
    TriangleMesh mesh,
    int index,
    int level,
    int textureWidth,
    int textureHeight
  );

  /**
   * Gets an approximate aspect ratio for the mesh.  This can be used
   * when creating textures to help reduce texture sizes when an
   * aspect ratio is large.
   *
   * @param index the index of the object within a group to create.
   *
   * @return the approximate ratio of width to height of the mesh object, or
   * Double.NaN if an aspect ratio could not be computed.
   */
  double getAspectRatio (
    int index
  );

} // MeshFactory interface
