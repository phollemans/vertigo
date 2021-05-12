/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.Executors;

/**
 * The <code>DaemonThreadFactory</code> creates threads with the daemon mode
 * set so that they don't prevent the Java VM from exiting.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class DaemonThreadFactory implements ThreadFactory {

  private static DaemonThreadFactory instance;
  private ThreadFactory defaultFactory = Executors.defaultThreadFactory();

  public static DaemonThreadFactory getInstance() {
    if (instance == null) instance = new DaemonThreadFactory();
    return (instance);
  } // getInstance

  protected DaemonThreadFactory () {}

  @Override
  public Thread newThread (Runnable r) {
    Thread thread = defaultFactory.newThread (r);
    thread.setDaemon (true);
    return (thread);
  } // newThread

} // DaemonThreadFactory

