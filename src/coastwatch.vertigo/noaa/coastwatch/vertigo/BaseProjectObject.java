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
 * The <code>BaseProjectObject</code> is the base class for all project
 * objects.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class BaseProjectObject implements ProjectObject {

  private String name;
  private String group;

  public String getName() { return (name); }
  public void setName (String name) { this.name = name; }

  public String getGroup() { return (group); }
  public void setGroup (String group) { this.group = group; }

  private Map<String, Object> spec = new HashMap<>();
  public Map<String, Object> getSpec() { return (Collections.unmodifiableMap (spec)); }
  public void setSpec (Map<String, Object> spec) { this.spec.clear(); this.spec.putAll (spec); }

} // BaseProjectObject interface







