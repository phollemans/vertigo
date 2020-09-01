/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

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

} // SphereTranslator
