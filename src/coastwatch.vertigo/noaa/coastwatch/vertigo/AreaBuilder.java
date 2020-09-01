/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>AreaBuilder</code> class builds an {@link Area} from a series
 * of property values.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class AreaBuilder extends BaseProjectObjectBuilder {

  private static final String TYPE = "area";
  
  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  @Override
  public Object getObject() {

    String name = (String) require ("name");
    double lat = (Double) require ("latitude");
    double lon = (Double) require ("longitude");
    double extent = (Double) require ("extent");
    Object obj = new Area (name, lat, lon, extent);

    propertyMap.clear();
    return (obj);

  } // getObject

  /////////////////////////////////////////////////////////////////

} // AreaBuilder class






