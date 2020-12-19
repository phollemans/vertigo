/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Date;
import java.io.IOException;

import javafx.scene.layout.Region;

/**
 * The <code>GeoSurfaceFactory</code> interface is for classes that create
 * instances of geographic data-based dynamic surfaces, selected from various
 * vertical levels and time steps.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface GeoSurfaceFactory extends ProjectViewObject {

  /**
   * Checks if this factory is initialized.
   *
   * @return true if the factory is initialized, or false if not.
   */
  boolean isInitialized();

  /**
   * Adds a task to the list to be performed before any other initialization.
   *
   * @param task the task to add to the list.
   *
   * @since 0.6
   */
  void addInitTask (Runnable task);

  /**
   * Initializes the factory before performing any other queries.
   * Initialization may take some time.  If the factory is already initialized,
   * no operation is performed.
   *
   * @throws IOException if factory initialization failed.
   */
  void initialize() throws IOException;

  /**
   * Determines if this factory has multiple time steps available.
   *
   * @return true if there are time steps or false if not.
   */
  boolean hasTimes();

  /**
   * Determines if this factory has multiple levels available.
   *
   * @return true if there are levels or false if not.
   */
  boolean hasLevels();

  /**
   * Gets the time steps available in this factory.
   *
   * @return the list of times.  The index of each time in the list can be
   * used to create surface objects.  The list may have zero length
   * if there are no time steps.
   */
  List<Date> getTimes();

  /**
   * Gets the vertical levels available in this factory.
   *
   * @return the list of level values.  The index of each level in the list
   * can be used to create surface objects.  The list may have zero
   * length if there are no levels.
   */
  List<Double> getLevels();

  /**
   * Creates a dynamic surface at the specified time and level.
   *
   * @param time the time step for the surface, ignored if there are no
   * time steps.
   * @param level the level for the surface, ignored if there are no levels.
   *
   * @return the dynamic surface matching the specified time and level.
   *
   * @throws IOException if factory initialization failed.
   */
  DynamicSurface createSurface (
    int time,
    int level
  ) throws IOException;

  /**
   * Gets the legend for the surfaces created by this factory.
   *
   * @return the legend to combine with surface display, or null if there
   * is no legend.
   *
   * @since 0.6
   */
  default Region getLegend() { return (null); }

  /**
   * Gets the vertical level units available in this factory.
   *
   * @return the vertical level measurement units, or null for none.
   *
   * @since 0.6
   */
  default String getLevelUnits() { return (null); }

  /**
   * Gets the data credit for this factory.
   *
   * @return the data credit or null for none.
   *
   * @since 0.6
   */
  default String getCredit() { return (null); }

  /**
   * Gets the source URL for this factory.
   *
   * @return the source URL or null for none.
   *
   * @since 0.6
   */
  default String getSourceUrl() { return (null); }

} // GeoSurfaceFactory class

