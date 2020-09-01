/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.geometry.Point3D;
import javafx.geometry.Bounds;
import javafx.scene.transform.Affine;

/**
 * The <code>Frustum</code> class holds data describing a 3D perspective
 * viewing frustum as a set of 6 enclosing planes.  It is used to detect
 * object boundary intersections with the view.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Frustum {

  // Constants
  // ---------
  
  private static final int NEAR = 0;
  private static final int FAR = 1;
  private static final int LEFT = 2;
  private static final int RIGHT = 3;
  private static final int TOP = 4;
  private static final int BOTTOM = 5;

  // Variables
  // ---------

  /** The set of planes bounding the view frustum. */
  private Plane[] planes;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new perspective view frustum.
   *
   * @param cameraOrient the affine transform of the camera that transforms
   * camera node orientation vectors to parent orientation vectors.
   * @param cameraPos the position of the camera in the camera node frame.
   * @param fov the vertical field of view of the camera in degrees.
   * @param nearClip the near clipping plane as the Z distance from the
   * camera eye (camera facing towards positive Z in its reference frame).
   * @param farClip the far clipping plane as the Z distance from the
   * camera eye (camera facing towards positive Z in its reference frame).
   * @param aspect the aspect ratio of the view as width/height.
   */
  public Frustum (
    Affine cameraOrient,
    Point3D cameraPos,
    double fov,
    double nearClip,
    double farClip,
    double aspect
  ) {
  
    planes = new Plane[6];
    
    double phi_v = Math.toRadians (fov)/2;
    double phi_h = Math.atan (aspect*Math.tan (phi_v));
    double cos_phi_v = Math.cos (phi_v);
    double sin_phi_v = Math.sin (phi_v);
    double cos_phi_h = Math.cos (phi_h);
    double sin_phi_h = Math.sin (phi_h);

    // We compute the planes so that all normals are pointed outward
    // from the frustum.  That way, testing points against the planes
    // will yield a positive value if the point is outside.
    for (int i = 0; i < 6; i++) {
      planes[i] = new Plane();
      Point3D planeDir = null;
      double zDist = 0;
      switch (i) {
      case NEAR:
        planeDir = new Point3D (0, 0, -1);
        zDist = nearClip;
        break;
      case FAR:
        planeDir = new Point3D (0, 0, 1);
        zDist = farClip;
        break;
      case LEFT:
        planeDir = new Point3D (-cos_phi_h, 0, -sin_phi_h);
        break;
      case RIGHT:
        planeDir = new Point3D (cos_phi_h, 0, -sin_phi_h);
        break;
      case TOP:
        planeDir = new Point3D (0, -cos_phi_v, -sin_phi_v);
        break;
      case BOTTOM:
        planeDir = new Point3D (0, cos_phi_v, -sin_phi_v);
        break;
      } // switch
      planes[i].norm = cameraOrient.transform (planeDir);
      planes[i].d = planes[i].norm.dotProduct (cameraOrient.transform (cameraPos.add (0, 0, zDist)));
    } // for
    
  } // Frustum

  /////////////////////////////////////////////////////////////////

  /**
   * Determines if the bounding box intersects this frustrum.
   *
   * @param bounds the bounds to check.
   *
   * @return true if the bounds intersect or false if not.
   */
  public boolean intersects (
    Bounds bounds
  ) {

    double minX, minY, minZ;
    boolean answer = true;

    for (int i = 0; i < 6; i++) {

      // Find minimum corner in bound box with respect to plane normal
      if (planes[i].norm.getX() >= 0) minX = bounds.getMinX();
      else minX = bounds.getMaxX();
    
      if (planes[i].norm.getY() >= 0) minY = bounds.getMinY();
      else minY = bounds.getMaxY();
    
      if (planes[i].norm.getZ() >= 0) minZ = bounds.getMinZ();
      else minZ = bounds.getMaxZ();
    
      // Check if the minimum corner of the bounding box is entirely on
      // the outside of the plane
      if ((planes[i].norm.dotProduct (minX, minY, minZ) - planes[i].d) > 0) {
        answer = false;
        break;
      } // if
      
    } // for

    return (answer);

  } // intersects

  /////////////////////////////////////////////////////////////////

} // Frustum class

