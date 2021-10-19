/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.Collections;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.io.IOException;

import org.jsoup.Jsoup;

import java.net.URL;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>WebMapSurfaceFactoryBuilder</code> class builds
 * {@link WebMapSurfaceFactory} objects from a series of property values:
 * <ul>
 *   <li> name (string) - surface name</li>
 *   <li> url (string)</li>
 *   <li> levels (integer) - resolution levels</li>
 *   <li> size (integer) - tile size</li>
 *   <li> start-lat (double) - upper-left latitude</li>
 *   <li> start-lon (double) - upper-left longitude</li>
 *   <li> has-time (boolean) - true if there is a time component</li>
 *   <li> start-time (date) - YYYY/MM/DD HH:MM *or* number of minutes before current time</li>
 *   <li> end-time (date) - same as start-time</li>
 *   <li> time-step (integer) - time step interval in minutes</li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class WebMapSurfaceFactoryBuilder extends BaseProjectObjectBuilder {

  private static final Logger LOGGER = Logger.getLogger (WorldTester.class.getName());

  private static final String TYPE = "WebMapSurface";

  /** The view context for the surfaces. */
  private GeoSurfaceViewContext viewContext;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new factory builder.
   *
   * @param viewContext the view context for the surfaces created by the factories.
   */
  public WebMapSurfaceFactoryBuilder (
    GeoSurfaceViewContext viewContext
  ) {
  
    this.viewContext = viewContext;

  } // WebMapSurfaceFactoryBuilder

  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectObject getObject() {

    String url = (String) require ("url");
    int levels = (Integer) require ("levels");
    int size = (Integer) require ("size");
    double startLat = (Double) require ("start-lat");
    double startLon = (Double) require ("start-lon");

    WebMapSurfaceFactory factory = null;
    
    boolean hasTime = (Boolean) require ("has-time");
    if (hasTime) {
      String timeType = (String) require ("time-type", Set.of ("list", "range"));
      if (timeType.equals ("range")) {
        Date startTime = (Date) require ("start-time");
        Date endTime = (Date) require ("end-time");
        int timeStep = (Integer) require ("time-step");
        var timeList = getTimesFromRange (startTime, endTime, timeStep);
        var webmap = new SimpleWebMap (url, levels, timeList, size, startLat, startLon, viewContext.coordTrans);
        factory = new WebMapSurfaceFactory (webmap, viewContext);
      } // if
      else if (timeType.equals ("list")) {
        String listUrl = (String) require ("list-url");
        String listPattern = (String) require ("list-pattern");
        var webmap = new SimpleWebMap (url, levels, null, size, startLat, startLon, viewContext.coordTrans);
        factory = new WebMapSurfaceFactory (webmap, viewContext);
        factory.addInitTask (() -> {
          try {
            var timeList = getTimesFromUrl (listUrl, listPattern);
            webmap.setTimes (timeList);
          } // try
          catch (IOException e) { throw new RuntimeException (e); }
        });
      } // else if
    } // if
    else {
      var webmap = new SimpleWebMap (url, levels, new ArrayList<>(), size, startLat, startLon, viewContext.coordTrans);
      factory = new WebMapSurfaceFactory (webmap, viewContext);
    } // else
    
    


// TODO: Need to have a method that tests the web map time list somewhere.
// A good place to put it would be in the ImageDataSource -- it could have
// a general test function that WebMapDataSource could implement, then in
// the initialize function for the WebMapSurfaceFactory, the time list could
// be tested and then truncated if needed.




    factory.setName ((String) require ("name"));
    factory.setGroup ((String) require ("group"));
    for (String config : List.of ("selectable", "layer", "time"))
      factory.setConfig (config, require ("config." + config));

    complete (factory);
    return (factory);
  
  } // getObject

  /////////////////////////////////////////////////////////////////

  /**
   * Gets a list of date values using endpoints and an increment value.
   *
   * @param startTime the starting time value in the range.
   * @param endTime the ending time value in the range.
   * @param timeStep the increment between values in minutes.
   *
   * @return the list of date values.
   */
  private List<Date> getTimesFromRange (
    Date startTime,
    Date endTime,
    int timeStep
  ) {

    List<Date> timeList = new ArrayList<>();

    // Truncate the end time so that it falls an integer number of
    // time steps from the beginning of the day.
    long endTimeMillis = endTime.getTime();
    long timeSpanMillis = endTimeMillis - startTime.getTime();

    var utc = TimeZone.getTimeZone ("UTC");
    Calendar cal = Calendar.getInstance (utc);
    cal.setTime (endTime);
    cal.set (Calendar.HOUR_OF_DAY, 0);
    cal.set (Calendar.MINUTE, 0);
    cal.set (Calendar.SECOND, 0);
    cal.set (Calendar.MILLISECOND, 0);
    long startOfDayMillis = cal.getTimeInMillis();
    
    long endTimeDiff = endTimeMillis - startOfDayMillis;
    long timeStepMillis = timeStep*60000;
    if (endTimeDiff % timeStepMillis != 0) {
      long timeSteps = endTimeDiff / timeStepMillis;
      endTime = new Date (startOfDayMillis + timeSteps*timeStepMillis);
      startTime = new Date (endTime.getTime() - timeSpanMillis);
    } // if

    // Loop over all time steps from start to end and populate the
    // time step list.
    cal.setTime (startTime);
    Calendar calEnd = Calendar.getInstance (utc);
    calEnd.setTime (endTime);

    while (cal.compareTo (calEnd) <= 0) {
      timeList.add (cal.getTime());
      cal.add (Calendar.MINUTE, timeStep);
    } // for
  
    return (timeList);
  
  } // getTimesFromRange

  /////////////////////////////////////////////////////////////////

  /**
   * Gets a list of date values using data from an HTTP server directory
   * list URL.
   *
   * @param listURL the URL to parse for date data.
   * @param listPattern the date pattern to search for.
   *
   * @return the list of date values.
   */
  private List<Date> getTimesFromUrl (
    String listUrl,
    String listPattern
  ) throws IOException {

    SimpleDateFormat fmt = new SimpleDateFormat (listPattern);
    fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));

    var doc = Jsoup.connect (listUrl).get();
    var links = doc.select ("a[href]");

    List<Date> timeList = new ArrayList<>();

    for (var link : links) {
      try {
        Date date = fmt.parse (link.text());
        timeList.add (date);
      } // try
      catch (ParseException e) { }
    } // for
    Collections.sort (timeList);

    LOGGER.fine ("Retrieved " + timeList.size() + " time steps from " + listUrl);

    return (timeList);

  } // getTimesFromUrl
  
  /////////////////////////////////////////////////////////////////

} // WebMapSurfaceFactoryBuilder class





