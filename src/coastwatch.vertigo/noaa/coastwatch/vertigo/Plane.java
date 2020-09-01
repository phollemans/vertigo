/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.geometry.Point3D;

/**
 * The <code>Plane</code> class holds data values that represent a plane in
 * 3D space.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Plane {

  /** The vector pointing in the direction of the plane normal. */
  public Point3D norm;
  
  /** The distance value d such that p.norm = d for all points p in the plane. */
  public double d;
  
} // Plane class
