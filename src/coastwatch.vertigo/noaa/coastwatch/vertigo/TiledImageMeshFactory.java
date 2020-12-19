/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;

import java.util.HashMap;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.BooleanSupplier;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.ObservableFaceArray;
import javafx.scene.shape.VertexFormat;
import javafx.collections.ObservableFloatArray;
import javafx.geometry.Point3D;

import static noaa.coastwatch.vertigo.Helpers.isTrue;
import static noaa.coastwatch.vertigo.SphereFunctions.THETA;
import static noaa.coastwatch.vertigo.SphereFunctions.PHI;

/**
 * The <code>TiledImageMeshFactory</code> class creates level of detail
 * triangle meshes using an image coordinate source to extract coordinates
 * from and an image tiling.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class TiledImageMeshFactory implements MeshFactory {

  private static final Logger LOGGER = Logger.getLogger (TiledImageMeshFactory.class.getName());

  // Constants
  // ---------
  
  private static final int X = 0;
  private static final int Y = 1;
  private static final int Z = 2;

  // Variables
  // ---------

  /** The image tiling to use for individual tiles. */
  private ImageTiling tiling;

  /** The minimum camera distance for each level of mesh detail. */
  private double[] dmin;

  /** The number of points along each edge of a mesh at maximum resolution. */
  private int fullResPoints;

  /** The array of all point data for meshes created by this factory. */
  private float[] pointData;

  /** The number of mesh points in the x direction in the point data array. */
  private int xPoints;

  /** The number of mesh points in the y direction in the point data array. */
  private int yPoints;

  /** The map of tile index to mesh descriptor. */
  private HashMap<Integer, TileMeshDescriptor> meshDescriptorMap;

  /** The winding order counter-clockwise flag.*/
  private boolean windingOrderCounterClock;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new mesh factory.
   *
   * @param tiling the image tiling to use for individual tiles.  The tile
   * indices within the tiling should be passed as the index values in the
   * {@link #create} method. The tiling must be such that each tile is made
   * up of an integer number of the highest resolution mesh divisions
   * in both the x and y directions.
   * @param coordSource the source to used for mesh point coordinates.
   * @param dmin the array of minimum camera distance values for each level
   * of mesh detail for use in the {@link #getLevelForDist} method.  The
   * level will be selected for a given distance such that dmin[level] is
   * less than the camera distance and dmin[level+1] is greater than the camera
   * distance, as much as possible (until the distance becomes too small and
   * no mesh with a higher level of detail is available).
   */
  public TiledImageMeshFactory (
    ImageTiling tiling,
    ImageCoordinateSource coordSource,
    double[] dmin
  ) throws IOException {

    this.tiling = tiling;
    this.dmin = dmin;

    // Calculate the pixel spacing the in x and y directions.  We need to
    // access coordinates with this pixel spacing.  The number of mesh divisions
    // in each tile in both the x and y direction is determined by the number
    // of levels: divs = 2^(levels - 1).
    int meshDivs = 1 << (dmin.length - 1);
    fullResPoints = meshDivs + 1;
    int xPixels = tiling.tileWidth / meshDivs;
    int yPixels = tiling.tileHeight / meshDivs;

    // First we need to get the coordinates around the edges of the image
    // so it can be used for extrapolation to the very edges.  We perform
    // four accesses, one for each side.
    ImageTile tileTop = new ImageTile (0, 0, tiling.width, 2);
    ImageAccess accessTop = new ImageAccess (tileTop, 1, 1);
    ImageAccessResult resultTop = coordSource.access (accessTop, null);
    
    ImageTile tileBottom = new ImageTile (0, tiling.height-2, tiling.width, 2);
    ImageAccess accessBottom = new ImageAccess (tileBottom, 1, 1);
    ImageAccessResult resultBottom = coordSource.access (accessBottom, null);
    
    ImageTile tileLeft = new ImageTile (0, 0, 2, tiling.height);
    ImageAccess accessLeft = new ImageAccess (tileLeft, 1, 1);
    ImageAccessResult resultLeft = coordSource.access (accessLeft, null);

    ImageTile tileRight = new ImageTile (tiling.width-2, 0, 2, tiling.height);
    ImageAccess accessRight = new ImageAccess (tileRight, 1, 1);
    ImageAccessResult resultRight = coordSource.access (accessRight, null);

    // Next we get the coordinates for the inner points.  We do this using
    // two strided accesses for pixels that are diagonally opposite one another
    // across the pixel corner that we need.
    ImageTile tileInner = new ImageTile (xPixels-1, yPixels-1,
      tiling.width-(xPixels-1), tiling.height-(yPixels-1));
    ImageAccess accessInner = new ImageAccess (tileInner, xPixels, yPixels);
    ImageAccessResult resultInner = coordSource.access (accessInner, null);

    ImageTile tileInnerBR = new ImageTile (xPixels, yPixels,
      tiling.width-xPixels, tiling.height-yPixels);
    ImageAccess accessInnerBR = new ImageAccess (tileInnerBR, xPixels, yPixels);
    ImageAccessResult resultInnerBR = coordSource.access (accessInnerBR, null);

    // Now we loop over each division and compute the coordinates of the
    // corners.  There are a number of cells in the x and y directions, and
    // points that bracket the cells, so points = cells + 1.
    int xCells = tiling.width / xPixels;
    if (xCells*xPixels < tiling.width) xCells++;
    int yCells = tiling.height / yPixels;
    if (yCells*yPixels < tiling.height) yCells++;
    xPoints = xCells + 1;
    yPoints = yCells + 1;

    // We use model space vectors to extrapolate to the corners and edges
    // of the image data, and also to find the intersections in the inner
    // region points.  Every interpolation/extrapolation operation is based
    // on four points, depending on where in the image data we're working:
    //
    // +-----------------------------------+
    // |p1|p2|  |  |  |p1|p2|  |  |  |p1|p2|
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |p3|  |  |  |  |p3|p4|  |  |  |  |p4|
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |  |  |  |  |  |  |  |  |  |  |  |  |
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |  |  |  |  |  |  |  |  |  |  |  |  |
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |p1|p2|  |  |  |p1|  |  |  |  |p1|p2|
    // +--+--+--+--+--+--+--+--+--+--+--+--|  <-- Imagine a much larger inner
    // |p3|p4|  |  |  |  |p4|  |  |  |p3|p4|      region.  Most points will
    // +--+--+--+--+--+--+--+--+--+--+--+--|      probably be inner points.
    // |  |  |  |  |  |  |  |  |  |  |  |  |
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |  |  |  |  |  |  |  |  |  |  |  |  |
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |p1|  |  |  |  |p1|p2|  |  |  |  |p2|
    // +--+--+--+--+--+--+--+--+--+--+--+--|
    // |p3|p4|  |  |  |p3|p4|  |  |  |p3|p4|
    // +-----------------------------------+
    //
    // In the case of corners and edges, we access the two rows and columns
    // of data around the outside of the image, and use three points for
    // corners and four points for edge extrapolation.  In the case of
    // interior points, we access strided data over the inner region and
    // use two points to interpolate the pixel corner between them.
    // Computations are done by forming u and v vectors (in the x and y
    // directions) and then adding scaled versions of the u and v vectors
    // to existing model points.
    double[] p1 = new double[3];
    double[] p2 = new double[3];
    double[] p3 = new double[3];
    double[] p4 = new double[3];
    double[] u = new double[3];
    double[] v = new double[3];
    double[] p12 = new double[3];
    double[] p34 = new double[3];
    double[] p13 = new double[3];
    double[] p24 = new double[3];
    double[] p = new double[3];
    pointData = new float[xPoints*yPoints*3];
    int pointIndex = 0;

    for (int xPoint = 0; xPoint < xPoints; xPoint++) {
      for (int yPoint = 0; yPoint < yPoints; yPoint++) {

        // We compute the image x and y values for the current point.  These
        // are converted to access-relative values (ax,ay) as needed.  Points
        // with () around them in the diagrams below are the central points
        // that (x,y) is addressing.
        int x = xPoint*xPixels;
        if (x > tiling.width-1) x = tiling.width-1;
        int y = yPoint*yPixels;
        if (y > tiling.height-1) y = tiling.height-1;

        // Handle points along the top edge: top-left corner, top-right corner,
        // and top edge.  We use the data access along the two pixels of the
        // top edge.
        if (y == 0) {

          int ax = x;
          int ay = y;

          if (x == 0) {
            // (p1) p2   +--> u
            //  p3       |
            //           v v
            coordSource.translate (resultTop, ax, ay, p1);
            coordSource.translate (resultTop, ax+1, ay, p2);
            coordSource.translate (resultTop, ax, ay+1, p3);
            fromto (p1, p2, u);
            fromto (p1, p3, v);
            add (p1, -0.5, u, p);
            add (p, -0.5, v, p);

            if (LOGGER.isLoggable (Level.FINE)) {
              LOGGER.fine ("UL corner (x,y) = (" + x + "," + y + ") " +
                " with (x,y,z) = " + Arrays.toString (p) +
                " and (lat,lon) = " + Arrays.toString (toLatLon (p)));
            } // if

          } // if

          else if (x == tiling.width-1) {
            // p1 (p2)   +--> u
            //     p4    |
            //           v v
            coordSource.translate (resultTop, ax-1, ay, p1);
            coordSource.translate (resultTop, ax, ay, p2);
            coordSource.translate (resultTop, ax, ay+1, p4);
            fromto (p1, p2, u);
            fromto (p2, p4, v);
            add (p2, 0.5, u, p);
            add (p, -0.5, v, p);

            if (LOGGER.isLoggable (Level.FINE)) {
              LOGGER.fine ("UR corner (x,y) = (" + x + "," + y + ") " +
                " with (x,y,z) = " + Arrays.toString (p) +
                " and (lat,lon) = " + Arrays.toString (toLatLon (p)));
            } // if

          } // else if

          else {
            // p1 (p2)   +--> u
            // p3  p4    |
            //           v v
            coordSource.translate (resultTop, ax-1, ay, p1);
            coordSource.translate (resultTop, ax, ay, p2);
            coordSource.translate (resultTop, ax-1, ay+1, p3);
            coordSource.translate (resultTop, ax, ay+1, p4);
            avg (p1, p2, p12);
            avg (p3, p4, p34);
            fromto (p12, p34, v);
            add (p12, -0.5, v, p);
          } // else

        } // if

        // Handle points along the bottom edge: bottom-left corner, bottom-right
        // corner, and bottom edge.  We use the data access along the two pixels
        // of the bottom edge.
        else if (y == tiling.height-1) {

          int ax = x;
          int ay = 1;
        
          if (x == 0) {
            //  p1       +--> u
            // (p3) p4   |
            //           v v
            coordSource.translate (resultBottom, ax, ay-1, p1);
            coordSource.translate (resultBottom, ax, ay, p3);
            coordSource.translate (resultBottom, ax+1, ay, p4);
            fromto (p3, p4, u);
            fromto (p1, p3, v);
            add (p3, -0.5, u, p);
            add (p, 0.5, v, p);

            if (LOGGER.isLoggable (Level.FINE)) {
              LOGGER.fine ("LL corner (x,y) = (" + x + "," + y + ")" +
                " with (x,y,z) = " + Arrays.toString (p) +
                " and (lat,lon) = " + Arrays.toString (toLatLon (p)));
            } // if

          } // if

          else if (x == tiling.width-1) {
            //     p2    +--> u
            // p3 (p4)   |
            //           v v
            coordSource.translate (resultBottom, ax, ay-1, p2);
            coordSource.translate (resultBottom, ax-1, ay, p3);
            coordSource.translate (resultBottom, ax, ay, p4);
            fromto (p3, p4, u);
            fromto (p2, p4, v);
            add (p4, 0.5, u, p);
            add (p, 0.5, v, p);

            if (LOGGER.isLoggable (Level.FINE)) {
              LOGGER.fine ("LR corner (x,y) = (" + x + "," + y + ") " +
                " with (x,y,z) = " + Arrays.toString (p) +
                " and (lat,lon) = " + Arrays.toString (toLatLon (p)));
            } // if

          } // else if

          else {
            // p1  p2    +--> u
            // p3 (p4)   |
            //           v v
            coordSource.translate (resultBottom, ax-1, ay-1, p1);
            coordSource.translate (resultBottom, ax, ay-1, p2);
            coordSource.translate (resultBottom, ax-1, ay, p3);
            coordSource.translate (resultBottom, ax, ay, p4);
            avg (p1, p2, p12);
            avg (p3, p4, p34);
            fromto (p12, p34, v);
            add (p34, 0.5, v, p);
          } // else
        
        } // else if

        else {

          // Handle points along the left edge using the data access along
          // the two pixels of the left edge.
          if (x == 0) {

            int ax = x;
            int ay = y;

            //  p1  p2   +--> u
            // (p3) p4   |
            //           v v
            coordSource.translate (resultLeft, ax, ay-1, p1);
            coordSource.translate (resultLeft, ax+1, ay-1, p2);
            coordSource.translate (resultLeft, ax, ay, p3);
            coordSource.translate (resultLeft, ax+1, ay, p4);
            avg (p1, p3, p13);
            avg (p2, p4, p24);
            fromto (p13, p24, u);
            add (p13, -0.5, u, p);

          } // if
          
          // Handle points along the right edge using the data access along
          // the two pixels of the right edge.
          else if (x == tiling.width-1) {
        
            int ax = 1;
            int ay = y;

            // p1  p2    +--> u
            // p3 (p4)   |
            //           v v
            coordSource.translate (resultRight, ax-1, ay-1, p1);
            coordSource.translate (resultRight, ax, ay-1, p2);
            coordSource.translate (resultRight, ax-1, ay, p3);
            coordSource.translate (resultRight, ax, ay, p4);
            avg (p1, p3, p13);
            avg (p2, p4, p24);
            fromto (p13, p24, u);
            add (p24, 0.5, u, p);

          } // else if
          
          // Handle points in the inner region.  We use just two points for
          // this interpolation, and the two strided accesses -- the first
          // access is for points that fall on the top-left pixel p1, and the
          // second is for points that fall on the bottom-right pixel p4.
          else {

            // p1        +--> u
            //    p4     |
            //           v v
            coordSource.translate (resultInner, xPoint-1, yPoint-1, p1);
            coordSource.translate (resultInnerBR, xPoint-1, yPoint-1, p4);
            avg (p1, p4, p);

          } // else
        
        } // else

        // We now have in p the computed point in model coordinates that
        // we need for the mesh.  We store the point coordinates in the
        // array of all point data.
        int offset = pointIndex*3;
        pointData[offset + X] = (float) p[X];
        pointData[offset + Y] = (float) p[Y];
        pointData[offset + Z] = (float) p[Z];
        pointIndex++;

      } // for
    } // for

    // Now that we have all the points, we make a map that contains each
    // tile index with the data needed for a full resolution mesh of that
    // tile.
    int tileCount = tiling.getTiles();
    LOGGER.fine ("Image tiling mesh has " + tileCount + " tiles of width " +
      tiling.tileWidth + " by height " + tiling.tileHeight + " and " +
      pointData.length/3 + " total mesh points");
    meshDescriptorMap = new HashMap<>();
    for (int tileIndex = 0; tileIndex < tileCount; tileIndex++) {
      
      ImageTile tile = tiling.getTile (tileIndex);
            
      int startXPoint = tile.minX / xPixels;
      int endXPoint = Math.min (startXPoint + meshDivs, xPoints-1);
      int tileXPoints = endXPoint - startXPoint + 1;

      int startYPoint = tile.minY / yPixels;
      int endYPoint = Math.min (startYPoint + meshDivs, yPoints-1);
      int tileYPoints = endYPoint - startYPoint + 1;
      
      TileMeshDescriptor desc = new TileMeshDescriptor();
      desc.startXPoint = startXPoint;
      desc.startYPoint = startYPoint;
      desc.tileXPoints = tileXPoints;
      desc.tileYPoints = tileYPoints;

      meshDescriptorMap.put (tileIndex, desc);
    
    } // for

    // Finally, decide on which face winding order to use when generating
    // the mesh data.
    computeWindingOrder();

  } // TiledImageMeshFactory

  /////////////////////////////////////////////////////////////////

  /** Computes the winding order for this mesh factory. */
  private void computeWindingOrder () {
  
    // We have to determine which winding order tiles in this
    // mesh are in.  Normally coordinate data is stored in this
    // order when looking at a face:
    //
    // p1------p2
    // |     / |
    // |   /   |     (Case 1)
    // | /     |
    // p3------p4
    //
    // But it could be that the order is reversed, and coordinates actually
    // look like this:
    //
    // p3------p4
    // |     / |
    // |   /   |     (Case 2)
    // | /     |
    // p1------p2
    //
    // If faces are created from (p1,p3,p2) and (p2,p3,p4) in counter-clockwise
    // order under case 2, then they are facing the wrong way.  The test to
    // find out the correct winding order is to compute:
    //
    //  (p1 x p3) . p2 > 0
    //
    // If the statement is true, then the winding order of faces is
    // counter-clockwise, otherwise it's clockwise.  We know this because we
    // want the face normal to always point away from the origin.
    //
    // To find suitable (p1,p2,p3) points to test, we need to look through
    // the tiles and find one which doesn't have a degenerate triangle (which
    // can happen at a pole).  When one is found, we use it to compute the
    // winding order.  We look for points that are a similar order of magnitude
    // apart.
    windingOrderCounterClock = true;
    for (TileMeshDescriptor desc : meshDescriptorMap.values()) {
 
      int endXPoint = desc.startXPoint + desc.tileXPoints - 1;
      int endYPoint = desc.startYPoint + desc.tileYPoints - 1;
      Point3D p1 = getPoint (desc.startXPoint, desc.startYPoint);
      Point3D p2 = getPoint (endXPoint, desc.startYPoint);
      Point3D p3 = getPoint (desc.startXPoint, endYPoint);
      double p1p2 = p1.distance (p2);
      double p1p3 = p1.distance (p3);

      if (p1p2 != 0 && p1p3 != 0) {
        if ((Math.min (p1p2, p1p3) / Math.max (p1p2, p1p3)) > 0.3) {
          double result = (p1.crossProduct (p3)).dotProduct (p2);
          if (result < 0) windingOrderCounterClock = false;
          break;
        } // if
      } // if
    
    } // for
    LOGGER.fine ("Mesh winding order is " + (windingOrderCounterClock ? "counter-" : "") +
      "clockwise");

  } // computeWindingOrder

  /////////////////////////////////////////////////////////////////

  @Override
  public double getAspectRatio (
    int index
  ) {
  
    // We look at the corner points of the tile specified and compute the
    // approximate aspect ratio.
    //
    // p1------p2
    // |     / |
    // |   /   |
    // | /     |
    // p3------p4
    //
    TileMeshDescriptor desc = meshDescriptorMap.get (index);

    int endXPoint = desc.startXPoint + desc.tileXPoints - 1;
    int endYPoint = desc.startYPoint + desc.tileYPoints - 1;
    Point3D p1 = getPoint (desc.startXPoint, desc.startYPoint);
    Point3D p2 = getPoint (endXPoint, desc.startYPoint);
    Point3D p3 = getPoint (desc.startXPoint, endYPoint);
    Point3D p4 = getPoint (endXPoint, endYPoint);

    double p1p2 = p1.distance (p2);
    double p3p4 = p3.distance (p4);
    double p1p3 = p1.distance (p3);
    double p2p4 = p2.distance (p4);

    double width = Math.max (p1p2, p3p4);
    double height = Math.max (p1p3, p2p4);
    double aspect = (width == 0 || height == 0 ? Double.NaN : width/height);
  
    return (aspect);
  
  } // getAspectRatio

  /////////////////////////////////////////////////////////////////

  /** Gets a point from the point data. */
  private Point3D getPoint (int xPoint, int yPoint) {
  
    int index = xPoint*yPoints + yPoint;
    int offset = index*3;
    return (new Point3D (pointData[offset + X], pointData[offset + Y], pointData[offset + Z]));

  } // getPoint

  /////////////////////////////////////////////////////////////////

  /** Holds data about where to locate a tile's mesh point data. */
  private static class TileMeshDescriptor {

    /** The starting points for a tile mesh in the x and y directions. */
    public int startXPoint, startYPoint;
    
    /** The number of mesh points for a tile in the x and y directions. */
    public int tileXPoints, tileYPoints;
  
  } // TileMeshDescriptor class

  /////////////////////////////////////////////////////////////////

  // result = p2 - p1
  private static void fromto (double[] p1, double[] p2, double[] result) {
    for (int i = 0; i < 3; i++) result[i] = p2[i] - p1[i];
  } // fromto

  /////////////////////////////////////////////////////////////////

  // result = p + factor*v
  private static void add (double[] p, double factor, double[] v, double[] result) {
    for (int i = 0; i < 3; i++) result[i] = p[i] + factor*v[i];
  } // add

  /////////////////////////////////////////////////////////////////

  // result = (p1 + p2)/2
  private static void avg (double[] p1, double[] p2, double[] result) {
    for (int i = 0; i < 3; i++) result[i] = (p1[i] + p2[i])/2;
  } // avg

  /////////////////////////////////////////////////////////////////

  // Converts to (lat,lon) from model (x,y,z).
  private static double[] toLatLon (double[] p) {
  
    double[] sphere = new double[2];
    SphereFunctions.pointToSphere (p[X], p[Y], p[Z], sphere);
    double lat = 90 -  Math.toDegrees (sphere[THETA]);
    double lon = Math.toDegrees (sphere[PHI]) - 180;
    while (lon < -180) lon += 360;
    while (lon > 180) lon -= 360;
    return (new double[] {lat, lon});
    
  } // toLatLon
  
  /////////////////////////////////////////////////////////////////

  @Override
  public int getLevels() { return (dmin.length); }

  /////////////////////////////////////////////////////////////////

  @Override
  public TriangleMesh create (
    int index,
    int level,
    BooleanSupplier cancelled
  ) {

    // We start by retrieving the descriptor created in the constructor
    // for the tile index.
    TileMeshDescriptor desc = meshDescriptorMap.get (index);
    
    // We need to create a mesh that is possibly a subset of the full
    // resolution mesh.  Suppose we have a full resolution mesh of 5x5 points,
    // then the levels are:
    //
    // Level 0 = 5x5:
    //
    // +--+--+--+--+
    // |  |  |  |  |
    // +--+--+--+--+
    // |  |  |  |  |
    // +--+--+--+--+  <-- There's a stride of 1 along each direction.
    // |  |  |  |  |
    // +--+--+--+--+
    // |  |  |  |  |
    // +--+--+--+--+
    //
    // Level 1 = 3x3:
    //
    // +-----+-----+
    // |     |     |
    // |     |     |
    // |     |     |
    // +-----+-----+  <--- We have a stride of 2 between points in the
    // |     |     |       full resolution mesh.
    // |     |     |
    // |     |     |
    // +-----+-----+
    //
    // Level 2 = 2x2:
    //
    // +-----------+
    // |           |
    // |           |
    // |           |
    // |           |  <--- We have a stride of 4 between points in the full
    // |           |       resolution mesh.
    // |           |
    // |           |
    // +-----------+
    //
    // In general, stride = 2^level.  Except, there are places where tiles
    // are truncated and a full number of points in either x or y are not
    // available.  We treat these tiles by stopping at the last point when
    // subsampling the mesh by stride.
    int stride = (1 << level);

    int xBoxes = desc.tileXPoints-1;
    xBoxes = (int) Math.ceil ((double) xBoxes / stride);
    int[] xPointArray = new int[xBoxes+1];
    for (int i = 0; i < xPointArray.length; i++)
      xPointArray[i] = Math.min (desc.startXPoint + i*stride, xPoints-1);

    int yBoxes = desc.tileYPoints-1;
    yBoxes = (int) Math.ceil ((double) yBoxes / stride);
    int[] yPointArray = new int[yBoxes+1];
    for (int i = 0; i < yPointArray.length; i++)
      yPointArray[i] = Math.min (desc.startYPoint + i*stride, yPoints-1);

    int meshPoints = xPointArray.length * yPointArray.length;

    if (LOGGER.isLoggable (Level.FINEST))
      LOGGER.finest ("Tile[" + index + "] at level " + level + ": " + xPointArray.length + "x" + yPointArray.length);

    // We can now create the triangle mesh by tracing along each row and
    // column of boxes to form two triangles per box.  We start by adding all
    // the mesh points.
    TriangleMesh mesh = new TriangleMesh (VertexFormat.POINT_NORMAL_TEXCOORD);

    ObservableFloatArray meshPointArray = mesh.getPoints();
    meshPointArray.ensureCapacity (meshPoints * 3);
    for (int i = 0; i < xPointArray.length; i++) {
      for (int j = 0; j < yPointArray.length; j++) {
        int pointIndex = xPointArray[i]*yPoints + yPointArray[j];
        int offset = pointIndex*3;
        float x = pointData[offset + X];
        float y = pointData[offset + Y];
        float z = pointData[offset + Z];
        meshPointArray.addAll (x, y, z);

        if (LOGGER.isLoggable (Level.FINEST)) {
          double[] coord = new double[] {x, y, z};
          LOGGER.finest ("Added mesh point (x,y) = " + i + "," + j +
            " with (x,y,z) = " + Arrays.toString (coord) +
            " and (lat,lon) = " + Arrays.toString (toLatLon (coord)));
        } // if

      } // for
    } // for

    // Now we add the normals, so that any light source hitting the
    // surface shows even lighting even at the edge of a facet.
    ObservableFloatArray normalsArray = mesh.getNormals();
    normalsArray.setAll (meshPointArray);

    // Now we add the faces, two triangles per box:
    //
    // p1------p2
    // |     / |
    // |   /   |
    // | /     |
    // p3------p4
    //
    // Note that each point in a face is specifying the triplet (p,n,t)
    // for each triangle corner, which are indices into the point, normal,
    // and texture arrays.  So we have 9 values per face, but they're repeated
    // because we put the values of each in the same locations in the different
    // arrays.
    ObservableFaceArray meshFaceArray = mesh.getFaces();
    meshFaceArray.ensureCapacity ((xBoxes * yBoxes * 2) * 9);
    for (int i = 0; i < xBoxes; i++) {
      for (int j = 0; j < yBoxes; j++) {
        int p1 = i*(yBoxes+1) + j;
        int p3 = p1+1;
        int p2 = p1 + (yBoxes+1);
        int p4 = p2+1;

        // When the winding order is clockwise, essentially this means that
        // for our faces, we switch the order of p2 and p3.
        if (!windingOrderCounterClock) {
          int tmp = p2;
          p2 = p3;
          p3 = tmp;
        } // if
        meshFaceArray.addAll (p1, p1, p1, p3, p3, p3, p2, p2, p2);
        meshFaceArray.addAll (p2, p2, p2, p3, p3, p3, p4, p4, p4);

        if (LOGGER.isLoggable (Level.FINEST)) {
          LOGGER.finest ("Added faces (" + p1 + "," + p3 + "," + p2 + ")" +
            " and (" + p2 + "," + p3 + "," + p4 + ")");
        } // if

      } // for
    } // for

    // We initialize the texture array here because there are references
    // to it in the faces array.  If we don't initialize it and a texture is
    // never set up, the mesh has a zero extent bounds object and is not
    // displayed.  This seems to keep the system happy even though there
    // is no actual data in the texture yet.
    ObservableFloatArray meshTextureArray = mesh.getTexCoords();
    meshTextureArray.ensureCapacity (meshPoints * 2);
    for (int i = 0; i < meshPoints; i++) meshTextureArray.addAll (0, 0);

    return (mesh);
  
  } // create

  /////////////////////////////////////////////////////////////////

  @Override
  public void setTexturePoints (
    TriangleMesh mesh,
    int index,
    int level,
    int textureWidth,
    int textureHeight
  ) {

    // Recreate some of the numbers we need from when the mesh was created
    // in the first place.
    TileMeshDescriptor desc = meshDescriptorMap.get (index);

    int stride = (1 << level);
    int xBoxes = desc.tileXPoints-1;
    xBoxes = (int) Math.ceil ((double) xBoxes / stride);
    int xPoints = xBoxes+1;

    int yBoxes = desc.tileYPoints-1;
    yBoxes = (int) Math.ceil ((double) yBoxes / stride);
    int yPoints = yBoxes+1;

    int meshPoints = xPoints*yPoints;

    // Add the texture coordinates.  The u and v values are mainly
    // derived from the integer position of the points along the x and y
    // directions within the mesh, except for the last point which if the
    // mesh is truncated, needs to be forced to 1.
    ObservableFloatArray meshTextureArray = mesh.getTexCoords();
    meshTextureArray.clear();
    meshTextureArray.ensureCapacity (meshPoints * 2);

    // Set up for computing the normalized values for texture coordinates.
    int maxBoxes = (fullResPoints-1) / stride;
    ImageTile tile = tiling.getTile (index);
    float boxX = (float) tiling.tileWidth / (tile.width * maxBoxes);
    float boxY = (float) tiling.tileHeight / (tile.height * maxBoxes);

    // There's a 1 pixel border around the outside of the image where the
    // pixels are duplicated.  This avoids the texture wrap-around issue.  We
    // have to adjust and scale the texture coordinates to account for this
    // border.
    float startU = 1.0f/textureWidth;
    float scaleU = (float) (textureWidth-2) / textureWidth;
    float startV = 1.0f/textureHeight;
    float scaleV = (float) (textureHeight-2) / textureHeight;

    // Loop over all points along a row and down the columns and compute
    // the texture (u,v) values, taking into account the border pixel.
    for (int i = 0; i < xPoints; i++) {
      float u = startU + scaleU * Math.min (i*boxX, 1.0f);
      for (int j = 0; j < yPoints; j++) {
        float v = startV + scaleV * Math.min (j*boxY, 1.0f);

        meshTextureArray.addAll (u, v);

        if (LOGGER.isLoggable (Level.FINEST)) {
          LOGGER.finest ("Added texture point (x,y) = " + i + "," + j + " with (u, v) = " + u + "," + v);
        } // if

      } // for
    } // for

  } // setTexturePoints

  /////////////////////////////////////////////////////////////////

  @Override
  public int getLevelForDist (
    double dist
  ) {
  
    int level = dmin.length-1;
    while (dist < dmin[level] && level > 0) level--;
    return (level);

  } // getLevelForDist

  /////////////////////////////////////////////////////////////////

} // TiledImageMeshFactory class

