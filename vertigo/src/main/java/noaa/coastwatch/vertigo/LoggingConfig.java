/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.logging.LogManager;
import java.util.function.Function;
import java.util.function.BiFunction;
import java.io.InputStream;

/**
 * The <code>LoggingConfig</code> class sets up the default logging configuration.
 * This class should be named on the Java command line with the system
 * property java.util.logging.config.class to properly initialize the logging system.
 *
 * @author Peter Hollemans
 * @since 0.7
 */
public class LoggingConfig {

  /////////////////////////////////////////////////////////////////

  public LoggingConfig () {

    var file = "logging.properties";
    try (var stream = getClass().getResourceAsStream (file)) {

      var manager = LogManager.getLogManager();
      manager.reset();
      manager.updateConfiguration (stream, null);

    } // try
    catch (Exception e) {
      e.printStackTrace();
    } // catch

  } // LoggingConfig
  
  /////////////////////////////////////////////////////////////////

} // LoggingConfig class
