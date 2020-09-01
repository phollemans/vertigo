/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import java.io.IOException;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>DatasetSurfaceFactory</code> class is a geographic surface
 * factory that creates surfaces from dataset variables.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class DatasetSurfaceFactory extends BaseProjectViewObject implements GeoSurfaceFactory {

  private static final Logger LOGGER = Logger.getLogger (DatasetSurfaceFactory.class.getName());

  /** The dataset to use for retrieving data and coordinates. */
  private Dataset dataset;

  /** The variable in the dataset to use. */
  private String variable;
  
  /** The function for the tile writer for converting data values into colors. */
  private DoubleToColorConverter converter;
  
  /** The possible list of time steps. */
  private List<Date> timeList;
  
  /** The possible list of level values. */
  private List<Double> levelList;

  /** The source for coordinate data. */
  private ImageCoordinateSource coordSource;

  /** The view context for the surfaces. */
  private GeoSurfaceViewContext viewContext;

  /** The pixels wide of the image data for the surface. */
  private int width;
  
  /** The pixels high of the image data for the surface. */
  private int height;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new surface factory.
   *
   * @param dataset the dataset to use for retrieving data and coordinates.
   * @param variable the variable in the dataset to use for data values.
   * @param converter the function for converting data values into colors.
   * @param viewContext the view context for the surfaces created by the factory.
   */
  public DatasetSurfaceFactory (
    Dataset dataset,
    String variable,
    DoubleToColorConverter converter,
    GeoSurfaceViewContext viewContext
  ) {
  
    this.dataset = dataset;
    this.variable = variable;
    this.converter = converter;
    this.viewContext = viewContext;
    
  } // DatasetSurfaceFactory

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

    // First check the dataset and see if the variable we need is there.
    var variables = dataset.getVariables();
    LOGGER.fine ("Dataset has " + variables.size() + " variable(s)");
    if (!variables.contains (variable))
      throw new IOException ("Dataset does not contain requested variable " + variable);

    // Next store the time, level, and dimension information.
    timeList = dataset.getTimes (variable);
    levelList = dataset.getLevels (variable);
    coordSource = dataset.getCoordinateSource (variable);
    int[] dims = dataset.getDimensions (variable);
    width = dims[0];
    height = dims[1];

    // Report various statistics for debugging purposes.
    if (LOGGER.isLoggable (Level.FINE)) {

      int steps = timeList.size();
      SimpleDateFormat fmt = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
      fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
      String timeRange;
      if (steps == 0) timeRange = "No time information";
      else if (steps == 1) timeRange = fmt.format (timeList.get (0)) + " (1 timestep)";
      else timeRange = fmt.format (timeList.get (0)) + " to " + fmt.format (timeList.get (steps-1)) + " (" + timeList.size() + " steps)";

      LOGGER.fine ("Variable " + variable + ":\n" +
        "  Time range:      " + timeRange + "\n" +
        "  Vertical levels: " + levelList.size() + "\n" +
        "  Width, height:   " + width + ", " + height
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
  public boolean hasLevels() {
  
    return (levelList.size() != 0);
  
  } // hasLevels

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Date> getTimes() { return (timeList); }

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Double> getLevels() { return (levelList); }

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
    ImageDataSource<double[]> dataSource = dataset.getDataSource (variable, time, level);

    // Create the facet data source for the surface and then the surface
    // itself.
    ImageTileWriter tileWriter = new ColorTileWriter<double[]> (dataSource, converter);
    FacetDataSource source = new TiledImageFacetDataSource (width, height, coordSource,
      tileWriter, viewContext.deltaFunc, viewContext.viewProps);
    DynamicSurface surface = new DynamicSurface (source);

    return (surface);
  
  } // createSurface

  /////////////////////////////////////////////////////////////////

} // DatasetSurfaceFactory class


