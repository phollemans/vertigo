/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.function.Consumer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javafx.geometry.Point3D;
import javafx.geometry.Bounds;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.image.Image;
import javafx.scene.shape.MeshView;
import javafx.scene.paint.PhongMaterial;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

import javafx.application.Platform;

/**
 * The <code>DynamicSurface</code> class manages a set of surface facets that
 * update and change depending on the position of the camera.  The facets start
 * out uninitialized with nothing to view.  When the camera position property
 * is set for the first time and the surface set to active mode, the facets are
 * generated in a background process and then pushed to a consumer function
 * supplied from the caller.  Subsequent updates to the facets will take the
 * form of an update to the facet mesh view.
 *
 * @see Facet
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class DynamicSurface {

  private static final Logger LOGGER = Logger.getLogger (DynamicSurface.class.getName());

  // Variables
  // ---------

  /** The source used for creating facets. */
  private FacetDataSource source;

  /** The list of facets being initialized but not yet fully created. */
  private List<Facet> facetInitList;

  /** The list of facets finished being created, to be used when updating. */
  private List<Facet> facetList;

  /** The camera position property. */
  private ObjectProperty<Point3D> cameraPositionProp = new SimpleObjectProperty<> (this, "cameraPosition");
  public final void setCameraPosition (Point3D value) { cameraPositionProp.set (value); }
  public final Point3D getCameraPosition() { return (cameraPositionProp.get()); }
  public final ObjectProperty<Point3D> cameraPositionProperty() { return (cameraPositionProp); }

  /** The consumer called when a facet transitions to having a mesh view. */
  private Consumer<Facet> facetConsumer;

  /** The consumer called when a facet has an update to the scene graph. */
  private Consumer<Runnable> updateConsumer;

  /** The thread for running the time consuming part of the update. */
  private Thread updateThread;

  /**
   * The flag for when the facets are initializing.  Since part of the
   * initialization is in a background thread, we don't want to start a new
   * initialization run, so this is marked true when we've started the
   * initialize() method.
   */
  private boolean initializeCalled;

  /** The listener for camera change events. */
  private ChangeListener<Point3D> cameraListener;

  /**
   * The active flag, true if we are listening for camera position updates
   * or false if not.
   */
  private boolean isActive;

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the consumer for facets that are ready to be viewed.
   *
   * @param consumer the facet consumer to use.
   */
  public void setFacetConsumer (
    Consumer<Facet> consumer
  ) {

    facetConsumer = consumer;
  
    // As soon as the facet consumer is set, we send any facets that have
    // already been initialized to the consumer, so that they can be shown
    // right away.
    if (facetConsumer != null) {
      for (Facet facet : facetList) facetConsumer.accept (facet);
    } // if
  
  } // setFacetConsumer

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the consumer for updates to the scene graph.  If no consumer is
   * ever set, updates to the scene graph will be performed at the facet level
   * when they become available.
   *
   * @param consumer the update consumer to use.
   */
  public void setUpdateConsumer (
    Consumer<Runnable> consumer
  ) {

    updateConsumer = consumer;

    // As soon as the update consumer is set, we reset the consumer in all
    // of the facets.
    for (Facet facet : facetInitList) facet.setUpdateConsumer (updateConsumer);
    for (Facet facet : facetList) facet.setUpdateConsumer (updateConsumer);
  
  } // setUpdateConsumer

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new dynamic surface.
   *
   * @param source the source to use for facet data.
   */
  public DynamicSurface (
    FacetDataSource source
  ) {

    this.source = source;
    
    // Initialize by creating the facet list properties.  Actual facet
    // instances aren't created and added yet until a camera position
    // is set and acrtive mode is on.
    facetInitList = new ArrayList<>();
    facetList = new ArrayList<>();
      
    // Listen for changes in the camera position and update the
    // facets as needed
    cameraListener = (obs, oldVal, newVal) -> update();
    
  } // DynamicSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the active mode, which is off by default.  When the surface is
   * active, it updates the facets in response to setting the camera position
   * property.  When not active, no updates occur.  When the state
   * transitions from inactive to active, the surface is initialized
   * (if not already) and the current camera position is sent to the facets
   * for an update.  When the state transitions from active to inactive, the
   * facets being updated are stopped.
   */
  public void setActive (boolean flag) {

    if (isActive != flag) {

      isActive = flag;

      if (isActive) {
        cameraPositionProp.addListener (cameraListener);
        update();
      } // if

      else {
        cameraPositionProp.removeListener (cameraListener);
        for (Facet facet : facetInitList) facet.stopUpdate();
        for (Facet facet : facetList) facet.stopUpdate();
      } // else
  
    } // if
  
  } // setActive

  /////////////////////////////////////////////////////////////////

  /** Initializes the surface facets with their first node values. */
  private void initialize () {
  
    // Make sure we aren't called more than once.  This can happen if a camera
    // position change occurs before all the facets are done being generated.
    if (initializeCalled) return;
    initializeCalled = true;

    // We don't know the facet position yet so we can't determine what level
    // of texture or mesh to use.  We create them using the lowest density mesh
    // and no texture.  We wait for them to be created and then add them to the
    // official facet list.  They need to have a node first before we can pass
    // them back to the caller, because the caller expects to know where to
    // place each facet.  Once all the facets have a node, we call update
    // again.
    int facetCount = source.getFacets();
    LOGGER.fine ("Creating initial set of " + facetCount + " facets at lowest mesh resolution");

    int lowestMeshLevel = source.getMeshFactory().getLevels()-1;
    for (int i = 0; i < facetCount; i++) {
      Facet facet = new Facet (source, i);
      facetInitList.add (facet);
      facet.setUpdateConsumer (updateConsumer);
      facet.nodeProperty().addListener ((obs, oldVal, newVal) -> {
        if (oldVal == null && newVal != null) {
          LOGGER.finest ("Adding new facet " + facet.getIndex() + " to the list");
          facetInitList.remove (facet);
          facetList.add (facet);
          if (facetConsumer != null) facetConsumer.accept (facet);
        } // if
        if (facetList.size() == facetCount) {
          LOGGER.fine ("Initialization of " + facetCount + " facets is complete");
          update();
        } // if
      });
      facet.update (lowestMeshLevel, -1);
    } // for
  
  } // initialize

  /////////////////////////////////////////////////////////////////

  /** Updates the surface facets using the current camera position property. */
  private void update () {

    if (cameraPositionProp.get() == null) {
      LOGGER.warning ("Update called with no camera position set");
      return;
    } // if
    
    // Check if we are creating facets for the first time.  If so we need
    // to initialize first.  Then we'll be send back to this point after
    // initialization.
    if (facetList.size() == 0) initialize();

    // If for some reason update gets called, and we don't have a full facet
    // list, we don't do anything, assuming there are facets that are still
    // being created.  However if we have all facets created with a node,
    // we can start an actual update.
    else if (facetList.size() == source.getFacets()) {

      // We start the update process only if another is not already running.
      // This allows for us to coalesce multiple camera position updates into
      // one if the process of finding all the facets to update takes a longer
      // time than between camera updates.
      if (updateThread == null) {
        updateThread = new Thread (() -> updateInBackground (cameraPositionProp.get()));
        updateThread.start();
      } // if
      
    } // if

  } // update

  /////////////////////////////////////////////////////////////////

  /**
   * Updates this surface in a background thread using the specified
   * camera position.  When operations are sent to the JavaFX thread, we check
   * if we are still active before performing any updates to facets.
   *
   * @param newCameraPos the new camera position to use.
   */
  private void updateInBackground (
    Point3D newCameraPos
  ) {

    // Look for the minimum distance from the camera to any of the facets,
    // and use that distance for determining the levels of detail needed.
    // Currently we have no algorithm for facets to be at different
    // texture and mesh levels at the same time, so this is the solution for
    // now.  We sort the facets by distance so that the closest facets get
    // an update request first.
    double minDist = Double.MAX_VALUE;
    Map<Facet, Double> facetUpdateMap = new HashMap<>();
    List<Facet> stopList = new ArrayList<>();
    for (Facet facet : facetList) {
      if (facet.getNode().isVisible()) {
        double dist2 = dist2 (newCameraPos, facet.getCenter());
        if (dist2 < minDist) minDist = dist2;
        facetUpdateMap.put (facet, dist2);
      } // if
      else {
        stopList.add (facet);
      } // else
    } // for
    minDist = Math.sqrt (minDist);

    int meshLevel = source.getMeshFactory().getLevelForDist (minDist);
    int textureLevel = source.getTextureFactory().getLevelForDist (minDist);

    // Go through the list of facets and sort by distance, then create a list
    // to update.
    List<Entry<Facet, Double>> entryList = new ArrayList<> (facetUpdateMap.entrySet());
    entryList.sort (Entry.comparingByValue());

    List<Facet> updateList = new ArrayList<>();
    entryList.stream().map (entry -> entry.getKey()).forEach (facet -> {
      if (!facet.matches (meshLevel, textureLevel)) {
        updateList.add (facet);
      } // if
    });

    // Stop updating any facets that are not currently visible.
    Platform.runLater (() -> {
      if (isActive) {
        for (Facet facet : stopList) facet.stopUpdate();
      } // if
    });

    // If there are some facets to update, submit the list on the JavaFX
    // application thread.
    if (updateList.size() != 0) {
      LOGGER.finer ("Updating for facet distance " + minDist + ", (mesh,texture) = " + meshLevel + "," + textureLevel);
      LOGGER.finer (updateList.size() + " facet update(s) needed");
      Platform.runLater (() -> {
        if (isActive) {
          for (Facet facet : updateList) facet.update (meshLevel, textureLevel);
        } // if
      });
    } // if
    
    // Check if the camera position has changed during the time we've been
    // running this method, and launch a new update if so.  Also make sure to
    // null the update thread so a new one can run.
    Platform.runLater (() -> {
      updateThread = null;
      if (isActive) {
        if (!newCameraPos.equals (cameraPositionProp.get())) update();
      } // if
    });
  
  } // updateInBackground

  /////////////////////////////////////////////////////////////////

  /** Computes the distance^2 between points. */
  private double dist2 (Point3D a, Point3D b) {
  
    double dx = a.getX() - b.getX();
    double dy = a.getY() - b.getY();
    double dz = a.getZ() - b.getZ();
    return (dx*dx + dy*dy + dz*dz);

  } // dist2

  /////////////////////////////////////////////////////////////////

} // DynamicSurface class
