/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.scene.Node;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;

/**
 * The <code>SceneOctTree</code> class holds and organizes scene graph objects
 * into a spatial tree structure using their bounding boxes and allows for
 * queries against a view frustum.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class SceneOctTree {

  private static final Logger LOGGER = Logger.getLogger (SceneOctTree.class.getName());

  // Constants
  // ---------
  
  /** The maximum number of objects in a node. */
  private static final int MAX_OBJECTS = 8;
  
  /**
   * The maximum tree depth.  A problem occurs in badly behaved scene nodes
   * if we don't limit the maximum depth.  It's possible in insert a number
   * of degenerate nodes of the same size in the same location, and when the
   * tree tries to split, it goes into an infinite loop and causes a stack
   * overflow.  So for the bad apples, we have to limit the tree depth.
   */
  private static final int MAX_DEPTH = 8;

  // Variables
  // ---------

  /** The spatial bounds of this node in the tree. */
  private Bounds bounds;

  /** The depth of this tree from the root, starting at zero for the root. */
  private int depth;

  /**
   * The list of scene objects whose center points are contained within
   * this node, or null for none.  This is limited to the maximum number of
   * objects in a node.  We consider these nodes first class citizens of this
   * tree for the purposes of the splitting condition.
   */
  private List<Node> firstClassList;

  /**
   * The list of scene objects whose bounds intersect this node but whose
   * center points are somewhere else, or null for none.  This is unlimited
   * in length.  We consider these nodes second class citizens of this tree
   * and we carry them, but don't use them for the splitting condition.
   */
  private List<Node> secondClassList;

  /** The children of this node, or null for none (ie: the node is a leaf). */
  private SceneOctTree[] children;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new empty tree with a specified depth.
   *
   * @param bounds the global bounds for the tree.
   * @param depth the depth of the tree.
   */
  private SceneOctTree (
    Bounds bounds,
    int depth
  ) {

    this.bounds = bounds;
    this.depth = depth;

  } // SceneOctTree

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the bounds of this tree.
   *
   * @return the tree bounds.
   */
  public Bounds getBounds () { return (bounds); }
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new empty tree.
   *
   * @param bounds the global bounds for the tree.
   */
  public SceneOctTree (
    Bounds bounds
  ) {

    this.bounds = bounds;

  } // SceneOctTree

  /////////////////////////////////////////////////////////////////

  /**
   * Inserts the object as a first class citizen of this tree.
   * See {@link #insert(Node, Bounds, double, double, double)}.
   */
  private void insertFirstClass (
    Node object,
    Bounds objectBounds,
    double centerX,
    double centerY,
    double centerZ
  ) {

    // Base case -- put the object into this node and split
    if (children == null) {
      if (firstClassList == null) firstClassList = new ArrayList<>();
      firstClassList.add (object);
      split();
    } // if

    // Recursive case -- put the object into one or more children
    else {
      for (SceneOctTree child : children) {
        if (objectBounds.intersects (child.bounds))
          child.insert (object, objectBounds, centerX, centerY, centerZ);
      } // for
    } // else

  } // insertFirstClass

  /////////////////////////////////////////////////////////////////

  /**
   * Inserts the object as a second class citizen of this tree.
   * See {@link #insert(Node, Bounds, double, double, double)}.
   */
  private void insertSecondClass (
    Node object,
    Bounds objectBounds
  ) {

    // Base case -- put the object into this node.  We don't need to split
    // afterwards because this object won't contribute to the split condition.
    if (children == null) {
      if (secondClassList == null) secondClassList = new ArrayList<>();
      secondClassList.add (object);
    } // if

    // Recursive case -- put the object into one or more children.  We can
    // insert as second class here because we know that if the center point is
    // outside this tree, then it's outside all the children as well.
    else {
      for (SceneOctTree child : children) {
        if (objectBounds.intersects (child.bounds))
          child.insertSecondClass (object, objectBounds);
      } // for
    } // else

  } // insertSecondClass

  /////////////////////////////////////////////////////////////////

  /**
   * Inserts an object into the tree.
   *
   * @param object the object to insert.
   * @param objectBounds the object bounds to use for insertion.
   * @param centerX the bounds center point X coordinate.
   * @param centerY the bounds center point Y coordinate.
   * @param centerZ the bounds center point Z coordinate.
   */
  private void insert (
    Node object,
    Bounds objectBounds,
    double centerX,
    double centerY,
    double centerZ
  ) {

    if (this.bounds.contains (centerX, centerY, centerZ))
      insertFirstClass (object, objectBounds, centerX, centerY, centerZ);
    else if (this.bounds.intersects (objectBounds))
      insertSecondClass (object, objectBounds);
    else throw new IllegalArgumentException ("Object does not belong in this tree");
  
  } // insert

  /////////////////////////////////////////////////////////////////

  /**
   * Inserts an object into the tree.
   *
   * @param object the object to insert.
   */
  public void insert (
    Node object
  ) {

    Bounds objectBounds = object.getBoundsInLocal();

    if (LOGGER.isLoggable (Level.FINER)) {
      LOGGER.finer ("Inserting object with center (x,y,z) = " +
        objectBounds.getCenterX() + "," +
        objectBounds.getCenterY() + "," +
        objectBounds.getCenterZ() +
        " and (w,h,d) = " +
        objectBounds.getWidth() + "," +
        objectBounds.getHeight() + "," +
        objectBounds.getDepth());
    } // if

    insert (object, objectBounds, objectBounds.getCenterX(),
      objectBounds.getCenterY(), objectBounds.getCenterZ());

  } // insert
  
  /////////////////////////////////////////////////////////////////
  
  /**
   * Check if this tree has first class citizens and if the splitting condition
   * is met, split into children and insert all the objects into the children.
   */
  private void split () {

    if (firstClassList != null && firstClassList.size() > MAX_OBJECTS && depth < MAX_DEPTH) {

      // Set new bounds along each axis
      double[] xBounds = new double[] {bounds.getMinX(), bounds.getCenterX(), bounds.getMaxX()};
      double[] yBounds = new double[] {bounds.getMinY(), bounds.getCenterY(), bounds.getMaxY()};
      double[] zBounds = new double[] {bounds.getMinZ(), bounds.getCenterZ(), bounds.getMaxZ()};

      // Create each child
      children = new SceneOctTree[8];
      for (int x = 0; x <= 1; x++) {
        for (int y = 0; y <= 1; y++) {
          for (int z = 0; z <= 1; z++) {
            Bounds childBounds = new BoundingBox (
              xBounds[x], yBounds[y], zBounds[z],
              xBounds[x+1] - xBounds[x], yBounds[y+1] - yBounds[y], zBounds[z+1] - zBounds[z]
            );
            int index = x + y*2 + z*4;
            children[index] = new SceneOctTree (childBounds, this.depth+1);
          } // for
        } // for
      } // for

      // Insert first class objects into children.  Note that we don't know
      // if the object will be first or second class in the child so we have
      // to perform a full insert.
      for (Node object : firstClassList) {
        Bounds objectBounds = object.getBoundsInLocal();
        for (SceneOctTree child : children) {
          if (objectBounds.intersects (child.bounds))
            child.insert (object);
        } // for
      } // for
      firstClassList = null;

      // Insert second class objects into children (if any exist).  We know
      // that the object will be second class in the child, so we just
      // perform a second class insert.
      if (secondClassList != null) {
        for (Node object : secondClassList) {
          Bounds objectBounds = object.getBoundsInLocal();
          for (SceneOctTree child : children) {
            if (objectBounds.intersects (child.bounds))
              child.insertSecondClass (object, objectBounds);
          } // for
        } // for
      } // if
      secondClassList = null;

      // Perform a split on the children, just in case all the first class
      // objects that we just inserted all went into one child.
      for (SceneOctTree child : children) child.split();

    } // if

  } // split

  /////////////////////////////////////////////////////////////////

  /**
   * Finds the objects in this tree whose bounds are visible within the view
   * frustum.
   *
   * @param frustum the frustum to use for bounds checking.
   * @param objectsFound the output set of objects found (modified).
   */
  public void findVisible (
    Frustum frustum,
    Set<Node> objectsFound
  ) {

    // First check if this is a leaf node with no objects in it.  In that
    // case there's nothing to do, and we don't even want to perform the
    // frustum check.
    if (children == null && firstClassList == null && secondClassList == null) return;

    // Now do the frustum intersection check and if so, go deeper and look
    // for objects
    if (frustum.intersects (this.bounds)) {
      
      // Base case -- check the individual objects here against the frustum.
      // We try to eliminate multiple frustum intersection queries by checking
      // if the object has already been found.
      if (children == null) {

        if (firstClassList != null) {
          for (Node object : firstClassList) {
            if (!objectsFound.contains (object)) {
              if (frustum.intersects (object.getBoundsInLocal())) objectsFound.add (object);
            } // if
          } // for
        } // if

        if (secondClassList != null) {
          for (Node object : secondClassList) {
            if (!objectsFound.contains (object)) {
              if (frustum.intersects (object.getBoundsInLocal())) objectsFound.add (object);
            } // if
          } // for
        } // if

      } // if

      // Recursive case -- check the children
      else {
        for (SceneOctTree child : children) {
          child.findVisible (frustum, objectsFound);
        } // for
      } // else
    
    } // if

  } // findVisible
  
  /////////////////////////////////////////////////////////////////

  /** Visits a tree for the purposes of gathering statistics. */
  private static class TreeNodeVisitor {
  
    private int firstClassCount;
    private int secondClassCount;
    private int maxDepth;
    private int nodeCount;
    private int leafCount;
    private int emptyLeafCount;

    public void visit (SceneOctTree node) {
      if (node.firstClassList != null) firstClassCount += node.firstClassList.size();
      if (node.secondClassList != null) secondClassCount += node.secondClassList.size();
      if (node.children == null) leafCount++;
      nodeCount++;
      if (node.children == null && node.firstClassList == null && node.secondClassList == null)
        emptyLeafCount++;
      if (node.depth > maxDepth) maxDepth = node.depth;          
    } // visit
    
    public void log () {
      LOGGER.finest (
        "Tree structure summary:\n" +
        "  First class object count = " + firstClassCount + "\n" +
        "  Second class object count = " + secondClassCount + "\n" +
        "  Maximum tree depth = " + maxDepth + "\n" +
        "  Total tree nodes = " + nodeCount + "\n" +
        "  Total leaf nodes = " + leafCount + "\n" +
        "  Empty leaf count = " + emptyLeafCount + "\n" +
        "  Avg first class per non-empty leaf = " + ((float) firstClassCount/(leafCount - emptyLeafCount)) + "\n" +
        "  Avg second class per non-empty leaf = " + ((float) secondClassCount/(leafCount - emptyLeafCount))
      );
    } // log
  
  } // TreeNodeVisitor

  /////////////////////////////////////////////////////////////////

  /** Performs a traversal of this tree with the visitor. */
  private void traversal (
    TreeNodeVisitor visitor
  ) {

    visitor.visit (this);
    if (children != null) {
      for (SceneOctTree child : children) child.traversal (visitor);
    } // if

  } // traversal

  /////////////////////////////////////////////////////////////////

  /** Summarizes the statistics of this tree to the logging system. */
  public void summarize () {

    TreeNodeVisitor visitor = new TreeNodeVisitor();
    traversal (visitor);
    visitor.log();
  
  } // summarize

  /////////////////////////////////////////////////////////////////

  // TODO: We should also have remove() and merge() functions here.

} // SceneOctTree class
