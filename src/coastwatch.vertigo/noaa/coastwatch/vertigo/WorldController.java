/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import javafx.application.Platform;

import javafx.scene.Node;
import javafx.scene.input.PickResult;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ZoomEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

import javafx.scene.AmbientLight;
import javafx.scene.PointLight;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.Box;
import javafx.scene.shape.Shape3D;
import javafx.scene.shape.DrawMode;

import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;

import javafx.geometry.Point3D;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;

import javafx.util.Duration;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;

import static noaa.coastwatch.vertigo.SphereFunctions.THETA;
import static noaa.coastwatch.vertigo.SphereFunctions.PHI;

/**
 * The <code>WorldController</code> class is the main controller component of
 * the MVC for Vertigo.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WorldController {

  private static final Logger LOGGER = Logger.getLogger (WorldController.class.getName());

  // Constants
  // ---------

  private static final int POSITION_SHIFT_DURATION = 1500;
  private static final int SHIFT_ANGLE = 60;
  private static final int CAMERA_ZOOM_DURATION = POSITION_SHIFT_DURATION;
  private static final int SCENE_CHANGES_PER_PULSE = 2;

  private static final double BAACKGROUND_VIEW_ORDER = 20;
  private static final double BASE_VIEW_ORDER = 10;
  private static final double USER_VIEW_ORDER = 5;
  private static final double FLOAT_VIEW_ORDER = 1;

  // Variables
  // ---------

  /** The view used for display. */
  private WorldView view;

  /** The model being controlled. */
  private WorldModel model;
  
  /** The point that dragging started. */
  private Point3D dragStartPoint;

  /** The view angles that dragging started at. */
  private double dragStartXAngle, dragStartYAngle;

  /** The timeline used for shifting position. */
  private Timeline shiftTimeline;

  /** The timeline used for zooming the camera. */
  private Timeline zoomTimeline;

  /** The globe object that view controls manipulate. */
  private MeshView globe;
  
  /** The graticule object for globe reference. */
  private MeshView graticule;

  /** The drawing mode flag for 3D shapes. */
  private boolean lineMode;

  /** The number of camera position updates since the last counter reset. */
  private int cameraUpdatesSinceReset;

  /** The queue for accumlating and performing scene graph changes. */
  private ConcurrentLinkedQueue<Runnable> sceneGraphChangeQueue;

  /** The intersection point between the view cursor and globe (possibly null). */
  private ObjectProperty<Point3D> globeIntersectProp;

  /** The coordinate translator between geographic and model coordinates (possibly null). */
  private ObjectProperty<GeoCoordinateTranslator> coordTransProp;

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the property that holds the intersection point between the view
   * cursor and the globe surface.  The intersection point may be null when
   * the view cursor is not intersecting the globe.
   *
   * @return the globe intersection property.
   *
   * @since 0.6
   */
  public ReadOnlyObjectProperty<Point3D> globeIntersectProperty() { return (globeIntersectProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the property that holds the translator between geographic and
   * model coordinates for the world.  A default translator property is
   * initially created based on spherical coordinates.
   *
   * @return the coordinate translator property.
   *
   * @since 0.6
   */
  public ObjectProperty<GeoCoordinateTranslator> coordTransProperty() { return (coordTransProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Adds a change in the form of a runnable object to the scene graph change
   * queue.  The change will be performed as time permits either less quickly
   * while the view is being animated, or more quickly when the view is at
   * rest.
   *
   * @param change the scene graph change to perform.
   */
  public void addSceneGraphChange (
    Runnable change
  ) {

    sceneGraphChangeQueue.add (change);

  } // addSceneGraphChange
  
  /////////////////////////////////////////////////////////////////

  /** Creates a new controller object with default view and empty model. */
  public WorldController () {

    view = new WorldView();
    double radius = view.worldRadius();
    Bounds bounds = new BoundingBox (-radius, -radius, -radius, radius*2, radius*2, radius*2);
    model = new WorldModel (bounds);
    sceneGraphChangeQueue = new ConcurrentLinkedQueue<>();
    globeIntersectProp = new SimpleObjectProperty<> (this, "globeIntersect");
    coordTransProp = new SimpleObjectProperty<> (this, "coordTrans", new SphereTranslator (radius));

    // Set up the timer that polls for scene graph changes and performs
    // them when no animations are happening.  We assume that animation has
    // stopped when there are no camera updates over the course of about
    // 20 pulses (= 1/60 s * 20).
    Timer updateTimer = new Timer (true);
    updateTimer.scheduleAtFixedRate (new TimerTask () {
      public void run() {
        Platform.runLater (() -> {
          if (cameraUpdatesSinceReset == 0) performSceneChanges (Integer.MAX_VALUE);
          else cameraUpdatesSinceReset = 0;
        });
      } // run
    }, 0, 333);

    // Add the various change and event listeners.
    addViewReference();
    addViewMouseControls();
//    addViewKeyboardControls();
    addViewListeners();

  } // WorldController

  /////////////////////////////////////////////////////////////////

  /** Stops any animation in progress. */
  private void stopAnimation () {

    if (zoomTimeline != null) zoomTimeline.stop();
    if (shiftTimeline != null) shiftTimeline.stop();

  } // stopAnimation

  /////////////////////////////////////////////////////////////////

  /** Adds the standard view reference objects. */
  private void addViewReference () {

    // First add the globe that the user will be using as the basis
    // of manipulating the view
    MeshObjectFactory factory = MeshObjectFactory.getInstance();
    float radius = (float) view.worldRadius();
    globe = new MeshView (factory.createGeodesicPolyhedron (radius, 3));
    globe.setMaterial (new PhongMaterial (Color.color (0.3, 0.3, 0.3)));
    globe.setViewOrder (BASE_VIEW_ORDER);
    view.addObject (globe);
    
    LOGGER.fine ("Globe has " +
      (((TriangleMesh) globe.getMesh()).getFaces().size() / 6) + " faces");

    // Add some default ambient and point lights
    AmbientLight ambient = new AmbientLight (Color.color (0.5, 0.5, 0.5));
//    AmbientLight ambient = new AmbientLight (Color.color (1, 1, 1));
    PointLight point = new PointLight (Color.color (0.55, 0.55, 0.55));
    point.setTranslateX (-25);
    point.setTranslateY (-25);
    point.setTranslateZ (-50);
    view.addRootObject (ambient);
    view.addRootObject (point);

    // Add the graticule
    float lineWidth = radius/6371 * 20;
    graticule = new MeshView (factory.createSphericalGraticule (lineWidth, radius));
    graticule.setMaterial (new PhongMaterial (Color.rgb (255, 213, 63)));
    graticule.setMouseTransparent (true);
    graticule.setViewOrder (FLOAT_VIEW_ORDER);
    view.addObject (graticule);
    
    LOGGER.fine ("Graticule has " +
      (((TriangleMesh) graticule.getMesh()).getFaces().size() / 6) + " faces");

    // Set the graticule to only be lit with full ambient light
    AmbientLight fullAmbient = new AmbientLight (Color.WHITE);
    view.addRootObject (fullAmbient);
    point.getExclusionScope().add (graticule);
    ambient.getExclusionScope().add (graticule);
    fullAmbient.getScope().add (graticule);

    // Add the stars
//    var stars = StarFieldFactory.getInstance().createField (radius/6371);
//    stars.setViewOrder (BAACKGROUND_VIEW_ORDER);
//    view.addObject (stars);
//    point.getExclusionScope().add (stars);
//    ambient.getExclusionScope().add (stars);
//    fullAmbient.getScope().add (stars);

  } // addViewReference

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the graticule visible property, by default set to true.
   *
   * @return the graticule visible property.
   *
   * @since 0.6
   */
  public BooleanProperty graticuleVisibleProperty () { return (graticule.visibleProperty()); }

  /////////////////////////////////////////////////////////////////

  /** Adds the standard view mouse/trackpad manipulation controls. */
  private void addViewMouseControls () {

    // When the mouse is pressed, record the start so we can use it
    // in the drag
    view.addObjectEventHandler (MouseEvent.MOUSE_PRESSED, event -> {
      PickResult result = event.getPickResult();
      Node node = result.getIntersectedNode();
      dragStartPoint = node.localToScene (result.getIntersectedPoint());
      dragStartXAngle = view.xAngleProperty().getValue();
      dragStartYAngle = view.yAngleProperty().getValue();
    });

    // When the mouse is dragging, adjust the x and y rotation angles
    // accordingly
    view.addObjectEventHandler (MouseEvent.MOUSE_DRAGGED, event -> {
      PickResult result = event.getPickResult();
      Node node = result.getIntersectedNode();
      if (node == globe) {
        Point3D dragEndPoint = node.localToScene (result.getIntersectedPoint());

        double[] sphereStart = new double[2];
        SphereFunctions.pointToSphere (dragStartPoint, sphereStart);

        double[] sphereEnd = new double[2];
        SphereFunctions.pointToSphere (dragEndPoint, sphereEnd);

        double dTheta = sphereEnd[THETA] - sphereStart[THETA];
        double dPhi = - (sphereEnd[PHI] - sphereStart[PHI]);

        double newXAngle = dragStartXAngle + Math.toDegrees (dTheta);
        if (newXAngle < -90) newXAngle = -90;
        else if (newXAngle > 90) newXAngle = 90;
        
        double newYAngle = dragStartYAngle + Math.toDegrees (dPhi);
        while (newYAngle < 180) newYAngle += 360;
        while (newYAngle > 180) newYAngle -= 360;

        view.xAngleProperty().setValue (newXAngle);
        view.yAngleProperty().setValue (newYAngle);

      } // if
    });

    // Clean up after dragging
    view.addObjectEventHandler (MouseEvent.MOUSE_RELEASED, event -> {
      dragStartPoint = null;
    });

    // Perform a shift animation on double click
    view.addObjectEventHandler (MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getClickCount() == 2) {
        PickResult result = event.getPickResult();
        Node node = result.getIntersectedNode();
        if (node == globe) {
          Point3D point = result.getIntersectedPoint();
          double[] sphere = new double[2];
          SphereFunctions.pointToSphere (point, sphere);
          double newXAngle = 90 - Math.toDegrees (sphere[THETA]);
          double newYAngle = Math.toDegrees (sphere[PHI]);
          startShift (newXAngle, newYAngle);
        } // if
      } // if
    });

    // Modify the zoom on scroll
    view.addSceneEventHandler (ScrollEvent.SCROLL, event -> {
      double level = view.cameraZoomProperty().getValue();
      double min = view.minZoomLevel();
      double max = view.maxZoomLevel();
      double delta = event.getDeltaY() / ((max - min) / 3.5);
      double newLevel = level + delta;
      if (newLevel < min) newLevel = min;
      else if (newLevel > max) newLevel = max;
      if (newLevel != level) view.cameraZoomProperty().setValue (newLevel);
    });

    // Modify the zoom on zoom gesture
    view.addSceneEventHandler (ZoomEvent.ZOOM, event -> {
      double newLevel = view.getZoomForDistance (view.cameraDistance()/event.getZoomFactor());
      view.cameraZoomProperty().setValue (newLevel);
    });

    // Perform a zoom animation on double click
    view.addSceneEventHandler (MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getClickCount() == 2) {
        MouseButton button = event.getButton();
        switch (button) {
        case PRIMARY: startZoom (ZoomDirection.ZOOM_IN); break;
        case SECONDARY: startZoom (ZoomDirection.ZOOM_OUT); break;
        } // switch
      } // if
    });

    // Stop the animation on single click
    view.addSceneEventHandler (MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getClickCount() == 1) {
        stopAnimation();
        view.getScene().requestFocus();
      } // if
    });

    // Capture globe intersect point on mouse movement
    view.addSceneEventHandler (MouseEvent.MOUSE_MOVED, event -> {
      PickResult result = event.getPickResult();
      Node node = result.getIntersectedNode();
      if (node == globe) {
        Point3D point = result.getIntersectedPoint();
        globeIntersectProp.setValue (point);
      } // if
      else {
        globeIntersectProp.setValue (null);
      } // else
    });

    view.addSceneEventHandler (MouseEvent.MOUSE_EXITED, event -> {
      globeIntersectProp.setValue (null);
    });

    view.cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
      globeIntersectProp.setValue (null);
    });

  } // addViewMouseControls

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a shift animation to the specified view angles.
   *
   * @param newXAngle the new X angle value.
   * @param newYAngle the new Y angle value.
   */
  private void startShift (
    double newXAngle,
    double newYAngle
  ) {

    // Reset the Y angle so that the world doesn't rotate further than
    // 180 degrees when shifting to the new coordinates.
    double yAngle = view.yAngleProperty().getValue();
    if (Math.abs (newYAngle - yAngle) > 180) {
      if (yAngle < 0) yAngle += 360;
      else yAngle -= 360;
      view.yAngleProperty().setValue (yAngle);
    } // if

    // Start the shift animation.
    KeyValue keyXValue = new KeyValue (view.xAngleProperty(), newXAngle, Interpolator.EASE_BOTH);
    KeyValue keyYValue = new KeyValue (view.yAngleProperty(), newYAngle, Interpolator.EASE_BOTH);
    KeyFrame frame = new KeyFrame (Duration.millis (POSITION_SHIFT_DURATION), keyXValue, keyYValue);
    shiftTimeline = new Timeline (frame);
    shiftTimeline.play();

  } // startShift
  
  /////////////////////////////////////////////////////////////////

  /**
   * Starts a zoom animation to the specified level.
   *
   * @param newLevel the new zoom level.  The level must be within in the
   * zoom level bounds specified by the view.
   */
  private void startZoom (double newLevel) {

    DoubleProperty zoomProp = view.cameraZoomProperty();
    KeyValue keyValue = new KeyValue (zoomProp, newLevel, Interpolator.EASE_BOTH);
    KeyFrame frame = new KeyFrame (Duration.millis (CAMERA_ZOOM_DURATION), keyValue);
    zoomTimeline = new Timeline (frame);
    zoomTimeline.play();

  } // startZoom

  /////////////////////////////////////////////////////////////////

  /** The possible directions for zoom operations. */
  private enum ZoomDirection { ZOOM_IN, ZOOM_OUT }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a zoom animation in the specified direction.
   *
   * @param dir the zoom direction, either ZOOM_IN or ZOOM_OUT.
   */
  private void startZoom (ZoomDirection dir) {
  
    DoubleProperty zoomProp = view.cameraZoomProperty();
    double level = zoomProp.getValue();
    int mult = (dir == ZoomDirection.ZOOM_IN ? 1 : -1);
    double newLevel = level + mult*(view.maxZoomLevel() - view.minZoomLevel())/3.0;
    if (newLevel > view.maxZoomLevel()) newLevel = view.maxZoomLevel();
    if (newLevel < view.minZoomLevel()) newLevel = view.minZoomLevel();
    if (newLevel != level) startZoom (newLevel);
  
  } // startZoom

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a zoom and shift animation to the specified angles and level.
   *
   * @param newXAngle the new X angle value in the range [-90..90].
   * @param newYAngle the new Y angle value in the range [-180..180].
   * @param angle the new extent angle in the range [0..180].
   */
  public void zoomTo (
    double newXAngle,
    double newYAngle,
    double angle
  ) {

    startShift (newXAngle, newYAngle);
    startZoom (view.getZoomForExtent (angle));

  } // zoomTo

  /////////////////////////////////////////////////////////////////

  /** The possible directions for shift operations. */
  public enum ShiftDirection { NONE, NORTH, SOUTH, EAST, WEST }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a shift animation in the specified direction.
   *
   * @param direction the direction to shift towards.
   * @param rate the shift rate, where the normal rate is 1, half is 0.5,
   * double is 2, etc.
   */
  public void shiftTo (
    ShiftDirection direction,
    double rate
  ) {

    double xAngleInc = 0, yAngleInc = 0;
    switch (direction) {
    case WEST: yAngleInc = -SHIFT_ANGLE; break;
    case EAST: yAngleInc = SHIFT_ANGLE; break;
    case NORTH: xAngleInc = SHIFT_ANGLE; break;
    case SOUTH: xAngleInc = -SHIFT_ANGLE; break;
    } // switch

    double angleFactor = view.cameraDistance() / view.maxCameraDistance();

    double xAngle = view.xAngleProperty().getValue();
    double newXAngle = xAngle + xAngleInc*angleFactor;
    if (newXAngle < -90) newXAngle = -90;
    else if (newXAngle > 90) newXAngle = 90;

    double yAngle = view.yAngleProperty().getValue();
    int sign = yAngleInc > 0 ? 1 : -1;
    
    // This next line helps with making east/west shifts appear similar in
    // distance to north/south shifts.  Except for two cases: above 75 degrees
    // we limit the latitude factor, and when zoomed far out at high latitudes,
    // we limit the total shift angle.  This approximates reasonable shift
    // behaviour.
    double newYAngle = yAngle + sign*Math.min (SHIFT_ANGLE, Math.abs (yAngleInc)*angleFactor / Math.cos (Math.toRadians (Math.min (75, Math.abs (xAngle)))));

    if (yAngle < 0) yAngle += 360;
    else if (yAngle > 360) yAngle -= 360;

    if (newXAngle != xAngle || newYAngle != yAngle) {
      stopAnimation();
      KeyValue keyXValue = new KeyValue (view.xAngleProperty(), newXAngle, Interpolator.EASE_BOTH);
      KeyValue keyYValue = new KeyValue (view.yAngleProperty(), newYAngle, Interpolator.EASE_BOTH);
      int duration = (int) (POSITION_SHIFT_DURATION/rate);
      KeyFrame frame = new KeyFrame (Duration.millis (duration), keyXValue, keyYValue);
      shiftTimeline = new Timeline (frame);
      shiftTimeline.play();
    } // if

  } // shiftTo
  
  /////////////////////////////////////////////////////////////////

  /**
   * Starts a zoom in animation.
   *
   * @since 0.6
   */
  public void zoomIn () { startZoom (ZoomDirection.ZOOM_IN); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a zoom out animation.
   *
   * @since 0.6
   */
  public void zoomOut () { startZoom (ZoomDirection.ZOOM_OUT); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a north shift animation.
   *
   * @since 0.6
   */
  public void shiftNorth () { shiftTo (ShiftDirection.NORTH, 1.7); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a south shift animation.
   *
   * @since 0.6
   */
  public void shiftSouth () { shiftTo (ShiftDirection.SOUTH, 1.7); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a east shift animation.
   *
   * @since 0.6
   */
  public void shiftEast () { shiftTo (ShiftDirection.EAST, 1.7); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts a west shift animation.
   *
   * @since 0.6
   */
  public void shiftWest () { shiftTo (ShiftDirection.WEST, 1.7); }

  /////////////////////////////////////////////////////////////////

//  /** Adds the standard view keyboard manipulation controls. */
//  private void addViewKeyboardControls () {
//
//    // Change the view zoom in response to A or Z.
//    view.addSceneEventHandler (KeyEvent.KEY_PRESSED, event -> {
//      switch (event.getCode()) {
//      case A: startZoom (ZoomDirection.ZOOM_IN); break;
//      case Z: startZoom (ZoomDirection.ZOOM_OUT); break;
//      } // switch
//    });
//
//    // Change the view X and Y rotation angles in response
//    // to the arrow keys.
//    view.addSceneEventHandler (KeyEvent.KEY_PRESSED, event -> {
//      ShiftDirection direction = ShiftDirection.NONE;
//      switch (event.getCode()) {
//      case J: direction = ShiftDirection.WEST; break;
//      case L: direction = ShiftDirection.EAST; break;
//      case I: direction = ShiftDirection.NORTH; break;
//      case K: direction = ShiftDirection.SOUTH; break;
//      } // switch
//      if (direction != ShiftDirection.NONE) {
//        shiftTo (direction, 1.7);
//      } // if
//    });
//
//  } // addViewKeyboardControls

  /////////////////////////////////////////////////////////////////

  /** Updates the model for frustum changes. */
  private void updateFrustum () {

    // Find the visible nodes and turn all others off
    Frustum frustum = view.getFrustum();
    long start = System.nanoTime();
    Set<Node> visible = model.findVisible (frustum);
    long duration = System.nanoTime() - start;
    if (LOGGER.isLoggable (Level.FINEST)) {
      LOGGER.finest ("Completed frustum search in " + duration*1e-6f + " ms");
      LOGGER.finest ("View frustum matches " + visible.size() + " objects");
    } // if
    model.updateVisibility (visible);

  } // updateFrustum

  /////////////////////////////////////////////////////////////////

  /** Starts a demo mode that periodically performs random shifts in the view. */
  public void startDemoMode () {

    double inZoom = view.minZoomLevel()*0.3 + view.maxZoomLevel()*0.7;
    double outZoom = view.minZoomLevel()*0.8 + view.maxZoomLevel()*0.2;
    DoubleProperty zoomProp = view.cameraZoomProperty();
    zoomProp.setValue (inZoom);
    int demoDuration = POSITION_SHIFT_DURATION*2;

    Timer demoTimer = new Timer (true);
    demoTimer.scheduleAtFixedRate (new TimerTask () {
      public void run() { Platform.runLater (() -> {

        Point3D cartesianPoint = new Point3D (
          Math.random()*2 - 1,
          Math.random()*2 - 1,
          Math.random()*2 - 1
        );
        double[] spherePoint = new double[2];
        SphereFunctions.pointToSphere (cartesianPoint, spherePoint);
        double newXAngle = 90 - Math.toDegrees (spherePoint[THETA]);
        double newYAngle = Math.toDegrees (spherePoint[PHI]);

        double yAngle = view.yAngleProperty().getValue();
        if (Math.abs (newYAngle - yAngle) > 180) {
          if (yAngle < 0) yAngle += 360;
          else yAngle -= 360;
          view.yAngleProperty().setValue (yAngle);
        } // if

        LOGGER.fine ("New X angle = " + newXAngle + " deg");
        LOGGER.fine ("New Y angle = " + newYAngle + " deg");

        stopAnimation();

        KeyValue keyXValue = new KeyValue (view.xAngleProperty(), newXAngle, Interpolator.EASE_BOTH);
        KeyValue keyYValue = new KeyValue (view.yAngleProperty(), newYAngle, Interpolator.EASE_BOTH);
        KeyFrame frame = new KeyFrame (Duration.millis (demoDuration), keyXValue, keyYValue);
        shiftTimeline = new Timeline (frame);
        shiftTimeline.play();

        KeyValue keyValueOutZoom = new KeyValue (zoomProp, outZoom, Interpolator.EASE_BOTH);
        KeyFrame frameOutZoom = new KeyFrame (Duration.millis (demoDuration/2), keyValueOutZoom);
        KeyValue keyValueInZoom = new KeyValue (zoomProp, inZoom, Interpolator.EASE_BOTH);
        KeyFrame frameInZoom = new KeyFrame (Duration.millis (demoDuration), keyValueInZoom);

        zoomTimeline = new Timeline (frameOutZoom, frameInZoom);
        zoomTimeline.play();

      });
    }}, demoDuration, demoDuration*2);
  
  } // startDemoMode

  /////////////////////////////////////////////////////////////////

  /** Adds listeners for view changes. */
  private void addViewListeners () {

    ChangeListener<Number> listener = (obs, oldVal, newVal) -> {
      if (oldVal != newVal) updateFrustum();
    };
    
    // Set up the listeners for angle changes
    view.xAngleProperty().addListener (listener);
    view.yAngleProperty().addListener (listener);

    // Set up the listener for zoom changes (ie: camera z position)
    view.cameraZoomProperty().addListener (listener);

    // Set up listeners for view size changes
    view.getScene().widthProperty().addListener (listener);
    view.getScene().heightProperty().addListener (listener);

    // Set up for handling metered view scene graph changes.  We do this
    // by allowing only some number of scene graph changes for each
    // camera update.
    view.cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
      cameraUpdatesSinceReset++;
      performSceneChanges (SCENE_CHANGES_PER_PULSE);
    });

  } // addViewListeners

  /////////////////////////////////////////////////////////////////

  /**
   * Performs a number of scene graph changes, up to a specific limit.
   *
   * @param limit the maximum number of scene graph changes to make.
   */
  private void performSceneChanges (
    int limit
  ) {

    int count = 0;
    while (!sceneGraphChangeQueue.isEmpty() && count < limit) {
      Runnable change = sceneGraphChangeQueue.remove();
      change.run();
      count++;
    } // while

    if (count != 0) LOGGER.finer ("Performed " + count + " scene graph changes");

  } // performSceneChanges

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the line mode flag for objects added to this controller.  In line
   * mode, object outlines only are drawn.  This helps to show the mesh
   * structure that objects are composed of for testing.  By default line mode
   * is false and objects are filled with their colour or texture.  Line mode
   * must be set before any objects are added.
   *
   * @param lineMode the line mode flag, true to show only lines.
   */
  public void setLineMode (
    boolean lineMode
  ) {
    
    this.lineMode = lineMode;
  
  } // setLineMode

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an object to the view and model.
   *
   * @param object the object to add.
   */
  public void addObject (
    Node object
  ) {

    Frustum frustum = view.getFrustum();
    boolean visible = frustum.intersects (object.getBoundsInLocal());
    object.setVisible (visible);
    model.addObject (object);
    if (LOGGER.isLoggable (Level.FINEST)) model.summarize();

    object.setMouseTransparent (true);
    object.setViewOrder (USER_VIEW_ORDER);
    if (lineMode && object instanceof Shape3D) {
      ((Shape3D) object).setDrawMode (DrawMode.LINE);
    } // if
    view.addObject (object);

  } // addObject

  /////////////////////////////////////////////////////////////////

  /** Clears the objects from the view and model. */
  public void clearObjects() {

    Set<Node> objectSet = model.getObjects();
    for (Node node : objectSet) view.removeObject (node);
    model.clear();
  
  } // clearObjects

  /////////////////////////////////////////////////////////////////

  /**
   * Removes an object from the view.
   *
   * @param object the object to remove.
   */
  public void removeObject (
    Node object
  ) {

    // TODO: We need to implement an object removeal in the
    // tree.  Since we don't have that yet, the recommended way
    // to remove objects is to clear the model and add new objects.
    
    // model.removeObject (object);
    // view.removeObject (object);

    throw new UnsupportedOperationException();

  } // removeObject

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the view created by this controller.
   *
   * @return the view.
   */
  public WorldView getView () { return (view); }

  /////////////////////////////////////////////////////////////////

  /**
   * Starts the controller functioning.  Should be called from the
   * JavaFX Application thread after the application is started.
   */
  public void start() {
  
    if (Platform.isFxApplicationThread()) {
      view.getScene().requestFocus();
      updateFrustum();
    } // if
  
  } // start

  /////////////////////////////////////////////////////////////////

} // WorldController class
