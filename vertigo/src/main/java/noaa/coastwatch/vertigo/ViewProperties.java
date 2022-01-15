/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ViewProperties</code> class holds various properties related to
 * the model view and provides calculations on the properties to convert
 * between model space and pixel space.  The calculations are based on the
 * equations relating pixel space shift to model space shift:
 * <pre>
 *   dPixel = cFact * dModel
 *   cFact = vres / 2*d*tan(phi/2)
 * </pre>
 * where the conversion factor cFact converts distances between the spaces.
 * Model space is the 3D (x,y,z) space in which the vertices of the view model
 * are based.  Pixel space is the (x,y) of the image viewed by the camera
 * on the screen.  The value d is the distance from the camera to the subject
 * being viewed, and phi is the vertical field of view angle.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ViewProperties {

  /** The view resolution in the vertical direction in pixels. */
  public int vres;
  
  /** The maximum desired pixel shift for model inaccuracies. */
  public int tau;
  
  /** Tan (phi/2) where phi is the vertical field of view angle. */
  public double tan_phi_o_2;
  
  /** The camera minimum distance in model space. */
  public double cmin;
  
  /** The camera maximum distance in model space. */
  public double cmax;
  
  /////////////////////////////////////////////////////////////////
    
  /**
   * Computes the camera distance for a given model-to-pixel space ratio.
   *
   * @param dModel the distance in model space perpendicular to the camera
   * view direction.
   * @param dPixel the distance in pixel space on the screen.
   *
   * @return the distance from the camera to the subject in model space that
   * matches the model-to-pixel shift ratio computed from the parameters.
   */
  public double cdist (
    double dModel,
    double dPixel
  ) {
  
    double d = (vres * dModel) / (2*dPixel*tan_phi_o_2);
    return (d);
    
  } // cdist

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the minimum camera distance for a given model shift.
   *
   * @param dModel the distance in model space perpendicular to the camera
   * view direction.
   *
   * @return the minimum distance from the camera to the subject in model
   * space that matches the model-to-pixel shift ratio computed using the
   * specified model distance and the tau property.  Any camera distance
   * greater than the minimum returned will result in a lesser pixel shift
   * than tau in screen space.
   */
  public double cameraDistMin (
    double dModel
  ) {
  
    return (cdist (dModel, tau));
  
  } // cameraDistMin

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the model space distance for a given camera distance and
   * pixel space distance.
   *
   * @param cdist the camera distance in model space units.
   * @param dPixel the distance in pixel space units.
   *
   * @return the model distance.
   */
  private double dModel (
    double cdist,
    double dPixel
  ) {
  
    double delta = (cdist * 2*dPixel*tan_phi_o_2)/vres;
    return (delta);
    
  } // dModel

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the maximum desired shift in model space units at the
   * minimum camera viewing distance (ie: highest resolution of the model needed).
   *
   * @return the maximum model space shift.
   */
  public double deltaMaxHigh() { return (dModel (cmin, tau)); }

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the maximum desired shift in model space units at the
   * maximum camera viewing distance (ie: lowest resolution of the model needed).
   *
   * @return the maximum model space shift.
   */
  public double deltaMaxLow() { return (dModel (cmax, tau)); }

  /////////////////////////////////////////////////////////////////

} // ViewProperties class
