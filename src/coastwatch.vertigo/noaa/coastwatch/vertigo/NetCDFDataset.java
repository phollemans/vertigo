/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.IOException;
import java.io.Closeable;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.function.BooleanSupplier;
import java.util.Timer;
import java.util.TimerTask;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.dataset.CoordinateAxis1D;
import ucar.nc2.Attribute;
import ucar.ma2.Range;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.DataType;
import ucar.unidata.geoloc.LatLonPoint;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>NetCDFDataset</code> class is a dataset based on a NetCDF file
 * or remote URL.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class NetCDFDataset implements Dataset, Closeable {

  private static final Logger LOGGER = Logger.getLogger (NetCDFDataset.class.getName());

  // Variables
  // ---------
  
  /** The NetCDF dataset, possibly remote. */
  private String datasetName;
  
  /** The translator for geographic (lat,lon) to image (x,y,z). */
  private GeoCoordinateTranslator trans;

  /** The cache of available handles to use for operations. */
  private List<DatasetHandle> handleCache;

  /** The closed flag, true if this dataset is closed. */
  private boolean isClosed;

  /////////////////////////////////////////////////////////////////

  /** Closes a NetCDF dataset with a warning on I/O exception. */
  private void closeDataset (NetcdfDataset ncDataset) {
  
    try { ncDataset.close(); }
    catch (IOException e) {
      LOGGER.log (Level.WARNING, "Close failed for NetCDF dataset " + datasetName, e);
    } // catch

  } // closeDataset

  /////////////////////////////////////////////////////////////////

  @Override
  public void close() {

    synchronized (handleCache) {

      if (!isClosed) {

        // Go through the cache of open datasets and close them.  There
        // may also be datasets that are still in use and not in the
        // cache.  These get cleaned up in the DatasetHandle close operation
        // below.
        for (var handle : handleCache) {
          closeDataset (handle.ncDataset);
        } // for

        // Mark this dataset as closed and clear the cache.  We don't want
        // to null the cache because we're using it for synchronized
        // statements all over the palce.
        handleCache.clear();
        isClosed = true;
  
      } // if
  
    } // synchronized
    
  } // close

  /////////////////////////////////////////////////////////////////

  /** Holds a reference to a dataset and allows for auto closing. */
  private class DatasetHandle implements AutoCloseable {
  
    public NetCDFCancelTask cancelTask;
    public NetcdfDataset ncDataset;
    public GridDataset gridDataset;
    
    @Override
    public void close () {

      synchronized (handleCache) {

        // Null the supplier for the next handle user.  Then, only if this
        // dataset isn't closed, put this handle back into the cache.
        cancelTask.setSupplier (null);
        if (!isClosed) handleCache.add (this);
        else closeDataset (this.ncDataset);

        if (LOGGER.isLoggable (Level.FINER)) LOGGER.finer ("Released dataset handle for " + datasetName);

      } // synchronized
    
    } // close

  } // DatasetHandle

  /////////////////////////////////////////////////////////////////

  /**
   * Holds data for cancelling a NetCDF dataset task. The cancel task is
   * passed in when opening a NetCDF dataset, and has a BooleanSupplier value
   * that can be set to cancel a task in progress, or set to null for no
   * cancelling.
   */
  private static class NetCDFCancelTask implements CancelTask {
  
    private BooleanSupplier supplier;
  
    @Override
    public boolean isCancel () { return (supplier != null && supplier.getAsBoolean()); }

    @Override
    public void setError (String msg) { }
    
    @Override
    public void setProgress (String msg, int progress) { }
  
    public void setSupplier (BooleanSupplier supplier) { this.supplier = supplier; }

  } // NetCDFCancelTask

  /////////////////////////////////////////////////////////////////

  /**
   * Acquires and returns an instance of a dataset handle that is not
   * currently in use by another thread.  One may be created if none
   * are available.
   *
   * @param supplier the supplier for booleans for the cancel task, or null
   * for none.
   *
   * @return the acquired handle or null if the operation was cancelled while
   * acquiring the handle.
   *
   * @throws IOException if the dataset is closed and so no handles can be
   * acquired.
   */
  private DatasetHandle acquireHandle (BooleanSupplier supplier) throws IOException {

    DatasetHandle handle = null;

    synchronized (handleCache) {

      // First check if this dataset is closed.  We can't acquire any more
      // handles in this case.
      if (isClosed) throw new IOException ("Cannot acquire handle for closed dataset");

      // Look for a handle in the cache.
      if (handleCache.size() != 0) {
        handle = handleCache.remove (0);
        handle.cancelTask.setSupplier (supplier);
        if (LOGGER.isLoggable (Level.FINER)) LOGGER.finer ("Retrieved dataset handle for " + datasetName);
      } // else
      
    } // synchronized

    // If we couldn't acquire a handle from the cache, create and return a
    // new one.  In the process, make sure we aren't cancelled while acquiring
    // the handle.
    if (handle == null) {
      var cancelTask = new NetCDFCancelTask();
      cancelTask.setSupplier (supplier);
      var ncDataset = NetcdfDataset.openDataset (datasetName, true, cancelTask);
      if (!cancelTask.isCancel()) {
        var gridDataset = new GridDataset (ncDataset);
        if (!cancelTask.isCancel()) {
          handle = new DatasetHandle();
          handle.cancelTask = cancelTask;
          handle.ncDataset = ncDataset;
          handle.gridDataset = gridDataset;
          if (LOGGER.isLoggable (Level.FINER)) LOGGER.finer ("Created new dataset handle for " + datasetName);
        } // if
      } // if
    } // if

    return (handle);

  } // acquireHandle

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new NetCDF dataset.
   *
   * @param datasetName the full path to the dataset, including protocol if
   * remote.
   * @param trans the translator from geographic (lat,lon) to model (x,y,z).
   */
  public NetCDFDataset (
    String datasetName,
    GeoCoordinateTranslator trans
  ) {

    this.datasetName = datasetName;
    this.trans = trans;
    this.handleCache = new ArrayList<>();

    // We create a timer here to verify that the cache has the number
    // of handles in it that we think should be there.
    if (LOGGER.isLoggable (Level.FINER)) {
      var updateTimer = new Timer (true);
      updateTimer.scheduleAtFixedRate (new TimerTask () {
        public void run() {
          synchronized (handleCache) {
            LOGGER.finer ("Handle cache for " + datasetName + " has " + handleCache.size() + " entries");
          } // synchronized
        } // run
      }, 0, 5000);
    } // if

  } // NetCDFDataset

  /////////////////////////////////////////////////////////////////

  @Override
  public List<String> getVariables() throws IOException {
  
    List<String> varList = new ArrayList<>();
    try (DatasetHandle handle = acquireHandle (null)) {

      List<GridDatatype> gridList = handle.gridDataset.getGrids();
      for (var grid : gridList) varList.add (grid.getName());

    } // try
    
    return (varList);
  
  } // getVariables

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Date> getTimes (
    String varName
  ) throws IOException {
  
    List<Date> timeList = new ArrayList<>();
    try (DatasetHandle handle = acquireHandle (null)) {

      GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
      GridCoordSystem system = grid.getCoordinateSystem();
      List<CalendarDate> dateList = system.getCalendarDates();

      for (var calDate : dateList) {
        Date date = new Date (calDate.getMillis());
        timeList.add (date);
      } // for
      
    } // try

    return (timeList);
  
  } // getTimes

  /////////////////////////////////////////////////////////////////

  @Override
  public List<Double> getLevels (
    String varName
  ) throws IOException {

    List<Double> levelList = new ArrayList<>();
    try (DatasetHandle handle = acquireHandle (null)) {

      GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
      GridCoordSystem system = grid.getCoordinateSystem();
      CoordinateAxis1D axis = system.getVerticalAxis();
      if (axis != null) {
        double[] levelValues = axis.getCoordValues();
        for (double level : levelValues) levelList.add (level);
      } // if
      
    } // try
    
    return (levelList);
  
  } // getLevels

  /////////////////////////////////////////////////////////////////

  @Override
  public String getLevelUnits (
    String varName
  ) throws IOException {

    String units = null;
    try (DatasetHandle handle = acquireHandle (null)) {

      GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
      GridCoordSystem system = grid.getCoordinateSystem();
      CoordinateAxis1D axis = system.getVerticalAxis();
      if (axis != null) {
        units = axis.getUnitsString();
      } // if

    } // try
  
    return (units);
  
  } // getLevelUnits

  /////////////////////////////////////////////////////////////////

  @Override
  public int[] getDimensions (
    String varName
  ) throws IOException {

    int[] dims = new int[2];
    try (DatasetHandle handle = acquireHandle (null)) {

      GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
      dims[0] = grid.getXDimension().getLength();
      dims[1] = grid.getYDimension().getLength();

    } // try
    
    return (dims);

  } // getDimensions
  
  /////////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getAttributes (
    String varName
  ) throws IOException {
  
    Map<String, Object> attMap = new LinkedHashMap<>();
    try (DatasetHandle handle = acquireHandle (null)) {

      GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
      List<Attribute> attList = grid.getAttributes();
      for (var att : attList) {
        Object value;
        if (att.isArray())
          value = att.getValues().copyTo1DJavaArray();
        else if (att.isString())
          value = att.getStringValue();
        else
          value = att.getNumericValue();
        attMap.put (att.getShortName(), value);
      } // for
      
    } // try
    
    return (attMap);

  } // getAttributes

  /////////////////////////////////////////////////////////////////

  @Override
  public Map<String, Object> getGlobalAttributes () throws IOException {
  
    Map<String, Object> attMap = new LinkedHashMap<>();
    try (DatasetHandle handle = acquireHandle (null)) {

      List<Attribute> attList = handle.ncDataset.getGlobalAttributes();
      for (var att : attList) {
        Object value;
        if (att.isArray())
          value = att.getValues().copyTo1DJavaArray();
        else if (att.isString())
          value = att.getStringValue();
        else
          value = att.getNumericValue();
        attMap.put (att.getShortName(), value);
      } // for
      
    } // try
    
    return (attMap);
  
  } // getGlobalAttributes

  /////////////////////////////////////////////////////////////////

  /** The context data for use in calls to data sources. */
  private static class DataContext {

    public Array dataArray;
    public Index valueIndex;

  } // DataContext class

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageDataSource<double[]> getDataSource (
    String varName,
    int timeIndex,
    int levelIndex
  ) throws IOException {

    ImageDataSource<double[]> dataSource = new ImageDataSource<>() {

      @Override
      public ImageAccessResult access (
        ImageAccess access,
        BooleanSupplier cancelled
      ) throws IOException {

        // Perform the data access.  Note that after every call to the
        // dataset, we check to see if we are cancelled.  If so, we return
        // a null result.

        ImageAccessResult result = null;
        try (DatasetHandle handle = acquireHandle (cancelled)) {
          if (isTrue (cancelled)) return (result);

          GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
          if (isTrue (cancelled)) return (result);

          // Detect an issue with the grid here.  This may indicate the dataset
          // is now in a corrupted state.
          if (grid == null) {
            throw new IOException ("Grid not found in call to findGridDatatype() for access " + access + " using handle " + handle);
          } // if

          Range xRange = new Range (access.tile.minX, access.tile.minX + access.tile.width - 1, access.strideX);
          Range yRange = new Range (access.tile.minY, access.tile.minY + access.tile.height - 1, access.strideY);
    
          GridDatatype subset = grid.makeSubset (
            new Range (0, 0),  // runtime
            new Range (0, 0),  // ensemble
            new Range (timeIndex, timeIndex),
            new Range (levelIndex, levelIndex),
            yRange,
            xRange
          );
          if (isTrue (cancelled)) return (result);

          // There seems to be a read error every so often for remote datasets.
          // So we make a small number of attempts here.
          Array array = null;
          boolean failed;
          int attempt = 1;
          IOException readException = null;
          do {
            try {
              array = subset.readDataSlice (0, 0, -1, -1);
              failed = false;
              if (attempt != 1) LOGGER.warning ("Successful read on attempt " + attempt + " for access " + access);
            } // try
            catch (IOException e) {
              LOGGER.warning ("Got exception on read attempt " + attempt + " for access " + access);
              failed = true;
              try { Thread.sleep (500); }
              catch (InterruptedException ie) { throw new IOException (ie); }
              attempt++;
              readException = e;
            } // catch
            if (isTrue (cancelled)) return (result);
          } while (failed == true && attempt <= 5);
          if (failed) throw (readException);

          DataContext context = new DataContext();
          context.dataArray = array;
          context.valueIndex = array.getIndex();
          result = new ImageAccessResult (access, context);
          
        } // try
        catch (InvalidRangeException e) { throw new IOException (e); }

        return (result);

      } // access

      @Override
      public void get (
        ImageAccessResult result,
        int x,
        int y,
        double[] data
      ) {

        DataContext context = (DataContext) result.context;
        context.valueIndex.set (y, x);
        data[0] = context.dataArray.getDouble (context.valueIndex);

      } // get

      @Override
      public void getMany (
        ImageAccessResult result,
        ImageCoordinateIterator iter,
        double[] data
      ) {

        DataContext context = (DataContext) result.context;
        int index = 0;
        while (iter.hasNext()) {
          iter.next();
          context.valueIndex.set (iter.getY(), iter.getX());
          data[index] = context.dataArray.getDouble (context.valueIndex);
          index++;
        } // while

      } // getMany

      @Override
      public void getAll (
        ImageAccessResult result,
        double[] data
      ) {

        DataContext context = (DataContext) result.context;
        double[] accessData = (double[]) context.dataArray.get1DJavaArray (DataType.DOUBLE);
        for (int i = 0; i < accessData.length; i++) data[i] = accessData[i];

      } // getAll

    };

    return (dataSource);

  } // getDataSource
  
  /////////////////////////////////////////////////////////////////

  /** The context data for use in calls to coordinate sources. */
  private static class CoordContext {

    public double[] lat;
    public double[] lon;
    public int accessWidth;
    public int accessHeight;
  
  } // CoordContext class

  /////////////////////////////////////////////////////////////////
  
  @Override
  public ImageCoordinateSource getCoordinateSource (
    String varName
  ) throws IOException {
  
    ImageCoordinateSource coordSource = new ImageCoordinateSource() {

      @Override
      public ImageAccessResult access (
        ImageAccess access,
        BooleanSupplier cancelled
      ) {
      
        ImageAccessResult result = null;
        try (DatasetHandle handle = acquireHandle (cancelled)) {

          GridDatatype grid = handle.gridDataset.findGridDatatype (varName);
          GridCoordSystem system = grid.getCoordinateSystem();

          int accessWidth = access.getWidth();
          int accessHeight = access.getHeight();
          double[] lat = new double[accessWidth*accessHeight];
          double[] lon = new double[accessWidth*accessHeight];

          int index = 0;
          for (int ay = 0; ay < accessHeight; ay++) {
            int y = access.tile.minY + ay*access.strideY;
            for (int ax = 0; ax < accessWidth; ax++) {
              int x = access.tile.minX + ax*access.strideX;
              LatLonPoint point = system.getLatLon (x, y);
              lat[index] = point.getLatitude();
              lon[index] = point.getLongitude();
              index++;
            } // for
            if (isTrue (cancelled)) break;
          } // for

          if (!isTrue (cancelled)) {
            CoordContext context = new CoordContext();
            context.lat = lat;
            context.lon = lon;
            context.accessWidth = accessWidth;
            context.accessHeight = accessHeight;
            result = new ImageAccessResult (access, context);
          } // if

        } // try
        catch (Exception e) { throw new RuntimeException (e); }

        return (result);

      } // access

      @Override
      public void get (
        ImageAccessResult result,
        int x,
        int y,
        double[] data
      ) {
      
        ImageAccess access = result.access;
        CoordContext context = (CoordContext) result.context;
        if (x < 0 || x > context.accessWidth-1)
          throw new IndexOutOfBoundsException ("Index " + x + " out of bounds for length " + (context.accessWidth-1));
        if (y < 0 || y > context.accessHeight-1)
          throw new IndexOutOfBoundsException ("Index " + y + " out of bounds for length " + (context.accessHeight-1));
        int index = y*context.accessWidth + x;
        trans.translate (context.lat[index], context.lon[index], data);
      
      } // get

      @Override
      public void getMany (
        ImageAccessResult result,
        ImageCoordinateIterator iter,
        double[] data
      ) {

        ImageAccess access = result.access;
        CoordContext context = (CoordContext) result.context;

        int index = 0;
        double[] temp = new double[3];
        while (iter.hasNext()) {
          iter.next();
          int x = iter.getX();
          if (x < 0 || x > context.accessWidth-1)
            throw new IndexOutOfBoundsException ("Index " + x + " out of bounds for length " + (context.accessWidth-1));
          int y = iter.getY();
          if (y < 0 || y > context.accessHeight-1)
            throw new IndexOutOfBoundsException ("Index " + y + " out of bounds for length " + (context.accessHeight-1));
          int coordIndex = y*context.accessWidth + x;
          trans.translate (context.lat[coordIndex], context.lon[coordIndex], temp);
          System.arraycopy (temp, 0, data, index*3, 3);
          index++;
        } // while

      } // getMany
    
    };
    
    return (coordSource);
  
  } // getCoordinateSource

  /////////////////////////////////////////////////////////////////

} // Dataset class

