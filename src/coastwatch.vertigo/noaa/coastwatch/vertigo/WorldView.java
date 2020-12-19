/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.transform.Rotate;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;

import javafx.stage.Screen;

import javafx.geometry.Point3D;

import javafx.event.EventType;
import javafx.event.EventHandler;
import javafx.event.Event;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>WorldView</code> class is the main view component of the MVC
 * for Vertigo.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WorldView {

  private static final Logger LOGGER = Logger.getLogger (WorldView.class.getName());

  // Constants
  // ---------

  /** The radius of the world for this view in model units. */
  public static final double WORLD_RADIUS = 5;

  /** The minimum camera Z coordinate value. */
  private static final double MIN_CAMERA_Z = -8*WORLD_RADIUS;

  /** The maximum camera Z coordinate value. */
//  private static final double MAX_CAMERA_Z = -WORLD_RADIUS-0.2;
  private static final double MAX_CAMERA_Z = -WORLD_RADIUS-3.5;

  /** The minimum zoom level in this view. */
  private static final double MIN_ZOOM_LEVEL = 0;

  /** The maximum zoom level in this view. */
  private static final double MAX_ZOOM_LEVEL = 100;

  /** The zoom level constants. */
  private static double A = (MAX_CAMERA_Z - MIN_CAMERA_Z) / (Math.sqrt (MAX_ZOOM_LEVEL) - Math.sqrt (MIN_ZOOM_LEVEL));
  private static double B = MIN_CAMERA_Z - A*Math.sqrt (MIN_ZOOM_LEVEL);

  // Variables
  // ---------

  /** The rotation angle about the X axis. */
  private DoubleProperty xAngleProp;
  
  /** The rotation angle about the Y axis. */
  private DoubleProperty yAngleProp;

  /** The camera Z position. */
  private DoubleProperty cameraZProp;

  /** The camera zoom level. */
  private DoubleProperty cameraZoomProp;

  /** The camera position in the reference frame of the rotating objects. */
  private ObjectProperty<Point3D> cameraPositionProp;

  /** The subscene that this world is viewed in. */
  private SubScene scene;

  /** The objects being viewed. */
  private Group objects;

  /** The perspective projection camera. */
  private PerspectiveCamera camera;

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the camera position in the reference frame of the view objects.
   *
   * @return the camera position property.
   */
  public ReadOnlyObjectProperty<Point3D> cameraPositionProperty() { return (cameraPositionProp); }

  /////////////////////////////////////////////////////////////////

  /** Creates a new view object. */
  public WorldView () {

    // Create a group for all model objects
    objects = new Group();

    // Set up the rotation of the objects
    Rotate xRotate = new Rotate (0, 0, 0, 0, Rotate.X_AXIS);
    xAngleProp = xRotate.angleProperty();
    Rotate yRotate = new Rotate (0, 0, 0, 0, Rotate.Y_AXIS);
    yAngleProp = yRotate.angleProperty();

    // It seems that here, the transforms in the node list are applied in the
    // opposite order of the list.  We want to rotate the objects around the
    // vertical Y axis first, then rotate the objects about the horizontal X
    // axis next.  In order to do that, we specify the X and then Y rotation
    // in the list.  So it seems that Y is applied to the points first, and
    // then X is applied.  This is confirmed by the documentation
    // for the Affine.append(Transform) method, which says that a node
    // transforms list with (X, Y) in that order is the same as performing
    // a matrix multiplication as X(Y(node)), which means Y is being applied
    // first and then X.
    objects.getTransforms().addAll (
      xRotate,
      yRotate
    );

    // Create a root group and put the objects in
    Group root = new Group();
    root.getChildren().add (objects);

    // Create the camera
    camera = new PerspectiveCamera (true);
    cameraZProp = camera.translateZProperty();
    cameraZoomProp = new SimpleDoubleProperty (MIN_ZOOM_LEVEL);
    cameraZoomProp.addListener ((obs, oldVal, newVal) -> {
      double level = newVal.doubleValue();
      setZoom (level);
    });
    setZoom (MIN_ZOOM_LEVEL);

    // Create the position property and link its value to the angles and
    // camera Z value.
    cameraPositionProp = new SimpleObjectProperty<> (this, "cameraPosition");
    cameraZProp.addListener ((obs, oldVal, newVal) -> updateCameraPosition());
    xAngleProp.addListener ((obs, oldVal, newVal) -> updateCameraPosition());
    yAngleProp.addListener ((obs, oldVal, newVal) -> updateCameraPosition());
    updateCameraPosition();
    
     // Create the scene and add the camera
    scene = new SubScene (root, 500, 500, false, SceneAntialiasing.BALANCED);
    scene.setFill (Color.rgb (2, 2, 8));
    scene.setCamera (camera);

  } // WorldView

  /////////////////////////////////////////////////////////////////

  /**
   * Updates the camera position property using the camera Z value and
   * rotation angles.
   */
  private void updateCameraPosition() {

    Point3D pos = objects.sceneToLocal (new Point3D (0, 0, cameraZProp.getValue()));
    cameraPositionProp.setValue (pos);

  } // updateCameraPosition

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the frustum for the perspective camera, in the local reference
   * frame of the view objects.
   *
   * @return the frustum describing the planes of the perspective camera bounds.
   */
  public Frustum getFrustum () {

    Affine cameraAffine = new Affine();
    cameraAffine.prependRotation (-xAngleProp.getValue(), 0, 0, 0, Rotate.X_AXIS);
    cameraAffine.prependRotation (-yAngleProp.getValue(), 0, 0, 0, Rotate.Y_AXIS);

    Point3D cameraPos = new Point3D (0, 0, cameraZProp.getValue());

    Frustum frustum = new Frustum (cameraAffine, cameraPos, camera.getFieldOfView(),
      camera.getNearClip(), camera.getFarClip(), scene.getWidth()/scene.getHeight());

    return (frustum);

  } // getFrustum

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the scene created by this view.
   *
   * @return the scene.
   */
  public SubScene getScene () { return (scene); }

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the zoom level for the view.
   *
   * @param level the new zoom level in the range [minZoomLevel(),
   * maxZoomLevel()].
   */
  private void setZoom (double level) {

    // Zoom 0 -> 100: max camera distance -> min camera distance
    // Zoom level is not linear, depends on distance from surface
    // of world.

    // Compute the z value and update the far clipping plane, because we
    // really only want to show objects on the near side of the world.
    double z = A*Math.sqrt (level) + B;
    camera.setFarClip (-z);

//camera.setFarClip (100);

    // Update the camera z
    cameraZProp.setValue (z);
    if (LOGGER.isLoggable (Level.FINEST)) LOGGER.finest ("Camera Z = " + z);

  } // setZoom

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the zoom level for the specified camera distance.
   *
   * @param dist the camera distance in graphics system model units in the
   * range [minCameraDistance(), maxCameraDistance()].
   *
   * @return the zoom level for the specified distance.
   *
   * @since 0.6
   */
  public double getZoomForDistance (double dist) {

    double level;
  
    if (dist <= minCameraDistance()) level = MAX_ZOOM_LEVEL;
    else if (dist >= maxCameraDistance()) level = MIN_ZOOM_LEVEL;
    else {
      double z = -WORLD_RADIUS - dist;
      level = Math.pow ((z - B)/A, 2);
    } // else

    return (level);

  } // getZoomForDistance

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the zoom level for the specified extent.
   *
   * @param angle the extent angle in degrees.  This is the angle along the
   * surface of the world in the vertical direction that will be visible when
   * the view is zoomed at the computed zoom level.
   *
   * @return the zoom level that will show the specified extent.
   */
  public double getZoomForExtent (double angle) {

    double phi = Math.toRadians (camera.getFieldOfView());
    double theta = Math.toRadians (angle/2);
    double z = -WORLD_RADIUS * (Math.sin (theta)/Math.tan (phi/2) + Math.cos (theta));
    double level = Math.pow ((z - B)/A, 2);
    if (level < MIN_ZOOM_LEVEL) level = MIN_ZOOM_LEVEL;
    else if (level > MAX_ZOOM_LEVEL) level = MAX_ZOOM_LEVEL;

    return (level);

  } // getZoomForExtent

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an object to this view.
   *
   * @param object the object to add.
   */
  public void addObject (
    Node object
  ) {

    objects.getChildren().add (object);

  } // addObject

  /////////////////////////////////////////////////////////////////

  /**
   * Removes an object from this view.
   *
   * @param object the object to remove.
   */
  public void removeObject (
    Node object
  ) {

    objects.getChildren().remove (object);

  } // removeObject

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an object to this view that does not rotate with the main objects.
   *
   * @param object the object to add.
   */
  public void addRootObject (
    Node object
  ) {

    ((Group) scene.getRoot()).getChildren().add (object);

  } // addRootObject

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an event handler to the group of objects added to the scene via
   * {@link #addObject}.
   *
   * @param type the type of the events to receive by the handler.
   * @param handler the handler to register.
   */
  public <T extends Event> void addObjectEventHandler​ (
    EventType<T> type,
    EventHandler<? super T> handler
  ) {
  
    objects.addEventHandler (type, handler);
  
  } // addObjectEventHandler

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an event handler to the scene in general.
   *
   * @param type the type of the events to receive by the handler.
   * @param handler the handler to register.
   */
  public <T extends Event> void addSceneEventHandler​ (
    EventType<T> type,
    EventHandler<? super T> handler
  ) {
  
    scene.addEventHandler (type, handler);
  
  } // addSceneEventHandler

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the X axis rotation angle property.  This is the angle that the
   * view is currently rotated about the X axis.
   *
   * @return the angle property.
   */
  public DoubleProperty xAngleProperty() { return (xAngleProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the Y axis rotation angle property.  This is the angle that the
   * view is currently rotated about the Y axis.
   *
   * @return the angle property.
   */
  public DoubleProperty yAngleProperty() { return (yAngleProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the camera zoom level property.  The camera zoom level determines
   * the camera location on the Z axis in such a way as to provide similar
   * visual differences between two adjacent zoom levels close to the minimum
   * or close to the maximum.
   *
   * @return the zoom level property.
   */
  public DoubleProperty cameraZoomProperty() { return (cameraZoomProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the world radius in graphics system model units.
   *
   * @return the world radius.
   */
  public double worldRadius() { return (WORLD_RADIUS); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the minimum camera distance from the surface in graphics system model units.
   *
   * @return the minimum camera distance.
   *
   * @since 0.6
   */
  public double minCameraDistance() { return (-WORLD_RADIUS - MAX_CAMERA_Z); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the maximum camera distance from the surface in graphics system model units.
   *
   * @return the maximum camera distance.
   *
   * @since 0.6
   */
  public double maxCameraDistance() { return (-WORLD_RADIUS - MIN_CAMERA_Z); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the camera distance from the surface in graphics system model units.
   *
   * @return the camera distance.
   *
   * @since 0.6
   */
  public double cameraDistance() { return (-WORLD_RADIUS - cameraZProp.getValue()); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the maximum camera zoom level.
   *
   * @return the maximum camera zoom level.
   */
  public double maxZoomLevel() { return (MAX_ZOOM_LEVEL); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the minimum camera zoom level.
   *
   * @return the minimum camera zoom level.
   */
  public double minZoomLevel() { return (MIN_ZOOM_LEVEL); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the properties of the current view state.
   *
   * @return the view properties.
   */
  public ViewProperties getProperties() {

    ViewProperties props = new ViewProperties();

    // Detect the screen that data is going to be displayed on.
    var screen = Screen.getPrimary();
    var bounds = screen.getBounds();
    int width = (int) bounds.getWidth();
    int height = (int) bounds.getHeight();
    String dpiProp = System.getProperty ("dpi");
    int dpi = (dpiProp != null ? Integer.parseInt (dpiProp) : (int) screen.getDpi());
    LOGGER.fine ("Detected display of dimensions " + width + "x" + height + " at " + dpi + " DPI");

    // We compute an approximate vertical resolution here, assuming the
    // view takes up half the screen.
    props.vres = (int) Math.round ((height/2.0)*(dpi/81.0));

    // Make it so that the accuracy of the model is such that it's at most
    // 4 pixels from the exact value.
    props.tau = 4;
//    props.tau = 2;
    
    // Set the other parameters accordingly.
    double fov = camera.getFieldOfView();
    props.tan_phi_o_2 = Math.tan (Math.toRadians (fov/2));
    props.cmin = -WORLD_RADIUS - MAX_CAMERA_Z;
    props.cmax = -WORLD_RADIUS - MIN_CAMERA_Z;

    return (props);

  } // getProperties

  /////////////////////////////////////////////////////////////////

} // WorldView class
