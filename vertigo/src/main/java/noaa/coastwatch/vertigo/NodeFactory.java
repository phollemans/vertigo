/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.logging.Logger;

import javafx.scene.Node;
import javafx.concurrent.Service;
import javafx.geometry.Point3D;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The <code>NodeFactory</code> class creates new node in an
 * asynchronous service based on a camera position.  To create a new node,
 * use the {@link #setCameraPosition} method to set the camera position, then
 * call the {@link Service#start} method.  Implementing classes should override the
 * {@link #getSpecsForPosition} method to create specification objects, and
 * the {@link Service#createTask} method to create a task object based on the
 * camera position property.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public abstract class NodeFactory extends Service<Node> {

  private static final Logger LOGGER = Logger.getLogger (NodeFactory.class.getName());

  /** The specifications for the node currently being created. */
  private Object pendingSpecs;

  /** The camera position property. */
  private ObjectProperty<Point3D> cameraPosition = new SimpleObjectProperty<> (this, "cameraPosition");
  public final void setCameraPosition (Point3D value) { cameraPosition.set (value); }
  public final Point3D getCameraPosition() { return (cameraPosition.get()); }
  public final ObjectProperty<Point3D> cameraPositionProperty() { return (cameraPosition); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the specifications object needed for creating nodes for a camera
   * position.  The specifications object may be used to determine if a
   * currently running factory will be producing the required specifications
   * by comparing the returned value from {@link #getPendingSpecs} to the value
   * returned here using the equals() method.
   *
   * @param pos the camera position to compute specification for.
   *
   * @return the node specifications needed for the camera position.
   */
  public abstract Object getSpecsForPosition (Point3D pos);

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the specifications for the node currently being produced by the
   * factory.
   *
   * @return the pending specs, or null if the factory is not in a ready,
   * running, or scheduled state.
   */
  public Object getPendingSpecs () {

    Object specs = null;
    switch (getState()) {
    case READY:
    case RUNNING:
    case SCHEDULED:
      specs = pendingSpecs;
      break;
    } // switch

    return (specs);
    
  } // getPendingSpecs

  /////////////////////////////////////////////////////////////////

  @Override
  protected void failed() {

    LOGGER.warning ("Node creation failed: " + getException().getMessage());

  } // failed

  /////////////////////////////////////////////////////////////////

} // NodeFactory class
