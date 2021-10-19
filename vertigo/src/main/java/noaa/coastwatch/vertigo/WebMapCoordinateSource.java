
/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BiConsumer;

/**
 * The <code>WebMapCoordinateSource</code> class implements a coordinate
 * source for tiled web maps.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WebMapCoordinateSource extends GeoFunctionCoordinateSource {

  private static final int LAT = 0;
  private static final int LON = 1;
  private static final int X = 0;
  private static final int Y = 1;

  /////////////////////////////////////////////////////////////////

  /**
   * Initializes this coordinate source.
   *
   * @param levels the number of resolution levels.
   * @param tileSize the square tile size in pixels for each server tile.
   * @param startLat the starting upper-left latitude of the map.  We assume
   * that maps are symmetrical about the equator and that the southern most
   * latitude is -startLat.
   * @param startLon the starting upper-left longitude of the map.
   * @param trans the translator from geographic (lat,lon) to model (x,y,z).
   */
  protected void init (
    int levels,
    int tileSize,
    double startLat,
    double startLon,
    GeoCoordinateTranslator trans
  ) {

    // Compute the resolution in x and y.
    int width = tileSize*2 * (1 << (levels-1));
    int height = width/2;
    double xRes = 360.0/width;
    double yRes = startLat*2 / height;

    // Create the coordinate source function
    BiConsumer<int[],double[]> function = (image, geo) -> {
      geo[LAT] = startLat - yRes*(image[Y] + 0.5);
      geo[LON] = startLon + xRes*(image[X] + 0.5);
      while (geo[LON] > 180) geo[LON] -= 360;
      while (geo[LON] < -180) geo[LON] += 360;
    };

    super.init (function, trans);
    
  } // init

  /////////////////////////////////////////////////////////////////

  protected WebMapCoordinateSource() {}

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new coordinate source.
   *
   * @param levels the number of resolution levels.
   * @param tileSize the square tile size in pixels for each server tile.
   * @param startLat the starting upper-left latitude of the map.  We assume
   * that maps are symmetrical about the equator and that the southern most
   * latitude is -startLat.
   * @param startLon the starting upper-left longitude of the map.
   * @param trans the translator from geographic (lat,lon) to model (x,y,z).
   *
   * @return the coordinate source instance.
   */
  public static WebMapCoordinateSource getInstance (
    int levels,
    int tileSize,
    double startLat,
    double startLon,
    GeoCoordinateTranslator trans
  ) {

    var source = new WebMapCoordinateSource();
    source.init (levels, tileSize, startLat, startLon, trans);
    return (source);
    
  } // getInstance

  /////////////////////////////////////////////////////////////////

} // WebMapCoordinateSource class
