/*
 * Vertigo Project
 * Copyright (c) 2021 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo.coord;

import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPoint;
import ucar.unidata.geoloc.ProjectionPointImpl;
import ucar.unidata.geoloc.LatLonPoint;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.LatLonPoints;
import ucar.nc2.constants.CF;
import ucar.nc2.constants.CDM;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>EllipsoidMercator</code> class performs Mercator projection
 * calculations for an ellipsoid earth model, wrapped for use with the NetCDF
 * Java library.
 *
 * @author Peter Hollemans
 * @since 0.7
 */
public class EllipsoidMercator extends ProjectionImpl {

  private static final Logger LOGGER = Logger.getLogger (EllipsoidMercator.class.getName());

  /** The transform from geographic to projection coordinates. */
  private CoordinateTransform toProjectionTrans;

  /** The transform from projection to geographic coordinates. */
  private CoordinateTransform toGeographicTrans;

  /////////////////////////////////////////////////////////////////

  protected EllipsoidMercator () { super ("Mercator", false); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectionImpl constructCopy() {

    var copy = new EllipsoidMercator();
    copy.toProjectionTrans = toProjectionTrans;
    copy.toGeographicTrans = toGeographicTrans;
    copy.setDefaultMapArea (defaultMapArea);
    copy.setName (name);
    for (var param : getProjectionParameters()) copy.addParameter (param);

    return (copy);

  } // constructCopy

  /////////////////////////////////////////////////////////////////

  /**
   * Creates an object instance.
   *
   * @param longitudeOrigin the longitude of the origin or Double.NaN for the default (0.0).
   * @param standardParallel the standard parallel or Double.NaN for the default (0.0).
   * @param scaleFactor the scale factor or Double.NaN for the default (1.0).  If both
   * standardParallel and scaleFactor are speficied, standardParallel takes precedence.
   * @param falseEasting the false easting or Double.NaN for the default (0.0).
   * @param falseNorthing the false northing or Double.NaN for the default (0.0).
   * @param semiMajor the semi-major axis of the ellipsoid in meters or Double.NaN for a sphere.
   * @param semiMinor the semi-minor axis of the ellipsoid in meters or Double.NaN for a sphere.
   * @param earthRadius the earth radius for the sphere in meters or Double.NaN for an ellipsoid.
   * If not specified, the semi-major and semi-minor axes must be specified.
   * @param units the units to use for XY projection point coordinate values.
   */
  public static EllipsoidMercator getInstance (
    double longitudeOrigin,
    double standardParallel,
    double scaleFactor,
    double falseEasting,
    double falseNorthing,
    double semiMajor,
    double semiMinor,
    double earthRadius,
    String units
  ) {
    
    var instance = new EllipsoidMercator();

    String params = "+proj=merc";
    instance.addParameter (CF.GRID_MAPPING_NAME, CF.MERCATOR);
    
    if (!Double.isNaN (longitudeOrigin)) {
      params += " +lon_0=" + longitudeOrigin;
      instance.addParameter (CF.LONGITUDE_OF_PROJECTION_ORIGIN, longitudeOrigin);
    } // if
    if (!Double.isNaN (standardParallel)) {
      params += " +lat_ts=" + standardParallel;
      instance.addParameter (CF.STANDARD_PARALLEL, standardParallel);
    } // if
    if (!Double.isNaN (scaleFactor)) {
      params += " +k_0=" + scaleFactor;
      instance.addParameter (CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, scaleFactor);
    } // if
    if (!Double.isNaN (falseEasting)) {
      params += " +x_0=" + falseEasting;
      instance.addParameter (CF.FALSE_EASTING, falseEasting);
    } // if
    if (!Double.isNaN (falseNorthing)) {
      params += " +y_0=" + falseNorthing;
      instance.addParameter (CF.FALSE_NORTHING, falseNorthing);
    } // if
    if (!Double.isNaN (semiMajor)) {
      params += " +a=" + semiMajor;
      instance.addParameter (CF.SEMI_MAJOR_AXIS, semiMajor);
    } // if
    if (!Double.isNaN (semiMinor)) {
      params += " +b=" + semiMinor;
      instance.addParameter (CF.SEMI_MINOR_AXIS, semiMinor);
    } // if
    if (!Double.isNaN (earthRadius)) {
      params += " +R=" + earthRadius;
      instance.addParameter (CF.EARTH_RADIUS, earthRadius);
    } // if
    params += " +units=" + units;
    instance.addParameter (CDM.UNITS, units);

    var systemFactory = new CRSFactory();
    var system = systemFactory.createFromParameters (null, params);
    var geo = system.createGeographic();

    LOGGER.fine ("Created PROJ.4 projection with parameters '" + params + "'");

    var transformFactory = new CoordinateTransformFactory();
    instance.toProjectionTrans = transformFactory.createTransform (geo, system);
    instance.toGeographicTrans = transformFactory.createTransform (system, geo);
  
    return (instance);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

  @Override
  public String paramsToString () { return (toString()); }

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {

    StringBuffer buffer = new StringBuffer();
    buffer.append ("Mercator{");
    var paramList = getProjectionParameters();
    for (int i = 0; i < paramList.size(); i++) {
      var param = paramList.get (i);
      buffer.append (param.toString().replace (" = ", "="));
      if (i < paramList.size()-1) buffer.append (",");
    } // for
    buffer.append ("}");
    
    return (buffer.toString());

  } // toString

  /////////////////////////////////////////////////////////////////

  @Override
  public boolean crossSeam (
    ProjectionPoint p1,
    ProjectionPoint p2
  ) {

    boolean cross = false;
    
    // Check for either point being infinite
    if (LatLonPoints.isInfinite (p1) || LatLonPoints.isInfinite (p2))
      cross = true;

    // Check for opposite sign
    if (p1.getX()*p2.getX() < 0)
      cross = true;

    return (cross);

  } // crossSeam

  /////////////////////////////////////////////////////////////////

  @Override
  public boolean equals (Object obj) {

    boolean isEqual = false;
    if (obj instanceof EllipsoidMercator) {
      var merc = (EllipsoidMercator) obj;
      isEqual = toProjectionTrans.getTargetCRS().equals (merc.toProjectionTrans.getTargetCRS());
      if (isEqual) {
        isEqual = ((defaultMapArea == null) == (merc.defaultMapArea == null));
      } // if
      if (isEqual) {
        isEqual = (defaultMapArea == null || merc.defaultMapArea.equals (defaultMapArea));
      } // if
    } // if

    return (isEqual);

  } // equals

  /////////////////////////////////////////////////////////////////

  @Override
  public int hashCode() { return (toProjectionTrans.getTargetCRS().hashCode()); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectionPoint latLonToProj (
    LatLonPoint latLon,
    ProjectionPointImpl result
  ) {

    ProjCoordinate source = new ProjCoordinate (latLon.getLongitude(), latLon.getLatitude());
    ProjCoordinate target = new ProjCoordinate();
    toProjectionTrans.transform (source, target);

    if (target.hasValidXandYOrdinates())
      result.setLocation (target.x, target.y);
    else
      result.setLocation (Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    return (result);

  } // latLonToProj

  /////////////////////////////////////////////////////////////////

  @Override
  public LatLonPoint projToLatLon (
    ProjectionPoint xy,
    LatLonPointImpl result
  ) {

    ProjCoordinate source = new ProjCoordinate (xy.getX(), xy.getY());
    ProjCoordinate target = new ProjCoordinate();
    toGeographicTrans.transform (source, target);

    if (target.hasValidXandYOrdinates())
      result.set (target.y, target.x);
    else
      result.set (Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);

    return (result);
    
  } // projToLatLon

  /////////////////////////////////////////////////////////////////

} // EllipsoidMercator class


