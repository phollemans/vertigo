/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * The <code>DatasetSurfaceFactoryBuilder</code> class builds
 * {@link DatasetSurfaceFactory} objects from a series of property values:
 * <ul>
 *   <li> name (string) - surface name</li>
 *   <li> type (string) - "dataset"</li>
 *   <li> url (string)</li>
 *   <li> variable (string)</li>
 *   <li> min (double)</li>
 *   <li> max (double)</li>
 *   <li> palette (string)</li>
 *   <li> function (string)</li>
 * </ul>
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class DatasetSurfaceFactoryBuilder extends BaseProjectObjectBuilder {

  private static final String TYPE = "DatasetSurface";

  /** The view context for the surfaces. */
  private GeoSurfaceViewContext viewContext;
  
  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new factory builder.
   *
   * @param viewContext the view context for the surfaces created by the factories.
   */
  public DatasetSurfaceFactoryBuilder (
    GeoSurfaceViewContext viewContext
  ) {
  
    this.viewContext = viewContext;

  } // DatasetSurfaceFactoryBuilder

  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  @Override
  public ProjectObject getObject() {

    // Access the dataset
    String url = (String) require ("url");
    Dataset dataset = new NetCDFDataset (url, viewContext.coordTrans);
    String variable = (String) require ("variable");

    // Create the array of colors from the palette
    String paletteName = (String) require ("palette");
    Palette palette = Palette.getInstance (paletteName);
    if (palette == null) palette = Palette.getInstance ("BW-Linear");
    List<Integer> colorList = new ArrayList<> (palette.getColors());
    colorList.add (0, 0xff000000);
    int[] map = colorList.stream().mapToInt (i -> i).toArray();

    // Create the mapping of values to colors
    double min = (Double) require ("min");
    double max = (Double) require ("max");
    String function = (String) require ("function", Set.of ("linear", "log"));
    DoubleToColorConverter converter = null;
    if (function.equals ("linear")) converter = DoubleToColorConverter.linearInstance (min, max, map);
    else if (function.equals ("log")) converter = DoubleToColorConverter.logInstance (min, max, map);

    // Create the surface factory and configure
    var factory = new DatasetSurfaceFactory (dataset, variable, converter, viewContext);
    factory.setName ((String) require ("name"));
    factory.setGroup ((String) require ("group"));
    for (String config : List.of ("selectable", "layer", "time", "level"))
      factory.setConfig (config, require ("config." + config));
    factory.legendFactoryProperty().getValue().logScaleHintProperty().setValue (function.equals ("log"));

    complete (factory);
    return (factory);
  
  } // getObject

  /////////////////////////////////////////////////////////////////

} // DatasetSurfaceFactoryBuilder class





