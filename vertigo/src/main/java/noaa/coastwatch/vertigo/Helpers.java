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

  /** Returns true if the supplier is not null, and returns true. */
  public static boolean isTrue (BooleanSupplier supplier) {
    return (supplier != null && supplier.getAsBoolean());
  } // isTrue

} // Helpers class
