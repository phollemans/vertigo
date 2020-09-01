/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.Group;
import javafx.geometry.Point3D;
import javafx.concurrent.Service;

/**
 * The <code>DynamicNode</code> class holds a single child node that can be
 * dynamically replaced at runtime based on the camera position.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class DynamicNode extends Group {

  /** The factory used to update this node. */
  private NodeFactory factory;

  /** The specifications for the currently active child node or null. */
  private Object childSpecs;

  /////////////////////////////////////////////////////////////////

  /**
   * Updates this node based on the camera position.  The update is performed
   * asynchronously.
   *
   * @param cameraPos the new camera position.
   */
  public void update (Point3D cameraPos) {
  
    // We get the specifications for the update needed here, and compare
    // to either what we currently have in the child node, or what is pending
    // from the factory if it's running.
    Object updateSpecs = factory.getSpecsForPosition (cameraPos);
    Object pendingSpecs = factory.getPendingSpecs();

    // If we need a different child object than is currently being created,
    // we cancel the current object creation and start a new one with the
    // camera position needed.  Otherwise, we just start the factory with
    // a new camera position.
    if (!updateSpecs.equals (childSpecs) && !updateSpecs.equals (pendingSpecs)) {
      factory.setCameraPosition (cameraPos);
      if (pendingSpecs != null) factory.restart();
      else factory.start();
    } // if

  } // update

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new object.
   *
   * @param factory the factory to use for creating new nodes to update
   * the child.
   */
  public DynamicNode (NodeFactory factory) {
  
    this.factory = factory;
    
    // When the factory produces a new node, we replace the current child
    // in the zero position and save the child specifications.
    factory.setOnSucceeded (event -> {
      childSpecs = factory.getSpecsForPosition (factory.getCameraPosition());
      List<Node> children = getChildren();
      if (children.size() == 0) children.add (factory.getValue());
      else children.set (0, factory.getValue());
    });
        
  } // DynamicNode

  /////////////////////////////////////////////////////////////////

  } // DynamicNode class
