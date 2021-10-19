/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.nio.IntBuffer;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.function.BooleanSupplier;

import java.util.logging.Logger;
import java.util.logging.Level;

import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelFormat;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>SimpleWebMap</code> class implements a version of the
 * <code>WebMap</code> interface.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class SimpleWebMap implements WebMap {

  private static final Logger LOGGER = Logger.getLogger (SimpleWebMap.class.getName());

  private static final int LAT = 0;
  private static final int LON = 1;
  private static final int X = 0;
  private static final int Y = 1;

  // Variables
  // ---------

  /* The URL pattern to use for tile retrieval. */
  private String urlPattern;

  /** The coordinate source for this web map. */
  private ImageCoordinateSource coordinateSource;

  /** The list of dates for this web map. */
  private List<Date> dateList;
  
  /** The dimensions of the map at highest resolution. */
  private int[] dims;

  /** The square tile size in pixels regardless of the resolution level. */
  private int tileSize;
  
  /** The number of resolution levels in the map. */
  private int levels;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a simple web map object.
   *
   * @param urlPattern the pattern to use for the URL for retrieving image
   * tiles.  The pattern is composed of the following special character
   * replacement sequences:
   * <ul>
   *   <li>%Y - the 4-digit year</li>
   *   <li>%M - the 2-digit month in the range [01..12]</li>
   *   <li>%D - the 2-digit day in the range [01..31]</li>
   *   <li>%J - the 3-digit Julian day in the range [001..366]</li>
   *   <li>%H - the 2-digit hour UTC in the range [00..23]</li>
   *   <li>%m - the 2-digit minute in the range [00..59]</li>
   *   <li>%L - the level in the range [0..levels-1]</li>
   *   <li>%l - the level in the range [1..levels]</li>
   *   <li>%y - the Y coordinate value of the tile, starting at 0 from the north</li>
   *   <li>%i - the Y coordinate value of the tile, starting at 0 from the south</li>
   *   <li>%x - the X coordinate value of the tile</li>
   * </ul>
   * @param levels the number of resolution levels.
   * @param dateList the list of dates available.  The index into the date
   * list is used in the {@link #getDataSource} method to specify which
   * date to use for tile images.
   * @param tileSize the square tile size in pixels for each server tile.
   * @param startLat the starting upper-left latitude of the map.  We assume
   * that maps are symmetrical about the equator and that the southern most
   * latitude is -startLat.
   * @param startLon the starting upper-left longitude of the map.
   * @param trans the translator from geographic (lat,lon) to model (x,y,z).
   */
  public SimpleWebMap (
    String urlPattern,
    int levels,
    List<Date> dateList,
    int tileSize,
    double startLat,
    double startLon,
    GeoCoordinateTranslator trans
  ) {

    this.urlPattern = urlPattern;
    this.levels = levels;
    this.dateList = dateList;
    this.tileSize = tileSize;

    int width = tileSize*2 * (1 << (levels-1));
    int height = width/2;
    dims = new int[] {width, height};
    coordinateSource = WebMapCoordinateSource.getInstance (levels, tileSize, startLat, startLon, trans);

  } // SimpleWebMap

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the date/time list.
   *
   * @param dateList the new list of dates values to use.
   *
   * @since 0.6
   */
  public void setTimes (List<Date> dateList) { this.dateList = dateList; }

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Date> getTimes() { return (dateList); }

  /////////////////////////////////////////////////////////////////

  @Override
  public int[] getDimensions() { return (dims); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageDataSource<int[]> getDataSource (
    int timeIndex
  ) {

    // Resolve the date values in the URL pattern and create the
    // data source using the specified time index.
    String url = urlPattern;
    if (dateList.size() != 0) {
      Calendar cal = Calendar.getInstance (TimeZone.getTimeZone ("UTC"));
      cal.setTime (dateList.get (timeIndex));
      url = url.replaceAll ("%Y", Integer.toString (cal.get (Calendar.YEAR)));
      url = url.replaceAll ("%M", String.format ("%02d", cal.get (Calendar.MONTH)+1));
      url = url.replaceAll ("%D", String.format ("%02d", cal.get (Calendar.DAY_OF_MONTH)));
      url = url.replaceAll ("%J", String.format ("%03d", cal.get (Calendar.DAY_OF_YEAR)));
      url = url.replaceAll ("%H", String.format ("%02d", cal.get (Calendar.HOUR_OF_DAY)));
      url = url.replaceAll ("%m", String.format ("%02d", cal.get (Calendar.MINUTE)));
    } // if

    return (new WebMapDataSource (url, levels, tileSize));
  
  } // getDataSource
  
  /////////////////////////////////////////////////////////////////

  @Override
  public ImageCoordinateSource getCoordinateSource() { return (coordinateSource); }

  /////////////////////////////////////////////////////////////////

  @Override
  public String getSourceURL() { return (urlPattern); }

  /////////////////////////////////////////////////////////////////

} // SimpleWebMap class
