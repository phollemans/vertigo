/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.beans.value.ChangeListener;

import java.net.URL;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;

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

  /** The maximum number of surfaces allowed in the cache. */
  private static final int MAX_SURFACES = 8;

  /** The project to access for objects. */
  private Project proj;

  /** The controller for the world view and model. */
  private WorldController worldController;

  /** The cache of surfaces requested. */
  private Map<String, DynamicSurface> surfaceCache;

  /** The cache of surface threads in progress. */
  private Map<String, Thread> surfaceThreadCache;

  /** The surface that is currently active in the view. */
  private DynamicSurface activeSurface;

  /** The active surface hash key. */
  private String activeSurfaceKey;

  /** The surface handler for the project. */
  private GeoSurfaceHandler surfaceHandler;

  /** The listener for progress on the active surface. */
  private ChangeListener<Number> progressListener;

  /** The progress of rendering the active surface in the range [0..1]. */
  private DoubleProperty progressProp;

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the progress property indicating the progress of the active surface
   * updating in the range [0..1] where 0 is not updated at all and 1 is
   * completely updated.
   *
   * @return the progress property.
   *
   * @since 0.6
   */
  public ReadOnlyDoubleProperty progressProperty () { return (progressProp); }

  /////////////////////////////////////////////////////////////////

  protected ProjectController () {

    // We create a surface cache here that can only hold up to some maximum
    // number of entries.  The memory used by each surface will otherwise
    // accumulate and result in an out of memory error.  It would be better
    // if we could monitor the actual memory used by a surface, but we
    // currently have no way to do that and texture images seem to only account
    // for a fraction of the total.
    surfaceCache = new LinkedHashMap<> (16, 0.75f, true) {
      @Override
      protected boolean removeEldestEntry (Map.Entry<String, DynamicSurface> eldest) {
        return (size() > MAX_SURFACES);
      } // removeEldestEntry
    };

    surfaceThreadCache = new HashMap<>();


//    if (LOGGER.isLoggable (Level.FINE)) {
//      var timer = new Timer (true);
//      timer.scheduleAtFixedRate (new TimerTask () {
//          public void run() {
//            if (activeSurface != null) {
//              LOGGER.fine ("Active surface using " + activeSurface.totalMemory()/1024/1024 + " Mb texture memory");
//            } // if
//          } // run
//        }, 10000, 10000
//      );
//    } // if


    // The progress property holds the progress state of whatever surface
    // is currently active.  We need to attach/unattach it as needed.
    progressProp = new SimpleDoubleProperty();
    progressProp.setValue (1);





  } // ProjectController

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the project stored by this controller.
   *
   * @return the project object.
   *
   * @since 0.6
   */
  public Project getProject () { return (proj); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an instance of a project controller for the specified project.
   *
   * @param projectFile the project file to read.
   * @param worldController the world controller to use for project objects.
   * @param surfaceHandler the surface handler to use for showing/hiding surfaces.
   * @param builderList the list of builders for project objects.
   *
   * @return the controller for the project.
   *
   * @throws IOException if an error occurred reading the project file.
   */
  public static ProjectController getInstance (
    URL projectFile,
    WorldController worldController,
    GeoSurfaceHandler surfaceHandler,
    List<ProjectObjectBuilder> builderList
  ) throws IOException {

    // Create the project using a set of builders -- note we specify the
    // palette first so that any palettes are available to the surfaces that
    // come next.
    var controller = new ProjectController();
    controller.proj = Project.readFromXML (projectFile, builderList);
    controller.worldController = worldController;
    controller.surfaceHandler = surfaceHandler;
  
    return (controller);
    
  } // getInstance

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
    var factory = proj.getObject (GeoSurfaceFactory.class, name);
    FutureTask<Void> initTask = new FutureTask<Void> (() -> {
      factory.initialize();
      return (null);
    });
    var initThread = new Thread (initTask);
    initThread.setDaemon (true);
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
    waitThread.setDaemon (true);
    waitThread.start();

  } // initSurface

  /////////////////////////////////////////////////////////////////
  
  /**
   * Clears the active surface.
   *
   * @since 0.6
   */
  public void clearSurface() {
  
    if (activeSurface != null) {
      surfaceHandler.deactivateSurface (activeSurface);
      progressProp.unbind();
      progressProp.setValue (1);
    } // if
    activeSurface = null;

  } // clearSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the specified surface active.
   *
   * @param name the surface to set active.
   */
  private void setActiveSurface (
    DynamicSurface surface
  ) {
  
    if (activeSurface != null) {
      surfaceHandler.deactivateSurface (activeSurface);
      progressProp.unbind();
    } // if
    progressProp.bind (surface.progressProperty());
    surfaceHandler.activateSurface (surface);
    activeSurface = surface;

  } // setActiveSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Shows the surface with the specified name, date/time, and level.
   *
   * @param name the surface name to search for.
   * @param timeIndex the time index for the surface, or -1 if the surface has
   * no times.
   * @param levelIndex the level index for the surface, or -1 if the surface
   * has no levels.
   *
   * @since 0.6
   */
  public void showSurface (
    String name,
    int timeIndex,
    int levelIndex
  ) {

    // Check the factory for the surface.
    var factory = proj.getObject (GeoSurfaceFactory.class, name);
    if (!factory.isInitialized())
      throw new IllegalStateException ("Surface factory for '" + name + "' is not initialized");

    // Create a hash key for the surface request
    String surfaceKey = name + (timeIndex != -1 ? "__T" + timeIndex : "") + (levelIndex != -1 ? "__L" + levelIndex : "");

    // Set the active surface key.  We use this value in a check when
    // creating and activating a surface to make sure that the surface being
    // activated should be the surface that was just created.
    activeSurfaceKey = surfaceKey;

    // If we have the surface cached, just show it immediately.
    var surface = surfaceCache.get (surfaceKey);
    if (surface != null) {
      setActiveSurface (surface);
    } // if

    // If it's not cached and there isn't already a thread creating the surface,
    // start a thread to create it and then show it when it's ready.
    else if (!surfaceThreadCache.containsKey (surfaceKey)) {

      // Create and activate the surface in a background thread.  The process
      // of creating the surface can take a little time, depending on the
      // data source.  If the active surface has been changed before we can
      // create the surface, we don't go through the process of activating it.
      var surfaceThread = new Thread (() -> {
        try {
          var newSurface = factory.createSurface (timeIndex, levelIndex);
          Platform.runLater (() -> {


            surfaceCache.put (surfaceKey, newSurface);


//LOGGER.fine ("Surface cache now contains " + surfaceCache.size() + " surfaces");
//long memory = 0;
//for (var obj : surfaceCache.values()) memory += obj.totalMemory();
//LOGGER.fine ("Using estimated " + memory/1024/1024 + " Mb in surface texture images");




            surfaceThreadCache.remove (surfaceKey);
            if (activeSurfaceKey.equals (surfaceKey)) setActiveSurface (newSurface);
            else LOGGER.warning ("Aborting display of surface '" + name + "' at time index " + timeIndex + ", level index " + levelIndex);
          });
        } // try
        catch (IOException e) {
          LOGGER.log (Level.WARNING, "Surface creation failed for '" + name + "' at time index " + timeIndex + ", level index " + levelIndex, e);
        } // catch
      });
      surfaceThreadCache.put (surfaceKey, surfaceThread);
      LOGGER.fine ("Starting thread for creation of surface '" + name + "' at time index " + timeIndex + ", level index " + levelIndex);
      surfaceThread.setDaemon (true);
      surfaceThread.start();

    } // else if

  } // showSurface

  /////////////////////////////////////////////////////////////////

  /**
   * Shows the area with the specified name.
   *
   * @param name the name of the area to show.
   */
  public void showArea (
    String name
  ) {

    var area = proj.getObject (Area.class, name);
    double xAngle = area.latitude;
    double yAngle = area.longitude+180;
    if (yAngle > 180) yAngle -= 360;
    worldController.zoomTo (xAngle, yAngle, area.extent);
      
  } // getArea

  /////////////////////////////////////////////////////////////////

} // ProjectController class

