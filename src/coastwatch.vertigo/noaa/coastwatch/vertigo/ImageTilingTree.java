/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.function.ToDoubleFunction;

/**
 * The <code>ImageTilingTree</code> class represents a hierarchical tiling
 * of an image such that individual tiles are no larger in width and height
 * than a specified maximum value.  The size of image tiles is determined
 * using a function that maps pairs of image coordinates to a distance metric.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ImageTilingTree {

  private static final Logger LOGGER = Logger.getLogger (ImageTilingTree.class.getName());

  private static final int X = 0;
  private static final int Y = 1;
  
  // Variables
  // ---------

  /** The image bounds of this node in the tree. */
  private int[] start, end;

  /** The depth of this tree from the root, starting at zero for the root. */
  private int depth;

  /** The children of this node, or null for none (ie: the node is a leaf). */
  private ImageTilingTree[] children;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new tiling with the specified bounds and depth.
   *
   * @param start the minimum image coordinates as [x, y].
   * @param end the maximum image coordinates as [x, y].
   * @param depth the depth of the tree.
   * @param distMetric the function mapping point pairs to distance.
   * @param maxDist the maximum allowed distance.
   */
  private ImageTilingTree (
    int[] start,
    int[] end,
    int depth,
    ToDoubleFunction<int[]> distMetric,
    double maxDist
  ) {

    this.start = start;
    this.end = end;
    this.depth = depth;

    // Compute the width and height of the bounds according to the metric
    double width = distMetric.applyAsDouble (new int[] {
      start[X], (start[Y] + end[Y])/2, end[X], (start[Y] + end[Y])/2
    });


//System.out.println ("width = " + width);
if (Double.isNaN (width)) throw new RuntimeException();


    double height = distMetric.applyAsDouble (new int[] {
      (start[X] + end[X])/2, start[Y], (start[X] + end[X])/2, end[Y]
    });


//System.out.println ("height = " + height);
if (Double.isNaN (height)) throw new RuntimeException();



    // This is the recursive step.  If the size function says we're over the
    // limit in either width or height, we split.






    if (Math.max (width, height) > maxDist) {

      double aspect = width / height;
      int[] stepSize;
      int[] tileSize;
      int childCount;
      
      int imageWidth = end[X] - start[X] + 1;
      int imageHeight = end[Y] - start[Y] + 1;

      // Split along the X direction
      if (aspect >= 1) {
        childCount = Math.max ((int) Math.round (aspect), 2);
//        childCount = 2;
        tileSize = new int[] {imageWidth / childCount, imageHeight};
        stepSize = new int[] {tileSize[X], 0};
      } // if

      // Split along the Y direction
      else {
        childCount = Math.max ((int) Math.round (1.0/aspect), 2);
//        childCount = 2;
        tileSize = new int[] {imageWidth, imageHeight / childCount};
        stepSize = new int[] {0, tileSize[Y]};
      } // else


//System.out.println ("childCount = " + childCount);



      // Create the children and recursively create trees at each child
      children = new ImageTilingTree[childCount];
      for (int i = 0; i < childCount; i++) {
        int[] childStart = new int[] {start[X] + i*stepSize[X], start[Y] + i*stepSize[Y]};
        int[] childEnd;
        if (i == childCount-1)
          childEnd = new int[] {end[X], end[Y]};
        else
          childEnd = new int[] {childStart[X] + tileSize[X] - 1, childStart[Y] + tileSize[Y] - 1};
        children[i] = new ImageTilingTree (childStart, childEnd, depth+1, distMetric, maxDist);
      } // for
    
    } // if

  } // ImageTilingTree

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new tiling with the specified bounds and depth.
   *
   * @param start the minimum image coordinates as [x, y].
   * @param end the maximum image coordinates as [x, y].
   * @param distMetric the function mapping point pairs to distance.
   * @param maxDist the maximum allowed distance.
   */
  public ImageTilingTree (
    int[] start,
    int[] end,
    ToDoubleFunction<int[]> distMetric,
    double maxDist
  ) {

    this (start, end, 0, distMetric, maxDist);

  } // ImageTilingTree

  /////////////////////////////////////////////////////////////////

  public int[] getStart() { return (start); }

  /////////////////////////////////////////////////////////////////

  public int[] getEnd() { return (end); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the leaf nodes in the tree.  The leaf nodes are the actual
   * nodes that satisfy the distance metric restriction specified when
   * constructing the tree.
   *
   * @return the leaf nodes.
   */
  public List<ImageTilingTree> getLeafNodes () {
  
    LeafVisitor visitor = new LeafVisitor();
    traversal (visitor);
    
    return (visitor.leafNodeList);
  
  } // getLeafNodes

  /////////////////////////////////////////////////////////////////

  /** Visits a tree for the purpose of gathering leaf nodes. */
  private static class LeafVisitor implements TreeNodeVisitor {
  
    public List<ImageTilingTree> leafNodeList = new ArrayList<>();

    public void visit (ImageTilingTree node) {
      if (node.children == null) leafNodeList.add (node);
    } // visit
    
  } // StatsVisitor

  /////////////////////////////////////////////////////////////////

  /** Interface for visitors to a tree node. */
  private interface TreeNodeVisitor {

    void visit (ImageTilingTree node);

  } // TreeNodeVisitor

  /////////////////////////////////////////////////////////////////

  /** Visits a tree for the purpose of gathering statistics. */
  private static class StatsVisitor implements TreeNodeVisitor {
  
    private int maxDepth;
    private int nodeCount;
    private int leafCount;

    public void visit (ImageTilingTree node) {
      nodeCount++;
      if (node.children == null) leafCount++;
      if (node.depth > maxDepth) maxDepth = node.depth;
    } // visit
    
    public void log () {
      LOGGER.fine ("Tree structure summary:");
      LOGGER.fine ("  Maximum tree depth = " + maxDepth);
      LOGGER.fine ("  Total tree nodes = " + nodeCount);
      LOGGER.fine ("  Total leaf nodes = " + leafCount);
    } // log
  
  } // StatsVisitor class

  /////////////////////////////////////////////////////////////////

  /** Performs a traversal of this tree with the visitor. */
  private void traversal (
    TreeNodeVisitor visitor
  ) {

    visitor.visit (this);
    if (children != null) {
      for (ImageTilingTree child : children) child.traversal (visitor);
    } // if

  } // traversal

  /////////////////////////////////////////////////////////////////

  /** Summarizes the statistics of this tree to the logging system. */
  public void summarize () {

    StatsVisitor visitor = new StatsVisitor();
    traversal (visitor);
    visitor.log();
  
  } // summarize

  /////////////////////////////////////////////////////////////////

} // ImageTilingTree class

