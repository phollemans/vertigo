/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Date;
import javafx.scene.paint.Color;

/**
 * The <code>ProjectViewObject</code> interface is for classes that are project
 * objects that are visible in the view.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ProjectViewObject extends ProjectObject {

  /**
   * Gets an object configuration value.
   *
   * @param name the configuration value name.
   *
   * @return the configuration value, or null if none exists.
   */
  Object getConfig (String name);

  default boolean getConfigBoolean (String name) { return ((Boolean) getConfig (name)); }
  default int getConfigInteger (String name) { return ((Integer) getConfig (name)); }
  default double getConfigDouble (String name) { return ((Double) getConfig (name)); }
  default String getConfigString (String name) { return ((String) getConfig (name)); }
  default Date getConfigDate (String name) { return ((Date) getConfig (name)); }
  default Color getConfigColor (String name) { return ((Color) getConfig (name)); }

} // ProjectViewObject interface





