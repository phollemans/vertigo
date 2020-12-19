/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.function.ToDoubleBiFunction;

import java.net.URL;
import java.net.MalformedURLException;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>WebMapSurfaceFactory</code> class is a surface factory that
 * creates surfaces from a tiled web map.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WebMapSurfaceFactory extends BaseProjectViewObject implements GeoSurfaceFactory {

  private static final Logger LOGGER = Logger.getLogger (WebMapSurfaceFactory.class.getName());

  /** The web map to use for retrieving data and coordinates. */
  private WebMap webmap;

  /** The possible list of time steps. */
  private List<Date> timeList;
  
  /** The source for coordinate data. */
  private ImageCoordinateSource coordSource;

  /** The view context for the surface. */
  private GeoSurfaceViewContext viewContext;

  /** The pixels wide of the image data for the surface. */
  private int width;
  
  /** The pixels high of the image data for the surface. */
  private int height;

  /** The list of extra init tasks to perform during initialization. */
  private List<Runnable> initTasks;

  /////////////////////////////////////////////////////////////////

  @Override
  public void addInitTask (Runnable task) { initTasks.add (task); }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new surface factory.
   *
   * @param webmap the WebMap to use for retrieving data and coordinates.
   * @param viewContext the view context for the surfaces created by the factory.
   */
  public WebMapSurfaceFactory (
    WebMap webmap,
    GeoSurfaceViewContext viewContext
  ) {
  
    this.webmap = webmap;
    this.viewContext = viewContext;
    this.initTasks = new ArrayList<>();
  
  } // WebMapSurfaceFactory

  /////////////////////////////////////////////////////////////////

  @Override
  public boolean isInitialized() {

    return (coordSource != null);
  
  } // isInitialized

  /////////////////////////////////////////////////////////////////

  @Override
  public void initialize() throws IOException {

    // Before anything else, see if we are already initialized.
    if (isInitialized()) return;

    // Run the init tasks if there are any
    for (var task : initTasks) task.run();

    // Store the time and dimension information.
    timeList = webmap.getTimes();

    coordSource = webmap.getCoordinateSource();
    int[] dims = webmap.getDimensions();
    width = dims[0];
    height = dims[1];

    // Perform a test download of image data.
    var dataSource = (WebMapDataSource) webmap.getDataSource (0);
    try { dataSource.testServer(); }
    catch (Exception e) { throw new IOException (e); }

    // Report various statistics for debugging purposes.
    if (LOGGER.isLoggable (Level.FINE)) {

      int steps = timeList.size();
      SimpleDateFormat fmt = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
      fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
      String timeRange;
      if (steps == 0) timeRange = "No time information";
      else if (steps == 1) timeRange = fmt.format (timeList.get (0)) + " (1 timestep)";
      else timeRange = fmt.format (timeList.get (0)) + " to " + fmt.format (timeList.get (steps-1)) + " (" + timeList.size() + " steps)";

      LOGGER.fine ("Tiled web map:\n" +
        "  Time range:    " + timeRange + "\n" +
        "  Width, height: " + width + ", " + height
      );
      
    } // if

  } // initialize
  
  /////////////////////////////////////////////////////////////////

  @Override
  public boolean hasTimes() {

    return (timeList.size() != 0);
  
  } // hasTimes

  /////////////////////////////////////////////////////////////////

  @Override
  public boolean hasLevels() { return (false); }

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Date> getTimes() { return (timeList); }

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Double> getLevels() { return (List.of()); }

  /////////////////////////////////////////////////////////////////

  @Override
  public DynamicSurface createSurface (
    int time,
    int level
  ) throws IOException {
    
    // Get the data source for the given time and level (ignored if they
    // don't exist).
    if (!hasTimes()) time = 0;
    if (!hasLevels()) level = 0;
    ImageDataSource<int[]> dataSource = webmap.getDataSource (time);

    // Create the facet data source for the surface and then the surface
    // itself.
    ImageTileWriter tileWriter = new ColorTileWriter<int[]> (dataSource, new IntegerPassThroughConverter());
    FacetDataSource source = new TiledImageFacetDataSource (width, height, coordSource,
      tileWriter, viewContext.deltaFunc, viewContext.viewProps);
    DynamicSurface surface = new DynamicSurface (source);

    return (surface);
  
  } // createSurface

  /////////////////////////////////////////////////////////////////

  @Override
  public String getCredit() {

    String credit = null;
    try {
      var url = new URL (webmap.getSourceURL());
      credit = url.getHost();
      if (credit.length() == 0) credit = null;
    } // try
    catch (MalformedURLException e) { }
    
    return (credit);
    
  } // getCredit

  /////////////////////////////////////////////////////////////////

} // WebMapSurfaceFactory class



