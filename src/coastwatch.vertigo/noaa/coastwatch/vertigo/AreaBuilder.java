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

  private static final String TYPE = "Area";
  
  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectObject getObject() {

    String name = (String) require ("name");
    double lat = (Double) require ("latitude");
    double lon = (Double) require ("longitude");
    double extent = (Double) require ("extent");
    String group = (String) require ("group");
    var area = new Area (lat, lon, extent);
    area.setName (name);
    area.setGroup (group);

    complete (area);
    return (area);

  } // getObject

  /////////////////////////////////////////////////////////////////

} // AreaBuilder class






