
/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.ToDoubleBiFunction;

/**
 * The <code>GeoSurfaceViewContext</code> class holds data about a geographic
 * dynamic surface and the view properties that it has.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class GeoSurfaceViewContext {

  /** The translator used to transform the surface geographic to model coordinates. */
  public GeoCoordinateTranslator coordTrans;

  /**
   * The function that computes the surface offset in model space resulting
   * from a straight line between two model (x,y,z) points.  This is specific
   * to a surface geometry.
   */
  public ToDoubleBiFunction<double[], double[]> deltaFunc;

  /** The properties of the view that the surface is part of. */
  public ViewProperties viewProps;
  
} // GeoSurfaceViewContext class

