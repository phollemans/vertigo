/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point3D;

import java.util.Map;
import java.util.HashMap;

import java.util.logging.Logger;

/**
 * The <code>GeoSurfaceHandler</code> class acts as the interface between
 * geographic dynamic surfaces and the Vertigo world controller.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class GeoSurfaceHandler {

  private static final Logger LOGGER = Logger.getLogger (GeoSurfaceHandler.class.getName());

  /** The singleton instance of the handler. */
  private static GeoSurfaceHandler instance;

  /** The map of surface to change listener. */
  private Map<DynamicSurface, ChangeListener<Point3D>> cameraListenerMap;

  /////////////////////////////////////////////////////////////////

  protected GeoSurfaceHandler() {
  
    cameraListenerMap = new HashMap<>();
  
  } // GeoSurfaceHandler

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the singleton instance of the handler.
   *
   * @return the singleton instance.
   */
  public static GeoSurfaceHandler getInstance () {
  
    if (instance == null) instance = new GeoSurfaceHandler();
    return (instance);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the context for creating geographic surfaces for the world
   * view.
   *
   * @return the view context.
   */
  public GeoSurfaceViewContext getContext () {

    var controller = WorldController.getInstance();
    var context = new GeoSurfaceViewContext();
    double radius = controller.getView().worldRadius();
    context.coordTrans = new SphereTranslator (radius);
    context.deltaFunc = (a,b) -> SphereFunctions.delta (a, b, radius);
    context.viewProps = controller.getView().getProperties();

    return (context);
    
  } // getContext
  
  /////////////////////////////////////////////////////////////////

  /**
   * Activates the specified surface by connecting it to the world controller.
   * This method must be called from the JavaFX application thread.
   *
   * @param surface the surface to activate.
   */
  public void activateSurface (
    DynamicSurface surface
  ) {
  
    if (!Platform.isFxApplicationThread()) throw new IllegalStateException ("Not on JavaFX application thread");
  
    var controller = WorldController.getInstance();
    surface.setFacetConsumer (facet -> controller.addObject (facet.getNode()));
    surface.setUpdateConsumer (update -> controller.addSceneGraphChange (update));

    ChangeListener<Point3D> listener = (obs, oldVal, newVal) -> {
      surface.setCameraPosition (newVal);
    };
    cameraListenerMap.put (surface, listener);
    controller.getView().cameraPositionProperty().addListener (listener);

    surface.setCameraPosition (controller.getView().cameraPositionProperty().get());
    surface.setActive (true);

  } // activateSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Deactivates the specified surface by disconnecting it from the world
   * controller.  This method must be called from the JavaFX application
   * thread.
   *
   * @param surface the surface to deactivate.
   */
  public void deactivateSurface (
    DynamicSurface surface
  ) {
  
    if (!Platform.isFxApplicationThread()) throw new IllegalStateException ("Not on JavaFX application thread");
  
    var listener = cameraListenerMap.remove (surface);
    if (listener == null) LOGGER.warning ("Camera position listener not found in cache");
    var controller = WorldController.getInstance();
    controller.getView().cameraPositionProperty().removeListener (listener);
    surface.setActive (false);
    surface.setFacetConsumer (null);
    surface.setUpdateConsumer (null);

    // TODO: In future versions we would selectively clear the facet nodes
    // from the controller that this surface contains.  For now though we
    // simply clear the entire model (we assume that only one surface is active
    // at once).
    controller.clearObjects();

  } // deactivateSurface
  
  /////////////////////////////////////////////////////////////////

} // GeoSurfaceHandler class

