/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.ToDoubleBiFunction;
import java.util.TreeMap;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

import java.io.IOException;

/**
 * The <code>TiledImageFacetDataSource</code> class creates facet data
 * from an image that is divided into rectangular regions.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class TiledImageFacetDataSource extends FacetDataSource {

  private static final Logger LOGGER = Logger.getLogger (TiledImageFacetDataSource.class.getName());

  // Constants
  // ---------

  private static final int X = 0;
  private static final int Y = 0;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new data source.
   *
   * @param width the total width of the image data.
   * @param height the total height of the image data.
   * @param coordSource the coordinate source for translating 2D image (x,y)
   * to 3D model (x,y,z).
   * @param tileWriter the writer that provides texture images for tiles
   * within the image.
   * @param delta the function that computes the surface offset in model space
   * resulting from a straight line between two model (x,y,z) points.  This is
   * specific to whatever surface the facets represent and is used as the
   * error estimate to determine appropriate facet mesh resolution.
   * @param props the properties of the view.
   *
   * @throws IOException if an error occurred using the coordinate source to
   * initialize the tiling properties.
   */
  public TiledImageFacetDataSource (
    int width,
    int height,
    ImageCoordinateSource coordSource,
    ImageTileWriter tileWriter,
    ToDoubleBiFunction<double[], double[]> delta,
    ViewProperties props
  ) throws IOException {

    // Set up to probe the image coordinates to find the maximum delta.  We
    // start by looking at tiles of 128x128 pixels and then adjust up or down
    // accordingly.  We want to have at least a 5x5 grid covering the image
    // to start the measurements.
    int minStride = (int) Math.pow (2, Math.floor (Math.log (Math.min (width/5, height/5)) / Math.log (2)));
    int stride = Math.min (128, minStride);
    ImageTile tile = new ImageTile (0, 0, width, height);
    
    // We loop here until we find an appropriate value for the tile size that
    // satisfies the delta max high limit.
    double deltaMaxHigh = props.deltaMaxHigh();
    LOGGER.fine ("Max delta at camera dist " + props.cmin + " is " + deltaMaxHigh);
    TreeMap<Integer, ImageAccessResult> strideResultMap = new TreeMap<>();
    TreeMap<Integer, Double> maxDeltaMap = new TreeMap<>();
    double[] model = new double[3];
    double[] modelBR = new double[3]; // BR == below and to the right
    boolean done = false;
    while (!done) {
    
      // Access the coordinates and run through the model points to see
      // what the maximum delta value is for the current stride.  We look at
      // each accessed pixel, and then the one to it's lower right to see what
      // delta we have.
      ImageAccess access = new ImageAccess (tile, stride, stride);
      ImageAccessResult result = coordSource.access (access, null);
      int accessWidth = access.getWidth();
      int accessHeight = access.getHeight();
      double maxDelta = -Double.MAX_VALUE;
      for (int x = 0; x < accessWidth-1; x++) {
        for (int y = 0; y < accessHeight-1; y++) {
          coordSource.translate (result, x, y, model);
          coordSource.translate (result, x+1, y+1, modelBR);
          double deltaValue = delta.applyAsDouble (model, modelBR);
          if (deltaValue > maxDelta) maxDelta = deltaValue;
        } // for
      } // for
      
      // Check if we're below or above the delta limit and adjust the stride
      // accordingly.  Store the stride in the list of candidates if it
      // resulted in a max delta that was within the limit.  Also store the
      // max delta we found for later use.
      if (maxDelta <= deltaMaxHigh) {
        strideResultMap.put (stride, result);
        maxDeltaMap.put (stride, maxDelta);
        LOGGER.fine ("Computed stride (" + stride + ") -> maxDelta (" + maxDelta + ")");
        stride = stride*2;
      } // if
      else {
        stride = stride/2;
      } // else

      // Check if the stride still makes sense
      if (stride < 1) throw new RuntimeException ("Cannot find usable tile size");
      else if (stride > Math.min (width, height)) done = true;
      
      // Check if we've already tried the adjusted stride and break out if so.
      if (strideResultMap.containsKey (stride)) done = true;

    } // while

    // We want the stride that is the largest value that didn't result in
    // exceeding the delta max high limit.  This will be the last key in the map.
    // The stride now becomes the tile size n.  We then need to pick a value
    // for m, which is the number of tiles of size n pixels that form
    // a facet.  This will form the basis of the image tiling.
    int n = strideResultMap.lastKey();
    double maxDeltaLevel0 = maxDeltaMap.get (n);
    LOGGER.fine ("Found optimal n value " + n);

    // Now we have to hunt for the value of m by measuring against the
    // delta max low limit.  We use the coordinates that we saved when
    // computing the value for n.
    double deltaMaxLow = props.deltaMaxLow();
    LOGGER.fine ("Max delta at max camera dist " + props.cmax + " is " + deltaMaxLow);
    ImageAccessResult result = strideResultMap.get (n);
    ImageAccess access = new ImageAccess (tile, n, n);
    int accessWidth = access.getWidth();
    int accessHeight = access.getHeight();
    int m = 1;
    maxDeltaMap.clear();
    done = false;
    while (!done) {

      // Look for the maximum value of delta for increasing values of
      // m until we go over the limit.  Also save the max delta values
      // on the way, to use later in computing dmin.
      double maxDelta = -Double.MAX_VALUE;
      for (int x = 0; x < accessWidth-m; x += m) {
        for (int y = 0; y < accessHeight-m; y += m) {
          coordSource.translate (result, x, y, model);
          coordSource.translate (result, x+m, y+m, modelBR);
          double deltaValue = delta.applyAsDouble (model, modelBR);
          if (deltaValue > maxDelta) {
            maxDelta = deltaValue;
            maxDeltaMap.put (m, maxDelta);
          } // if
        } // for
      } // for

      // Check if we have a valid maximum delta.  If not, we stop iterating
      // because m has gotten too large for the image.
      if (maxDelta == -Double.MAX_VALUE) {
        m = m/2;
        done = true;
      } // if

      else {

        LOGGER.fine ("Computed m (" + m + ") -> maxDelta (" + maxDelta + ")");

        // Check if m has gotten too large and caused the max delta value
        // to be too high.  If so, we conclude that the next lower m was the
        // best choice.
        if (maxDelta > deltaMaxLow) {
          m = m/2;
          done = true;
        } // if

        // Otherwise, try increasing m but we can't increase so far that a facet
        // goes outside the bounds of the image (in the case of small images).
        else {
          m = m*2;
          if (m*n > Math.min (width, height)) { m = m/2; done = true; }
        } // else

      } // else

    } // while
    
    // We've got n and m now, and the image can be tiled using square facets
    // whose sides are m*n pixels large.  The number of levels for the mesh
    // factory is log_2(m) + 1.  The values for dmin for the mesh now need to
    // be calculated.
    LOGGER.fine ("Found optimal m value " + m);
    int tileSize = m*n;
    LOGGER.fine ("Image tile size is " + tileSize);
    ImageTiling tiling = new ImageTiling (width, height, tileSize, tileSize);
    int meshLevels = 31 - Integer.numberOfLeadingZeros (m) + 1;
    LOGGER.fine ("Mesh has " + meshLevels + " resolution levels:");

    double[] dminMesh = new double[meshLevels];
    dminMesh[0] = props.cameraDistMin (maxDeltaLevel0);
    LOGGER.fine ("  dMin[0]: " + dminMesh[0]);
    for (int level = 1; level < meshLevels; level++) {
      dminMesh[level] = props.cameraDistMin (maxDeltaMap.get (1 << level));
      LOGGER.fine ("  dMin[" + level + "]: " + dminMesh[level]);
    } // for
    
    // Now we need to compute the dmin value for the texture resolutions.
    // We start by searching for the facet with the largest size in either
    // the x or y directions.  This will be the facet that, when displayed
    // onscreen, will take up the largest amount of screen space for a given
    // camera distance.
    double[] modelB = new double[3];
    double[] modelR = new double[3];
    double maxSize = -Double.MAX_VALUE;
    for (int x = 0; x < accessWidth-m; x += m) {
      for (int y = 0; y < accessHeight-m; y += m) {
        coordSource.translate (result, x, y, model);
        coordSource.translate (result, x+m, y, modelR);
        coordSource.translate (result, x, y+m, modelB);
        double xSize = magnitude (model, modelR);
        double ySize = magnitude (model, modelB);
        double size = Math.max (xSize, ySize);
        if (size > maxSize) {
          maxSize = size;
        } // if
      } // for
    } // for
    LOGGER.fine ("Found largest facet has size " + maxSize);

    // For the tile with the largest size, we need to know what the camera
    // distances will be for when the texture is displayed at the various
    // resolutions.  Textures are displayed by level, where:
    //
    // level 0 = full resolution
    // level 1 = 1/2 resolution (ie: stride = 2)
    // level 2 = 1/4 resolution (ie: stride = 4)
    //
    // We compute the camera distance for each level until we find a level
    // whose distance is beyond the maximum camera distance and then stop.
    // There's no purpose in a texture resolution lower than that needed for
    // when the camera is at its furthest distance away.
    List<Double> dminTextureList = new ArrayList<>();
    for (int level = 0;; level++) {
      int textureRes = (1 << level);
      double cameraDist = props.cdist (maxSize, tileSize/textureRes);
      if (cameraDist < props.cmax) dminTextureList.add (cameraDist);
      else break;
    } // for
    int textureLevels = dminTextureList.size();
    if (textureLevels == 0) LOGGER.fine ("Texture has a single resolution");
    else LOGGER.fine ("Texture has " + textureLevels + " resolution levels:");
    double[] dminTexture = new double[textureLevels];
    for (int i = 0; i < textureLevels; i++) {
      dminTexture[i] = dminTextureList.get (i);
      LOGGER.fine ("  dMin[" + i + "]: " + dminTexture[i]);
    } // for

    // Now we create the various factories needed
    TiledImageMeshFactory meshFactory = new TiledImageMeshFactory (tiling, coordSource, dminMesh);
    TiledImageTextureFactory textureFactory = new TiledImageTextureFactory (tiling, tileWriter, dminTexture);
    init (tiling.getTiles(), meshFactory, textureFactory);

  } // TiledImageFacetDataSource

  /////////////////////////////////////////////////////////////////

  /**
   * Computes the magnitude of a model vector between two points.
   *
   * @param a the first point.
   * @param b the second point.
   *
   * @return the magnitude of (a - b).
   */
  private static double magnitude (double[] a, double[] b) {
  
    double sum = 0;
    for (int i = 0; i < 3; i++) {
      double diff = a[i] - b[i];
      sum += diff*diff;
    } // for
  
    return (Math.sqrt (sum));
  
  } // magnitude

  /////////////////////////////////////////////////////////////////

} // TiledImageFacetDataSource class
