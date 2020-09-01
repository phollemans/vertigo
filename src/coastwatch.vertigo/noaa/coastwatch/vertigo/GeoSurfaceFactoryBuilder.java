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

/**
 * The <code>GeoSurfaceFactoryBuilder</code> class builds
 * {@link GeoSurfaceFactory} objects from a series of property values.  For
 * a dataset-based surface:
 * <ul>
 *   <li> name (string) - surface name</li>
 *   <li> type (string) - "dataset"</li>
 *   <li> url (string)</li>
 *   <li> variable (string)</li>
 *   <li> min (double)</li>
 *   <li> max (double)</li>
 *   <li> palette (string)</li>
 *   <li> function (string)</li>
 * </ul>
 * For a webmap-based surface:
 * <ul>
 *   <li> name (string) - surface name</li>
 *   <li> type (string) - "webmap" </li>
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
 * @since 0.5
 */
public class GeoSurfaceFactoryBuilder extends BaseProjectObjectBuilder {

  private static final String TYPE = "surface";

  /** The view context for the surfaces. */
  private GeoSurfaceViewContext viewContext;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new factory builder.
   *
   * @param viewContext the view context for the surfaces created by the factories.
   */
  public GeoSurfaceFactoryBuilder (
    GeoSurfaceViewContext viewContext
  ) {
  
    this.viewContext = viewContext;

  } // GeoSurfaceFactoryBuilder

  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  @Override
  public Object getObject() {

    Object obj = null;
    List<String> configNames = null;
    String type = (String) require ("type", Set.of ("dataset", "webmap"));

    if (type.equals ("dataset")) {
    
      String url = (String) require ("url");
      Dataset dataset = new NetCDFDataset (url, viewContext.coordTrans);
      String variable = (String) require ("variable");

      String paletteName = (String) require ("palette");
      Palette palette = Palette.getInstance (paletteName);
      if (palette == null) palette = Palette.getInstance ("BW-Linear");
      List<Integer> colorList = new ArrayList<> (palette.getColors());
      colorList.add (0, 0xff000000);
      int[] map = colorList.stream().mapToInt (i -> i).toArray();

      double min = (Double) require ("min");
      double max = (Double) require ("max");
      String function = (String) require ("function", Set.of ("linear", "log"));
      DoubleToColorConverter converter = null;
      if (function.equals ("linear")) converter = DoubleToColorConverter.linearInstance (min, max, map);
      else if (function.equals ("log")) converter = DoubleToColorConverter.logInstance (min, max, map);

      var factory = new DatasetSurfaceFactory (dataset, variable, converter, viewContext);
      factory.setName ((String) require ("name"));
      for (String config : List.of ("selectable", "layer", "time", "level"))
        factory.setConfig (config, require ("config." + config));
      obj = factory;
      
    } // if

    else if (type.equals ("webmap")) {

      String url = (String) require ("url");
      int levels = (Integer) require ("levels");
      int size = (Integer) require ("size");
      double startLat = (Double) require ("start-lat");
      double startLon = (Double) require ("start-lon");
      
      List<Date> timeList = new ArrayList<>();
      boolean hasTime = (Boolean) require ("has-time");
      if (hasTime) {

        Date startTime = (Date) require ("start-time");
        Date endTime = (Date) require ("end-time");
        int timeStep = (Integer) require ("time-step");

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

      } // if


// TODO: Need to have a method that tests the web map time list somewhere.
// A good place to put it would be in the ImageDataSource -- it could have
// a general test function that WebMapDataSource could implement, then in
// the initialize function for the WebMapSurfaceFactory, the time list could
// be tested and then truncated if needed.



      WebMap webmap = new SimpleWebMap (url, levels, timeList, size, startLat, startLon, viewContext.coordTrans);
      var factory = new WebMapSurfaceFactory (webmap, viewContext);
      factory.setName ((String) require ("name"));
      for (String config : List.of ("selectable", "layer", "time"))
        factory.setConfig (config, require ("config." + config));
      obj = factory;

    } // else if

    propertyMap.clear();
    return (obj);
  
  } // getObject

  /////////////////////////////////////////////////////////////////

} // GeoSurfaceFactoryBuilder class




