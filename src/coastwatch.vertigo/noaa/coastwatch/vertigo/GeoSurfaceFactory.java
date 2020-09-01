/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.Date;
import java.io.IOException;

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
   * Gets the time index for this surface factory that is closest to a given
   * date.
   *
   * @param date the date to search for.
   *
   * @return the time index whose date is closest to that requested.
   *
   * @throws IllegalStateException if this method is called when there are
   * no time steps.
   */
  default int closestTimeIndex (Date date) {
  
    long minDiff = Long.MAX_VALUE;
    int index = -1;
    long time = date.getTime();
    List<Date> times = getTimes();
    for (int i = 0; i < times.size(); i++) {
      long surfaceTime = times.get (i).getTime();
      long diff = Math.abs (time - surfaceTime);
      if (diff < minDiff) { diff = minDiff; index = i; }
      if (diff == 0) break;
    } // for
  
    if (index == -1) throw new IllegalStateException ("Surface factory has no time steps");
    return (index);

  } // closestTimeIndex

  /**
   * Gets the level index for this surface factory that is closest to a given
   * level.
   *
   * @param level the level value to search for.
   *
   * @return the level index whose value is closest to that requested.
   *
   * @throws IllegalStateException if this method is called when there are
   * no levels.
   */
  default int closestLevelIndex (double level) {
  
    double minDiff = Double.MAX_VALUE;
    int index = -1;
    List<Double> levels = getLevels();
    for (int i = 0; i < levels.size(); i++) {
      double surfaceLevel = levels.get (i);
      double diff = Math.abs (level - surfaceLevel);
      if (diff < minDiff) { diff = minDiff; index = i; }
      if (diff == 0) break;
    } // for
  
    if (index == -1) throw new IllegalStateException ("Surface factory has no levels");
    return (index);
  
  } // closestLevelIndex

  /////////////////////////////////////////////////////////////////

} // GeoSurfaceFactory class

