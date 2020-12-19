/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import java.util.Objects;

import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;

import java.io.FileNotFoundException;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;

import java.util.logging.Logger;
import java.util.logging.Level;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>WebMapDataSource</code> class provides image data from network
 * tiled web map images as 32-bit integer ARGB values.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WebMapDataSource implements ImageDataSource<int[]> {

  private static final Logger LOGGER = Logger.getLogger (WebMapDataSource.class.getName());

  private static final int X = 0;
  private static final int Y = 1;

  // Variables
  // ---------

  /* The URL pattern to use for tile retrieval. */
  private String urlPattern;

  /** The square tile size in pixels regardless of the resolution level. */
  private int tileSize;
  
  /** The number of resolution levels in the map. */
  private int levels;

  /** The cache map of tile key to image. */
  private Map<WebMapTileKey, Image> tileImageCache;

  /** The set of tiles current being retrieved. */
  private Set<WebMapTileKey> tileRetrievingSet;

  /** The set of tiles that were missing when retrieved. */
  private Set<WebMapTileKey> tileMissingSet;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new data source.
   *
   * @param urlPattern the pattern to use for the URL for retrieving image
   * tiles.  The pattern is composed of the following special character
   * replacement sequences:
   * <ul>
   *   <li>%L - the level in the range [0..levels-1]</li>
   *   <li>%l - the level in the range [1..levels]</li>
   *   <li>%y - the Y coordinate value of the tile, starting at 0 from the north</li>
   *   <li>%i - the Y coordinate value of the tile, starting at 0 from the south</li>
   *   <li>%x - the X coordinate value of the tile</li>
   * </ul>
   * @param levels the number of resolution levels.
   * @param tileSize the square tile size in pixels for each server tile.
   */
  public WebMapDataSource (
    String urlPattern,
    int levels,
    int tileSize
  ) {

    this.urlPattern = urlPattern;
    this.levels = levels;
    this.tileSize = tileSize;

    tileImageCache = new HashMap<>();
    tileRetrievingSet = new HashSet<>();
    tileMissingSet = new HashSet<>();

  } // WebMapDataSource

  /////////////////////////////////////////////////////////////////

  /** Holds the data for a web map tile key used for each image. */
  private static class WebMapTileKey {

    // The level of the tile in the web map.
    public int level;

    // The tile X coordinate.
    public int tileX;

    // The tile Y coordinate.
    public int tileY;

    /** Creates a new tile key. */
    public WebMapTileKey (int level, int tileX, int tileY) {
      this.level = level;
      this.tileX = tileX;
      this.tileY = tileY;
    } // WebMapTileKey

    @Override
    public int hashCode() { return (Objects.hash (level, tileX, tileY)); }

    @Override
    public boolean equals (Object obj) {
      boolean isEqual = false;
      if (obj instanceof WebMapTileKey) {
        WebMapTileKey key = (WebMapTileKey) obj;
        isEqual = (this.level == key.level && this.tileX == key.tileX && this.tileY == key.tileY);
      } // if
      return (isEqual);
    } // equals

    @Override
    public String toString () {
      return ("WebMapTileKey[level=" + level + ",tileX=" + tileX + ",tileY=" + tileY + "]");
    } // toString

  } // WebMapTileKey

  /////////////////////////////////////////////////////////////////

  /**
   * Gets a web map tile key for the given level and coordinates.
   *
   * @param level the level of the web map tiling [0..levels-1].
   * @param x the pixel x coordinate in the full resolution image.
   * @param y the pixel y coordinate in the full resolution image.
   *
   * @return the tile key of the web map tile at the specified
   * level that contains the full resolution (x,y) pixel.
   */
  private WebMapTileKey getTileKey (
    int level,
    int x,
    int y
  ) {
  
    int dLevel = (levels-1) - level;
    int mapFactor = (1 << dLevel);
    int mapTileSize = tileSize*mapFactor;
    var tileKey = new WebMapTileKey (level, x/mapTileSize, y/mapTileSize);

    return (tileKey);
  
  } // getTileKey

  /////////////////////////////////////////////////////////////////

  /** Holds the data for a retrieval request. */
  private static class RetrievalRequest {
    
    /** The outstanding tile keys required by this request. */
    public Set<WebMapTileKey> keys;

    /** The resulting images available. */
    public Map<WebMapTileKey, Image> imageMap;

    /** Creates a new retrieval request. */
    public RetrievalRequest (
      Set<WebMapTileKey> keys
    ) {
    
      this.keys = new HashSet<> (keys);
      this.imageMap = new HashMap<>();
      
    } // RetrievalRequest

    public boolean isComplete() {
      return (keys.size() == 0);
    } // isComplete

  } // RetrievalRequest class

  /////////////////////////////////////////////////////////////////

  /**
   * Performs a processing run of a request.  After processing, the
   * request may or may not be complete.  When complete, all tiles that
   * can possibly be added to the image map will be available.
   *
   * @param request the request to process.
   */
  private void process (
    RetrievalRequest request
  ) {

    // First transfer any of the images needed to complete the request
    // directly from the cache.
    synchronized (this) {
      for (var iter = request.keys.iterator(); iter.hasNext();) {
        var key = iter.next();
        if (tileImageCache.containsKey (key)) {
          request.imageMap.put (key, tileImageCache.get (key));
          iter.remove();
        } // if
      } // for
    } // synchronized
    
    // Next check if there's a web map image we can retrieve.
    WebMapTileKey keyToRetrieve = null;
    synchronized (this) {
      for (var key : request.keys) {
        if (!tileRetrievingSet.contains (key)) {
          keyToRetrieve = key;
          tileRetrievingSet.add (key);
          break;
        } // if
      } // for
    } // synchronized

    // Now perform the retrieval of the web map image.
    if (keyToRetrieve != null) {
      Image image = retrieve (keyToRetrieve);
      synchronized (this) {
        tileImageCache.put (keyToRetrieve, image);
        tileRetrievingSet.remove (keyToRetrieve);
      } // synchronized
    } // if

  } // process

  /////////////////////////////////////////////////////////////////

  /**
   * Tests the server connection for this data source.
   *
   * @throws Exception if an error occurred in testing.
   */
  public void testServer () throws Exception {

    var image1 = retrieveWithException (new WebMapTileKey (0, 0, 0));
    var image2 = retrieveWithException (new WebMapTileKey (0, 1, 0));

  } // testServer

  /////////////////////////////////////////////////////////////////

  /**
   * Retrieves a web map image using the specified key.
   *
   * @param key the key that specifies the level, x, and y values.
   *
   * @return the image.
   *
   * @throws Exception if an exception occurred when retrieving the image.
   *
   * @since 0.6
   */
  private Image retrieveWithException (
    WebMapTileKey key
  ) throws Exception {

    String tileURL = urlPattern;
    tileURL = tileURL.replaceAll ("%L", Integer.toString (key.level));
    tileURL = tileURL.replaceAll ("%l", Integer.toString (key.level + 1));
    tileURL = tileURL.replaceAll ("%x", Integer.toString (key.tileX));
    tileURL = tileURL.replaceAll ("%y", Integer.toString (key.tileY));
    tileURL = tileURL.replaceAll ("%i", Integer.toString ((1 << key.level) - 1 - key.tileY));
    Image image = new Image (tileURL);
    if (image.isError()) throw image.getException();

    return (image);

  } // retrieveWithException

  /////////////////////////////////////////////////////////////////

  /**
   * Retrieves a web map image using the specified key.
   *
   * @param key the key that specifies the level, x, and y values.
   *
   * @return the image or null on error.
   */
  private Image retrieve (
    WebMapTileKey key
  ) {

    // Check first if the tile has previously been found missing
    if (tileMissingSet.contains (key)) return (null);
  
    String tileURL = urlPattern;
    tileURL = tileURL.replaceAll ("%L", Integer.toString (key.level));
    tileURL = tileURL.replaceAll ("%l", Integer.toString (key.level + 1));
    tileURL = tileURL.replaceAll ("%x", Integer.toString (key.tileX));
    tileURL = tileURL.replaceAll ("%y", Integer.toString (key.tileY));
    tileURL = tileURL.replaceAll ("%i", Integer.toString ((1 << key.level) - 1 - key.tileY));
    Image image;
    try {
      image = new Image (tileURL);
      if (image.isError()) throw image.getException();
      LOGGER.finer ("Retrieved web map image with key " + key + ", URL " + tileURL);
    } // try
    catch (Exception e) {
      if (e instanceof FileNotFoundException) {
        LOGGER.warning ("Web map image file not found for URL " + tileURL);
        tileMissingSet.add (key);
      } // if
      else
        LOGGER.log (Level.WARNING, "Web map image retrieval failed for URL " + tileURL, e);
      image = null;
    } // catch

    return (image);

  } // retrieve

  /////////////////////////////////////////////////////////////////

  /** The context data for use in calls to data sources. */
  private static class Context {

    public int[] dataArray;
    public int accessWidth;
    public int accessHeight;

  } // Context class

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageAccessResult access (
    ImageAccess access,
    BooleanSupplier cancelled
  ) {
  
    // Determine what level in the web map tiling we need.  We compute the
    // minimum stride out of the X and Y directions, and then take the next
    // smallest factor of 2 to the stride.  For example, if the stride is
    // 14, we compute log_2 (14) = 3.81 so we round down to 3 and the stride
    // at that level is 8.  For a stride of 8, we go 3 levels down in
    // resolution from the maxium level and that's the level we want.  For
    // example if the number of levels is 8, level 7 is the full resolution,
    // so we go down 3 levels to level 4.
    int stride = (int) Math.min (access.strideX, access.strideY);
//    int dLevel = (int) Math.floor (Math.log (stride) / Math.log (2));
    int dLevel = (int) Math.ceil (Math.log (stride) / Math.log (2));
    int level = (levels-1) - dLevel;

    // Find the web map tile coordinates along the X and Y directions that cover
    // the requested tile.  Start by getting the tile coordinates of the corner
    // pixels.  These are the extremes that will help us compute all the
    // tile coordinates.  Then add all tiles in between.
    var minTileKey = getTileKey (level, access.tile.minX, access.tile.minY);
    int maxX = access.tile.minX + access.tile.width - 1;
    int maxY = access.tile.minY + access.tile.height - 1;
    var maxTileKey = getTileKey (level, maxX, maxY);
    Set<WebMapTileKey> tileKeySet = new LinkedHashSet<>();
    for (int y = minTileKey.tileY; y <= maxTileKey.tileY; y++) {
      for (int x = minTileKey.tileX; x <= maxTileKey.tileX; x++) {
        tileKeySet.add (new WebMapTileKey (level, x, y));
      } // for
    } // for

    // Submit the needed tile keys for retrieval and process.
    var request = new RetrievalRequest (tileKeySet);
    while (!request.isComplete() && !isTrue (cancelled)) process (request);

    ImageAccessResult result = null;
    if (!isTrue (cancelled)) {

      // Once outside of the request loop, one of two conditions must be
      // true: either we have all the tiles we need for the rendering, or
      // some of the tiles failed to load but we have the rest.  Either way
      // we work with what we have and build an array of tile image pixel
      // readers, which may include null values if any of the tile images
      // weren't retrieved.
      int tilesInX = maxTileKey.tileX - minTileKey.tileX + 1;
      int tilesInY = maxTileKey.tileY - minTileKey.tileY + 1;
      PixelReader[][] pixelReaders = new PixelReader[tilesInX][tilesInY];
      int missingTiles = 0;
      for (var key : tileKeySet) {
        Image image = request.imageMap.get (key);
        if (image == null) missingTiles++;
        else {
          int xIndex = key.tileX - minTileKey.tileX;
          int yIndex = key.tileY - minTileKey.tileY;
          pixelReaders[xIndex][yIndex] = image.getPixelReader();
        } // else
      } // for
      if (missingTiles != 0)
        LOGGER.finer ("Missing " + missingTiles + " web map image tiles for access " + access);

      // Now fill the access result with pixel data from the tiles
      // we just retrieved.  We start by computing vectors along the X and Y
      // directions for the source coordinates in the tiles.
      int mapFactor = (1 << dLevel);
      int mapTileSize = tileSize*mapFactor;

      int accessWidth = access.getWidth();
      int[] sourceXCoord = new int[accessWidth];
      int[] sourceXTile = new int[accessWidth];
      for (int x = 0; x < accessWidth; x++) {
        int mapX = access.tile.minX + x*access.strideX;
        sourceXTile[x] = (mapX / mapTileSize) - minTileKey.tileX;
        sourceXCoord[x] = (mapX % mapTileSize) / mapFactor;
      } // for

      int accessHeight = access.getHeight();
      int[] sourceYCoord = new int[accessHeight];
      int[] sourceYTile = new int[accessHeight];
      for (int y = 0; y < accessHeight; y++) {
        int mapY = access.tile.minY + y*access.strideY;
        sourceYTile[y] = (mapY / mapTileSize) - minTileKey.tileY;
        sourceYCoord[y] = (mapY % mapTileSize) / mapFactor;
      } // for

      // Now use the vectors just computed to extract the color data from
      // the various web map tiles.      
      int imagePixels = accessWidth*accessHeight;
      int[] colorData = new int[imagePixels];
      int index = 0;
      for (int y = 0; y < accessHeight; y++) {
        for (int x = 0; x < accessWidth; x++) {
          PixelReader reader = pixelReaders[sourceXTile[x]][sourceYTile[y]];
          if (reader != null)
            colorData[index] = reader.getArgb (sourceXCoord[x], sourceYCoord[y]);
          else
//            colorData[index] = 0xff000000; // TODO: Black or transparent?
            colorData[index] = 0x00000000; // TODO: Black or transparent?
          index++;
        } // for
        if (isTrue (cancelled)) break;
      } // for

      if (!isTrue (cancelled)) {
        var context = new Context();
        context.dataArray = colorData;
        context.accessWidth = accessWidth;
        context.accessHeight = accessHeight;
        result = new ImageAccessResult (access, context);
      } // if

    } // if

    return (result);

  } // access

  /////////////////////////////////////////////////////////////////

  @Override
  public void get (
    ImageAccessResult result,
    int x,
    int y,
    int[] data
  ) {
  
    ImageAccess access = result.access;
    Context context = (Context) result.context;
    if (x < 0 || x > context.accessWidth-1)
      throw new IndexOutOfBoundsException ("Index " + x + " out of bounds for length " + (context.accessWidth-1));
    if (y < 0 || y > context.accessHeight-1)
      throw new IndexOutOfBoundsException ("Index " + y + " out of bounds for length " + (context.accessHeight-1));
    int coordIndex = y*context.accessWidth + x;
    data[0] = context.dataArray[coordIndex];
  
  } // get

  /////////////////////////////////////////////////////////////////

  @Override
  public void getMany (
    ImageAccessResult result,
    ImageCoordinateIterator iter,
    int[] data
  ) {
  
    ImageAccess access = result.access;
    Context context = (Context) result.context;

    int index = 0;
    while (iter.hasNext()) {
      iter.next();
      int x = iter.getX();
      if (x < 0 || x > context.accessWidth-1)
        throw new IndexOutOfBoundsException ("Index " + x + " out of bounds for length " + (context.accessWidth-1));
      int y = iter.getY();
      if (y < 0 || y > context.accessHeight-1)
        throw new IndexOutOfBoundsException ("Index " + y + " out of bounds for length " + (context.accessHeight-1));
      int coordIndex = y*context.accessWidth + x;
      data[index] = context.dataArray[coordIndex];
      index++;
    } // while
  
  } // getMany

  /////////////////////////////////////////////////////////////////

  @Override
  public void getAll (
    ImageAccessResult result,
    int[] data
  ) {
  
    Context context = (Context) result.context;
    for (int i = 0; i < context.dataArray.length; i++) data[i] = context.dataArray[i];
  
  } // getAll

  /////////////////////////////////////////////////////////////////

} // WebMapDataSource class


