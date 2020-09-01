/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ImageCoordinateSource</code> interface is used by classes
 * that produce 3D model (x,y,z) coordinates from a set of 2D image (x,y)
 * coordinates.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ImageCoordinateSource extends ImageDataSource<double[]> {

  default void translate (ImageAccessResult result, int x, int y, double[] model) {
    get (result, x, y, model);
  } // translate

  default void translateMany (ImageAccessResult result, ImageCoordinateIterator iter, double[] model) {
    getMany (result, iter, model);
  } // translateMany

} // ImageCoordinateSource interface
