/*
 * Vertigo Project
 * Copyright (c) 2021 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo.coord;

import ucar.nc2.dataset.transform.AbstractTransformBuilder;
import ucar.nc2.dataset.transform.HorizTransformBuilderIF;
import ucar.nc2.constants.CF;
import ucar.nc2.AttributeContainer;
import ucar.nc2.dataset.ProjectionCT;

/**
 * An <code>EllipsoideMercatorBuilder</code> builds instances of
 * <code>EllipsoidMercator</code> objects wrapped for use by the NetCDF Java
 * library.
 *
 * @author Peter Hollemans
 * @since 0.7
 */
public class EllipsoidMercatorBuilder extends AbstractTransformBuilder implements HorizTransformBuilderIF {

  /////////////////////////////////////////////////////////////////

  @Override
  public String getTransformName() {

    return (CF.MERCATOR);

  } // getTransformName

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectionCT makeCoordinateTransform (
    AttributeContainer container,
    String geoCoordinateUnits
  ) {

    double scale = readAttributeDouble (container, CF.SCALE_FACTOR_AT_PROJECTION_ORIGIN, Double.NaN);
    double lon0 = readAttributeDouble (container, CF.LONGITUDE_OF_PROJECTION_ORIGIN, Double.NaN);
    double lat0 = readAttributeDouble (container, CF.STANDARD_PARALLEL, Double.NaN);
    double false_easting = readAttributeDouble (container, CF.FALSE_EASTING, 0.0);
    double false_northing = readAttributeDouble (container, CF.FALSE_NORTHING, 0.0);
    double semi_major_axis = readAttributeDouble (container, CF.SEMI_MAJOR_AXIS, Double.NaN);
    double semi_minor_axis = readAttributeDouble (container, CF.SEMI_MINOR_AXIS, Double.NaN);
    double inverse_flattening = readAttributeDouble (container, CF.INVERSE_FLATTENING, 0.0);

    // f' = inverse flattening
    // a = semi-major
    // b = semi-minor
    // f' = 1/f = a/(a-b)
    // f'(a-b) = a
    // f'a - f'b = a
    // -f'b = a - f'a
    // b = a - a/f' = a (1-1/f')

    double earth_radius = readAttributeDouble (container, CF.EARTH_RADIUS, Double.NaN);
    if (Double.isNaN (earth_radius)) {
      if (inverse_flattening != 0 && Double.isNaN (semi_minor_axis))
        semi_minor_axis = semi_major_axis*(1-1/inverse_flattening);
    } // if

    var proj = EllipsoidMercator.getInstance (lon0, lat0, scale, false_easting,
      false_northing, semi_major_axis, semi_minor_axis, earth_radius, geoCoordinateUnits);
    var trans = new ProjectionCT (container.getName(), "FGDC", proj);

    return (trans);
    
  } // makeCoordinateTransform
  
  /////////////////////////////////////////////////////////////////
  
} // EllipsoidMercatorBuilder class

