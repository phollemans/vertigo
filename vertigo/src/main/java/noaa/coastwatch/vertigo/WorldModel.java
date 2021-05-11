/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

import javafx.scene.Node;
import javafx.geometry.Bounds;

/**
 * The <code>WorldModel</code> class is the main model component of the MVC
 * for Vertigo.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WorldModel {

  // Variables
  // ---------

  /** The set of objects in this model. */
  private Set<Node> objectSet;
  
  /** The spatial tree for managing object visibility. */
  private SceneOctTree objectTree;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new model object.
   *
   * @param bounds the bounds enclosing all objects that will be inserted
   * into the model.
   */
  public WorldModel (
    Bounds bounds
  ) {

    objectSet = new HashSet<>();
    objectTree = new SceneOctTree (bounds);

  } // WorldModel

  /////////////////////////////////////////////////////////////////

  /** Clears all objects from the model. */
  public void clear() {
  
    objectSet.clear();
    objectTree = new SceneOctTree (objectTree.getBounds());
  
  } // clear

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an unmodifiable view of the set of objects in this model.
   *
   * @return the unmodifiable object set.
   */
  public Set<Node> getObjects() { return (Collections.unmodifiableSet (objectSet)); }

  /////////////////////////////////////////////////////////////////

  /**
   * Adds an object to the model.
   *
   * @param object the object to add.
   */
  public void addObject (Node object) {

    objectSet.add (object);
    objectTree.insert (object);

  } // addObject

  /////////////////////////////////////////////////////////////////

  /**
   * Finds the objects in the model whose bounds are visible within the
   * specified view frustum.
   *
   * @param frustum the frustum to use for bounds checking.
   *
   * @return the output set of objects found.
   */
  public Set<Node> findVisible (
    Frustum frustum
  ) {

    HashSet<Node> visibleObjects = new HashSet<>();
    objectTree.findVisible (frustum, visibleObjects);
    
    return (visibleObjects);

  } // getVisible

  /////////////////////////////////////////////////////////////////

  /**
   * Updates the visibility to true for a specific set of objects, making all
   * other objects in the model invisible.
   *
   * @param visibleObjects the objects that should be made visible.
   */
  public void updateVisibility (
    Set<Node> visibleObjects
  ) {

    for (Node node : objectSet) {
      node.setVisible (visibleObjects.contains (node));
    } // for
      
  } // updateVisibility

  /////////////////////////////////////////////////////////////////

  /** Summarizes the statistics of the model to the logging system. */
  public void summarize () {

    objectTree.summarize();
  
  } // summarize

  /////////////////////////////////////////////////////////////////

} // WorldModel class
