/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * An <code>Area</code> represents a center point and physical extent of the
 * surface of the earth.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Area {

  /** The name of the area for display. */
  public String name;

  /** The center latitude in degrees in the range [-90..90]. */
  public double latitude;

  /** The center longitude in degrees in the range [-180..180]. */
  public double longitude;

  /** The diameter in degrees of the area circle. */
  public double extent;

  /////////////////////////////////////////////////////////////////
  
  /**
   * Creates a new area object.
   *
   * @param latitude the center latitude in degrees in the range [-90..90].
   * @param longitude the center longitude in degrees in the range [-180..180].
   * @param extent the eter in degrees of the area circ.
   */
  public Area (
    String name,
    double latitude,
    double longitude,
    double extent
  ) {

    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.extent = extent;

  } // Area

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString() {
  
    return ("Area[name=" + name + ",latitude=" + latitude + ",longitude=" + longitude + ",extent=" + extent + "]");

  } // toString

  /////////////////////////////////////////////////////////////////

} // Area class

