/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Platform;

import java.net.URL;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>ProjectController</code> class holds a project and performs
 * various operations on it.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ProjectController {

  private static final Logger LOGGER = Logger.getLogger (ProjectController.class.getName());

  /** The project to access for objects. */
  private Project proj;

  /** The cache of surfaces requested. */
  private Map<String, DynamicSurface> surfaceCache;

  /** The cache of surface threads in progress. */
  private Map<String, Thread> surfaceThreadCache;

  /** The surface that is currently active in the view. */
  private DynamicSurface activeSurface;

  /** The active surface name. */
  private String activeSurfaceName;

  /////////////////////////////////////////////////////////////////

  protected ProjectController () {
  
    surfaceCache = new HashMap<>();
    surfaceThreadCache = new HashMap<>();
  
  } // ProjectController

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an instance of a project controller for the specified project.
   *
   * @param projectFile the project file to read.
   *
   * @return the controller for the project.
   *
   * @throws IOException if an error occurred reading the project file.
   */
  public static ProjectController getInstance (
    URL projectFile
  ) throws IOException {

    // Create the project using a set of builders -- note we specify the
    // palette first so that any palettes are available to the surfaces that
    // come next.
    List<ProjectObjectBuilder> builderList = List.of (
      new PaletteBuilder(),
      new AreaBuilder(),
      new GeoSurfaceFactoryBuilder (GeoSurfaceHandler.getInstance().getContext())
    );
    var controller = new ProjectController();
    controller.proj = Project.readFromXML (projectFile, builderList);
  
    return (controller);
    
  } // getInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the list of surface name in the project.
   *
   * @return the list of surface names.
   */
  public List<String> getSurfaces () {
  
    return (
      proj.getObjects (GeoSurfaceFactory.class).stream()
      .filter (factory -> factory.getConfigBoolean ("selectable"))
      .map (factory -> factory.getName())
      .collect (Collectors.toList())
    );
  
  } // getSurfaces

  /////////////////////////////////////////////////////////////////

  private GeoSurfaceFactory factoryForName (String name) {
  
    GeoSurfaceFactory factory = proj.getObjects (GeoSurfaceFactory.class).stream()
      .filter (f -> f.getName().equals (name))
      .findFirst().get();

    return (factory);
  
  } // factoryForName

  /////////////////////////////////////////////////////////////////

  /**
   * Initializes the specified surface. A surface cannot be shown using
   * {@link #showSurface} until it is initialized here.
   *
   * @param name the name of the surface to initialize.
   * @param exceptionConsumer the consumer to notify when initialization is
   * complete.  The value sent to the consumer accept() method will be null if
   * no exception occurred initializing the surface.  The consumer accept()
   * method is called on the JavaFX application thread.
   */
  public void initSurface (
    String name,
    Consumer<Exception> exceptionConsumer
  ) {

    // Create and start the task that initializes the factory and
    // possibly throws an exception.
    var factory = factoryForName (name);
    FutureTask<Void> initTask = new FutureTask<Void> (() -> {
      factory.initialize();
      return (null);
    });
    var initThread = new Thread (initTask);
    initThread.start();
    
    // Create and start the wait task that waits for the initialization to
    // finish and then passes the result to the consumer.  We only wait for a
    // certain amount of time.
    var waitThread = new Thread (() -> {
      Exception except = null;
      try { initTask.get (30, TimeUnit.SECONDS); }
      catch (Exception e) { except = e; }
      Exception fExcept = except;
      Platform.runLater (() -> exceptionConsumer.accept (fExcept));
    });
    waitThread.start();

  } // initSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the specified surface active.
   *
   * @param name the surface to set active.
   */
  private void setActiveSurface (
    DynamicSurface surface
  ) {
  
    var handler = GeoSurfaceHandler.getInstance();
    if (activeSurface != null) handler.deactivateSurface (activeSurface);
    handler.activateSurface (surface);
    activeSurface = surface;
  
  } // setActiveSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Shows the surface with the specified name.
   *
   * @param name the surface name to search for.
   */
  public void showSurface (
    String name
  ) {

    // Set the active surface name.  We use this value in a check when
    // creating and activating a surface to make sure that the surface being
    // activated should be the surface that was just created.
    activeSurfaceName = name;

    // If we have the surface cached, just show it immediately.
    var surface = surfaceCache.get (name);
    if (surface != null) {
      setActiveSurface (surface);
    } // if

    // If it's not cached and there isn't already a thread creating the surface,
    // start a thread to create it and then show it when it's ready.
    else if (!surfaceThreadCache.containsKey (name)) {

      // Check the factory for the surface.
      var factory = factoryForName (name);
      if (!factory.isInitialized())
        throw new IllegalStateException ("Surface factory for " + name + " is not initialized");

      // Create and activate the surface in a background thread.  The process
      // of creating the surface can take a little time, depending on the
      // data source.  If the active surface has been changed before we can
      // create the surface, we don't go through the process of activating it.
      var surfaceThread = new Thread (() -> {
        int time = factory.hasTimes() ? factory.closestTimeIndex (factory.getConfigDate ("time")) : 0;
        int level = factory.hasLevels() ? factory.closestLevelIndex (factory.getConfigDouble ("level")) : 0;
        try {
          var newSurface = factory.createSurface (time, level);
          Platform.runLater (() -> {
            surfaceCache.put (name, newSurface);
            surfaceThreadCache.remove (name);
            if (activeSurfaceName.equals (name)) setActiveSurface (newSurface);
            else LOGGER.warning ("Aborting display of surface " + name);
          });
        } // try
        catch (IOException e) {
          LOGGER.log (Level.WARNING, "Surface creation failed for " + name + " at time index " + time + ", level index " + level, e);
        } // catch
      });
      surfaceThreadCache.put (name, surfaceThread);
      surfaceThread.start();

    } // else if

  } // showSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the list of area names in the project.
   *
   * @return the list of area names.
   */
  public List<String> getAreas () {
  
    return (
      proj.getObjects (Area.class).stream()
      .map (area -> area.name)
      .collect (Collectors.toList())
    );
  
  } // getAreas

  /////////////////////////////////////////////////////////////////

  /**
   * Shows the area with the specified name.
   *
   * @param name the name of the area to show.
   */
  public void showArea (
    String name
  ) {

    var area = proj.getObjects (Area.class).stream()
      .filter (a -> a.name.equals (name))
      .findFirst().get();
    
    double xAngle = area.latitude;
    double yAngle = area.longitude+180;
    if (yAngle > 180) yAngle -= 360;
    WorldController.getInstance().zoomTo (xAngle, yAngle, area.extent);
      
  } // getArea

  /////////////////////////////////////////////////////////////////

} // ProjectController class

