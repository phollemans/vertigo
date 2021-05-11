/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import java.util.function.BiConsumer;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>GeoFunctionCoordinateSource</code> class wraps a function
 * and a coordinate translator to produce model (x,y,z) coordinates from
 * image (x,y) coordinates.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class GeoFunctionCoordinateSource implements ImageCoordinateSource {

  // Constants
  // ---------

  private static final int LAT = 0;
  private static final int LON = 1;
  private static final int X = 0;
  private static final int Y = 1;

  // Variables
  // ---------
  
  /** The function that translates image (x,y) to (lat,lon). */
  private BiConsumer<int[], double[]> func;

  /** The translator for (lat,lon) to model (x,y,z). */
  private GeoCoordinateTranslator trans;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Initializes this coordinate source.
   *
   * @see #getInstance
   */
  protected void init (
    BiConsumer<int[], double[]> func,
    GeoCoordinateTranslator trans
  ) {
  
    this.func = func;
    this.trans = trans;
    
  } // init

  /////////////////////////////////////////////////////////////////

  protected GeoFunctionCoordinateSource() {}

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new coordinate source.
   *
   * @param func the function that translates image (x,y) to (lat,lon).
   * @param trans the translator from (lat,lon) to model (x,y,z).
   */
  public static GeoFunctionCoordinateSource getInstance (
    BiConsumer<int[], double[]> func,
    GeoCoordinateTranslator trans
  ) {

    var source = new GeoFunctionCoordinateSource();
    source.init (func, trans);
    return (source);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

  /** The context data for use in calls to get coordinates. */
  private static class Context {

    public double[] lat;
    public double[] lon;
    public int accessWidth;
    public int accessHeight;
  
  } // Context class

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageAccessResult access (
    ImageAccess access,
    BooleanSupplier cancelled
  ) {
  
    ImageAccessResult result = null;
  
    int accessWidth = access.getWidth();
    int accessHeight = access.getHeight();
    double[] lat = new double[accessWidth*accessHeight];
    double[] lon = new double[accessWidth*accessHeight];

    int index = 0;
    int[] image = new int[2];
    double[] geo = new double[2];
    for (int ay = 0; ay < accessHeight; ay++) {
      image[Y] = access.tile.minY + ay*access.strideY;
      for (int ax = 0; ax < accessWidth; ax++) {
        image[X] = access.tile.minX + ax*access.strideX;
        func.accept (image, geo);
        lat[index] = geo[LAT];
        lon[index] = geo[LON];
        index++;
      } // for
      if (isTrue (cancelled)) break;
    } // for

    if (!isTrue (cancelled)) {
      Context context = new Context();
      context.lat = lat;
      context.lon = lon;
      context.accessWidth = accessWidth;
      context.accessHeight = accessHeight;
      result = new ImageAccessResult (access, context);
    } // if

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

    ImageAccess access = result.access;
    Context context = (Context) result.context;
    if (x < 0 || x > context.accessWidth-1)
      throw new IndexOutOfBoundsException ("Index " + x + " out of bounds for length " + (context.accessWidth-1));
    if (y < 0 || y > context.accessHeight-1)
      throw new IndexOutOfBoundsException ("Index " + y + " out of bounds for length " + (context.accessHeight-1));
    int index = y*context.accessWidth + x;
    trans.translate (context.lat[index], context.lon[index], data);

  } // get

  /////////////////////////////////////////////////////////////////

  @Override
  public void getMany (
    ImageAccessResult result,
    ImageCoordinateIterator iter,
    double[] data
  ) {

    ImageAccess access = result.access;
    Context context = (Context) result.context;

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

  /////////////////////////////////////////////////////////////////

} // GeoFunctionCoordinateSource class


