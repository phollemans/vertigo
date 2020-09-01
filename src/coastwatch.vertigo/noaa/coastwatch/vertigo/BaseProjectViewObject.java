/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Map;
import java.util.HashMap;

/**
 * The <code>BaseProjectViewObject</code> is the base class for all project
 * view objects.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class BaseProjectViewObject implements ProjectViewObject {

  private String name;
  private Map<String, Object> config = new HashMap<>();

  public String getName() { return (name); }
  public void setName (String name) { this.name = name; }

  public Object getConfig (String name) { return (config.get (name)); }
  public void setConfig (String name, Object value) { config.put (name, value); }

} // BaseProjectViewObject interface






