/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.function.BooleanSupplier;

/**
 * Methods to help make things simpler.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Helpers {

  /**
   * Determines if the supplier returns true.
   *
   * @param supplier the boolean supplier to check.
   *
   * @return true if the supplier is non-null and supplies a true value,
   * or false otherwise.
   */
  public static boolean isTrue (BooleanSupplier supplier) {
    return (supplier != null && supplier.getAsBoolean());
  } // isTrue

} // Helpers class
