/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Point3D;

import java.util.List;
import java.util.ArrayList;

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

  /** The controller for the world view and model. */
  private WorldController worldController;

  /** The list of active surfaces. */
  private List<DynamicSurface> surfaceList;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a surface handler for the specified world controller.
   *
   * @param worldController the world controller to handle surfaces for.
   *
   * @since 0.6
   */
  public GeoSurfaceHandler (
    WorldController worldController
  ) {
  
    this.worldController = worldController;
    surfaceList = new ArrayList<>();
  
    // Update the surface camera positions when needed
    var view = worldController.getView();
    view.cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
      surfaceList.forEach (surface -> surface.setCameraPosition (newVal));
    });

    // Force an update of the surfaces if the scene changes size
    var scene = view.getScene();
    ChangeListener<Number> listener = (obs, oldVal, newVal) -> {
      surfaceList.forEach (surface -> surface.update());
    };
    scene.widthProperty().addListener (listener);
    scene.heightProperty().addListener (listener);

  } // GeoSurfaceHandler

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the context for creating geographic surfaces for the world
   * view.
   *
   * @return the view context.
   */
  public GeoSurfaceViewContext getContext () {

    var context = new GeoSurfaceViewContext();
    context.coordTrans = worldController.coordTransProperty().getValue();
    double radius = worldController.getView().worldRadius();
    context.deltaFunc = (a,b) -> SphereFunctions.delta (a, b, radius);
    context.viewProps = worldController.getView().getProperties();

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
    if (surfaceList.contains (surface)) LOGGER.warning ("Surface already in active list");
    else {

      surface.setFacetConsumer (facet -> worldController.addObject (facet.getNode()));
      surface.setUpdateConsumer (update -> worldController.addSceneGraphChange (update));
      surfaceList.add (surface);
      surface.setCameraPosition (worldController.getView().cameraPositionProperty().get());
      surface.setActive (true);
    
    } // if

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
    if (!surfaceList.contains (surface)) LOGGER.warning ("Surface not in active list");
    else {
  
      surfaceList.remove (surface);

      surface.setActive (false);
      surface.setFacetConsumer (null);
      surface.setUpdateConsumer (null);

      // TODO: In future versions we would selectively clear the facet nodes
      // from the controller that this surface contains.  For now though we
      // simply clear the entire model (we assume that only one surface is active
      // at once).
      worldController.clearObjects();
      
    } // else

  } // deactivateSurface
  
  /////////////////////////////////////////////////////////////////

} // GeoSurfaceHandler class

