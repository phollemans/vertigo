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

  /**
   * Translates a 3D model coordinate in (x,y,z) to a geographic coordinate
   * in (lat,lon).  We assume that the model coordinate lies on the surface.
   *
   * @param x the model x coordinate.
   * @param y the model y coordinate.
   * @param z the model z coordinate.
   * @param latLon the geographic (lat,lon) coordinates in degrees in the
   * range [-90..90] and [-180..180] respectively (modified).
   *
   * @since 0.6
   */
  void translate (
    double x,
    double y,
    double z,
    double[] latLon
  );

} // GeoCoordinateTranslator interface

