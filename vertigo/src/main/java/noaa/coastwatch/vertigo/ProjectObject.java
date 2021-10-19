/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Map;

/**
 * The <code>ProjectObject</code> interface is for classes that are named
 * project objects.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ProjectObject {

  /**
   * Gets the project object name.
   *
   * @return the object name.
   */
  String getName();
  
  /**
   * Gets the project object group.
   *
   * @return the object group.
   */
  String getGroup();

  /**
   * Gets a map of specification values for this object, suitable for use in
   * building an identical instance of it.
   *
   * @return the object specifications as an unmodifiable map.
   *
   * @since 0.6
   */
  Map<String, Object> getSpec();

  /**
   * Sets the specification values for this object.
   *
   * @param spec the object specifications.
   *
   * @since 0.6
   */
  void setSpec (Map<String, Object> spec);

} // ProjectObject interface




