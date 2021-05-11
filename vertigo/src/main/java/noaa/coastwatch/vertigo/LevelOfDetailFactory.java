/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>LevelOfDetailFactory</code> interface is used by classes that create
 * model objects at various levels of detail.  Level 0 is the most detailed
 * level from the factory, while increasing levels have lower and lower detail.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface LevelOfDetailFactory {

  /**
   * Gets the number of levels of detail produced by this factory.
   *
   * @return the number of levels.
   */
  int getLevels();
    
  /**
   * Gets the level of detail required for a given camera distance.
   *
   * @param dist the distance that the camera is from the object being viewed.
   *
   * @return the level of detail required for the camera distance to show an
   * appropriate amount of visual information.
   */
  int getLevelForDist (
    double dist
  );

} // LevelOfDetailFactory interface
