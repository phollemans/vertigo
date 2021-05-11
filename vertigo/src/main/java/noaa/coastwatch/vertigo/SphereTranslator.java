/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import static noaa.coastwatch.vertigo.SphereFunctions.THETA;
import static noaa.coastwatch.vertigo.SphereFunctions.PHI;

/**
 * The <code>SphereTranslator</code> class translates (lat,lon) coordinates
 * to their equivalent (x,y,z) coordinates on the surface of a
 * sphere of a given radius.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class SphereTranslator implements GeoCoordinateTranslator {

  // Variables
  // ---------
  
  /** The radius of the sphere for coordinate translation. */
  private double radius;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new sphere translator.
   *
   * @param radius the radius of the sphere to translate from (lat,lon) to
   * model (x,y,z).
   */
  public SphereTranslator (
    double radius
  ) {
  
    this.radius = radius;
    
  } // SphereTranslator

  /////////////////////////////////////////////////////////////////

  @Override
  public void translate (
    double lat,
    double lon,
    double[] model
  ) {
  
    double theta = Math.toRadians (90 - lat);
    double phi = Math.toRadians (lon + 180);

    SphereFunctions.sphereToPoint (theta, phi, radius, model);

  } // translate

  /////////////////////////////////////////////////////////////////

  @Override
  public void translate (
    double x,
    double y,
    double z,
    double[] latLon
  ) {

    double[] sphere = latLon;
    SphereFunctions.pointToSphere (x, y, z, sphere);
    double lat = 90 -  Math.toDegrees (sphere[THETA]);
    double lon = Math.toDegrees (sphere[PHI]) - 180;
    while (lon < -180) lon += 360;
    while (lon > 180) lon -= 360;
    latLon[0] = lat;
    latLon[1] = lon;
    
  } // translate

  /////////////////////////////////////////////////////////////////

} // SphereTranslator
