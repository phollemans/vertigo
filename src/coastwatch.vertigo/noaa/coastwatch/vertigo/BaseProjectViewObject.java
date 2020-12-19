/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

/**
 * The <code>BaseProjectViewObject</code> is the base class for all project
 * view objects.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class BaseProjectViewObject extends BaseProjectObject implements ProjectViewObject {

  private Map<String, Object> config = new HashMap<>();

  public Object getConfig (String name) { return (config.get (name)); }
  public void setConfig (String name, Object value) { config.put (name, value); }

} // BaseProjectViewObject interface






