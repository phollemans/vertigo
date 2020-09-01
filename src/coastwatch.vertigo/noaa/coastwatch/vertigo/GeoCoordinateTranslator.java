/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>GeoCoordinateTranslator</code> interface is used by classes
 * that produce a model (x,y,z) coordinate from geographic (lat,lon)
 * coordinate.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface GeoCoordinateTranslator {

  /**
   * Translates a geographic coordinate in (lat,lon) to a 3D model coordinate
   * in (x,y,z).
   *
   * @param lat the latitude coordinate in degrees in the range [-90..90].
   * @param lon the longitude coordinate in degrees in the range [-180..180].
   * @param model the model (x,y,z) coordinate (modified).
   */
  void translate (
    double lat,
    double lon,
    double[] model
  );

} // GeoCoordinateTranslator interface

