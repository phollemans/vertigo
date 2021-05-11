/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import ucar.nc2.dataset.DatasetUrl;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.NetcdfDatasets;
import ucar.nc2.util.CancelTask;
import ucar.nc2.Variable;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.Section;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.function.BooleanSupplier;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>NetCDFDataSource</code> class provides data values from a 2D slice
 * of a variable in a NetCDF dataset.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class NetCDFDataSource implements ImageDataSource<double[]> {

  private static final Logger LOGGER = Logger.getLogger (NetCDFDataSource.class.getName());

  // Variables
  // ---------

  /** The NetCDF dataset, possibly remote. */
  private String datasetName;

  /** The variable name to access. */
  private String varName;

  /** The 2D slice of the variable to access. */
  private int[] slice;

  /////////////////////////////////////////////////////////////////

  static {

    var fileCache = NetcdfDatasets.getNetcdfFileCache();
    if (fileCache == null) {
      LOGGER.fine ("Initializing NetCDF file cache");
      NetcdfDatasets.initNetcdfFileCache (256, 512, 30);
    } // if

    // TODO: Where should we call NetcdfDatasets.shutdown() ?
  
  } // static

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new NetCDF source.
   *
   * @param datasetName the full path to the dataset, including protocol if remote.
   * @param varName the name of the variable within the dataset to access.
   * @param slice the 2D slice of the dataset to access.  This is an array
   * of values, two of which must be -1 to signify the dimensions to use for
   * the x and y indices of the slice.  All other slice values are the variable
   * dimensions to read with stride = 1 and length = 1.
   */
  public NetCDFDataSource (
    String datasetName,
    String varName,
    int[] slice
  ) {
  
    this.datasetName = datasetName;
    this.varName = varName;
    this.slice = slice;
  
  } // NetCDFDataSource

  /////////////////////////////////////////////////////////////////

  /** Holds data for cancelling a NetCDF dataset task. */
  private static class NetCDFCancelTask implements CancelTask {
  
    private BooleanSupplier cancelled;
    private boolean done;
  
    @Override
    public boolean isDone() { return (done); }
  
    @Override
    public void setDone (boolean done) { this.done = done; }
  
    @Override
    public boolean isCancel () { return (cancelled.getAsBoolean()); }

    @Override
    public void setError (String msg) { }
    
    @Override
    public void setProgress (String msg, int progress) { }
  
    public NetCDFCancelTask (BooleanSupplier cancelled) { this.cancelled = cancelled; }
  
  } // NetCDFCancelTask

  /////////////////////////////////////////////////////////////////

  /** The context data from a call to {@link #access}. */
  private static class NetCDFDataContext {

    private Array dataArray;
    private Index valueIndex;
  
  } // NetCDFDataContext class

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageAccessResult access (
    ImageAccess access,
    BooleanSupplier cancelled
  ) {
  
    int rank = slice.length;
    int[] start = new int[rank];
    int[] length = new int[rank];
    int[] stride = new int[rank];

    int[] xyStart = new int[] {access.tile.minX, access.tile.minY};
    int[] xyLength = new int[] {access.tile.width, access.tile.height};
    int[] xyStride = new int[] {access.strideX, access.strideY};
    int xy = 0;
    
    for (int i = 0; i < rank; i++) {
      if (slice[i] < 0) {
        start[i] = xyStart[xy];
        length[i] = xyLength[xy];
        stride[i] = xyStride[xy];
        xy++;
      } // if
      else {
        start[i] = slice[i];
        length[i] = 1;
        stride[i] = 1;
      } // else

    } // for

    DatasetUrl url = null;
    try { url = DatasetUrl.findDatasetUrl (datasetName); }
    catch (IOException e) { throw new RuntimeException (e); }

    CancelTask task = (cancelled != null ? new NetCDFCancelTask (cancelled) : null);

    ImageAccessResult result = null;
    try (NetcdfDataset ncDataset = NetcdfDatasets.acquireDataset (url, task)) {

      var root = ncDataset.getRootGroup();
      Variable variable = ncDataset.findVariable (root, varName);
      Section section = new Section (start, length, stride);

      if (!isTrue (cancelled)) {
        NetCDFDataContext context = new NetCDFDataContext();
        context.dataArray = variable.read (section);
        context.valueIndex = context.dataArray.getIndex();
        result = new ImageAccessResult (access, context);
      } // if

    } // try
    
    catch (Exception e) { throw new RuntimeException (e); }
  
    return (result);
    
  } // access

  /////////////////////////////////////////////////////////////////

  @Override
  public void get (
    ImageAccessResult result,
    int x,
    int y,
    double[] data
  ) {
  
    NetCDFDataContext context = (NetCDFDataContext) result.context;
    context.valueIndex.set (x, y);
    data[0] = context.dataArray.getDouble (context.valueIndex);

  } // get

  /////////////////////////////////////////////////////////////////

  @Override
  public void getMany (
    ImageAccessResult result,
    ImageCoordinateIterator iter,
    double[] data
  ) {

    NetCDFDataContext context = (NetCDFDataContext) result.context;
    int index = 0;
    while (iter.hasNext()) {
      iter.next();
      context.valueIndex.set (iter.getX(), iter.getY());
      data[index] = context.dataArray.getDouble (context.valueIndex);
      index++;
    } // while

  } // getMany

  /////////////////////////////////////////////////////////////////

} // NetCDFDataSource class

