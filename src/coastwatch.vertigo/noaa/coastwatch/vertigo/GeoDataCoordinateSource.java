/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;
import java.io.IOException;

import static noaa.coastwatch.vertigo.Helpers.isTrue;

/**
 * The <code>GeoDataCoordinateSource</code> class wraps a set of latitude
 * and longitude primitive double data sources and a coordinate translator
 * to produce model (x,y,z) coordinates from image (x,y) coordinates.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class GeoDataCoordinateSource implements ImageCoordinateSource {

  // Constants
  // ---------

  private static final int LAT = 0;
  private static final int LON = 1;

  // Variables
  // ---------
  
  /** The image data sources for latitude and longitude data. */
  private ImageDataSource<double[]>[] sourceArray;

  /** The translator for (lat,lon) to model (x,y,z). */
  private GeoCoordinateTranslator trans;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new coordinate source.
   *
   * @param sourceArray the data source array as [latitude, longitude].
   * @param trans the translator from (lat,lon) to model (x,y,z).
   */
  public GeoDataCoordinateSource (
    ImageDataSource<double[]>[] sourceArray,
    GeoCoordinateTranslator trans
  ) {
  
    this.sourceArray = sourceArray;
    this.trans = trans;
    
  } // GeoDataCoordinateSource

  /////////////////////////////////////////////////////////////////

  @Override
  public ImageAccessResult access (
    ImageAccess access,
    BooleanSupplier cancelled
  ) {
  
    ImageAccessResult[] context = null;

    try {
    
      ImageAccessResult latResult = sourceArray[LAT].access (access, cancelled);
      if (!isTrue (cancelled)) {
        ImageAccessResult lonResult = sourceArray[LON].access (access, cancelled);
        if (!isTrue (cancelled)) {
          context = new ImageAccessResult[] {latResult, lonResult};
        } // if
      } // if
  
    } // try
    catch (IOException e) { throw new RuntimeException (e); }
    
    return (new ImageAccessResult (access, context));
  
  } // access

  /////////////////////////////////////////////////////////////////

  @Override
  public void get (
    ImageAccessResult result,
    int x,
    int y,
    double[] data
  ) {

    ImageAccessResult[] resultArray = (ImageAccessResult[]) result.context;
    double[] temp = data;
    sourceArray[LAT].get (resultArray[LAT], x, y, temp);
    double lat = temp[0];
    sourceArray[LON].get (resultArray[LON], x, y, temp);
    double lon = temp[0];
    
    trans.translate (lat, lon, data);

  } // get

  /////////////////////////////////////////////////////////////////

  @Override
  public void getMany (
    ImageAccessResult result,
    ImageCoordinateIterator iter,
    double[] data
  ) {

    ImageAccessResult[] resultArray = (ImageAccessResult[]) result.context;

    int count = 0;
    while (iter.hasNext()) {
      iter.next();
      count++;
    } // while

    double[] lat = new double[count];
    iter.reset();
    sourceArray[LAT].getMany (resultArray[LAT], iter, lat);

    double[] lon = new double[count];
    iter.reset();
    sourceArray[LON].getMany (resultArray[LON], iter, lon);
    
    double[] tempData = new double[3];
    for (int i = 0; i < count; i++) {
      trans.translate (lat[i], lon[i], tempData);
      System.arraycopy (tempData, 0, data, i*3, 3);
    } // for

  } // getMany

  /////////////////////////////////////////////////////////////////

} // GeoDataCoordinateSource class


