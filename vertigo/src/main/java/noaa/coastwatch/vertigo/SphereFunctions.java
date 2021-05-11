/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.geometry.Point3D;

/**
 * The <code>SphereFunctions</code> class provides various methods for working
 * with data coordinates on a sphere.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class SphereFunctions {

  // Constants
  // ---------

  public static final int THETA = 0;
  public static final int PHI = 1;
  
  private static final int X = 0;
  private static final int Y = 1;
  private static final int Z = 2;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Converts the specified 3D point into the equivalent spherical coordinates.
   *
   * @param x the x coordinate in model space.
   * @param y the x coordinate in model space.
   * @param z the x coordinate in model space.
   * @param sphere the output spherical coordinates of the point as
   * [theta, phi] in radians.  Theta is the angle down from the vertical axis,
   * and phi is the angle around the vertical axis, starting from the positive
   * x axis towards the y axis (right-handed system).
   */
  public static void pointToSphere (
    double x,
    double y,
    double z,
    double[] sphere
  ) {

    // First the coordinates are rearranged to be in the cartesian
    // system where theta and phi exist rather than graphics model
    // space
    double cartX = -z;
    double cartY = x;
    double cartZ = -y;
    x = cartX;
    y = cartY;
    z = cartZ;

    // Then we perform the actual conversion to theta and phi
    double phi = Math.atan2 (y, x);
    double arg = z / Math.sqrt (x*x + y*y + z*z);
    if (arg > 1) arg = 1;
    double theta = Math.acos (arg);
    
    sphere[THETA] = theta;
    sphere[PHI] = phi;
  
  } // pointToSphere

  /////////////////////////////////////////////////////////////////

  /**
   * Converts the specified 3D point into the equivalent spherical coordinates.
   *
   * @param point the 3D point to convert.
   * @param sphere the output spherical coordinates of the point as
   * [theta, phi] in radians.  Theta is the angle down from the vertical axis,
   * and phi is the angle around the vertical axis, starting from the positive
   * x axis towards the y axis (right-handed system).
   */
  public static void pointToSphere (
    Point3D point,
    double[] sphere
  ) {

    pointToSphere (point.getX(), point.getY(), point.getZ(), sphere);
      
  } // pointToSphere

  /////////////////////////////////////////////////////////////////

  /**
   * Converts the specified spherical coordinates to a 3D point.
   *
   * @param theta the theta angle in radians. Theta is the angle down from
   * the vertical axis.
   * @param phi the phi angle in radians.  Phi is the angle around the
   * vertical axis, starting from the positive x axis towards the y
   * axis (right-handed system).
   * @param radius the radius of the sphere.
   * @param point the output model point coordinates as [x,y,z].
   */
  public static void sphereToPoint (
    double theta,
    double phi,
    double radius,
    double[] point
  ) {

    point[Z] = - radius * Math.sin (theta) * Math.cos (phi);
    point[X] = radius * Math.sin (theta) * Math.sin (phi);
    point[Y] = - radius * Math.cos (theta);
  
  } // pointToSphere

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the distance from the surface of a sphere to the midpoint
   * between two locations on the sphere.  This is useful for determining
   * the error when a straight line segment is used to approximate
   * a curved surface.
   *
   * @param a the first point on the sphere.
   * @param b the second point on the sphere.
   * @param radius the readius of the sphere.
   *
   * @return the maximum distance between the curved surface
   * and the line joining the surface points.
   */
  public static double delta (
    double[] a,
    double[] b,
    double radius
  ) {
  
    // Compute the dot product between the two vectors pointing from
    // the center of the sphere to the surface locations.
    double dot = 0;
    for (int i = 0; i < 3; i++) dot += (a[i]*b[i]);

    // Compute the angle between the two vectors.
    double cosTheta = dot/(radius*radius);
    double theta = Math.acos (cosTheta);

    // Finally, compute delta, the maximum distance between the curved surface
    // and the line joining the surface points.
    double delta = radius * (1 - Math.cos (theta/2));

    return (delta);

  } // delta

  /////////////////////////////////////////////////////////////////

  /**
   * Formats an angle to DDDºMM'SS.SS" notation.
   *
   * @param deg the angle to format in degrees.
   *
   * @return the formatted angle.
   *
   * @since 0.6
   */
  public static String degMinSec (
    double deg
  ) {

    deg = Math.abs (deg);
    int dd = (int) deg;
    int mm = (int) ((deg - dd)*60);
    double ss = (deg - dd - mm/60.0)*3600;
    return (String.format ("%3dº%02d'%05.2f\"", dd, mm, ss));

  } // degMinSec
  
  /////////////////////////////////////////////////////////////////

} // SphereFunctions class
