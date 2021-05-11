/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import javafx.application.Platform;

import javafx.stage.Stage;
import javafx.stage.Screen;

import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Dimension2D;
import javafx.beans.value.ChangeListener;

import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import java.io.IOException;
import java.util.prefs.Preferences;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>Vertigo</code> class launches the main Vertigo Project data
 * viewing application.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Vertigo extends Application {

  private static final Logger LOGGER = Logger.getLogger (Vertigo.class.getName());

  // The project file parameter, or null for none.
  private String project;

  /////////////////////////////////////////////////////////////////

  @Override
  public void init() throws Exception {

    // Get command line parameters
    Map<String, String> params = getParameters().getNamed();
    project = params.get ("project");
    boolean demoProject = (
      params.containsKey ("demoProject") ?
      params.get ("demoProject").equals ("true") :
      false
    );
    if (project == null && demoProject)
      project = getClass().getResource ("demo.vrtx").toString();

  } // init

  /////////////////////////////////////////////////////////////////

  /**
   * Stores the dimensions in the preferences using the specified key.
   *
   * @param key the key for storing.
   * @param dims the dimensions to store.
   *
   * @since 0.7
   */
  private void storeDims (
    String key,
    Dimension2D dims
  ) {

    Preferences prefs = Preferences.userNodeForPackage (getClass());
    prefs.putInt (key + ".width", (int) dims.getWidth());
    prefs.putInt (key + ".height", (int) dims.getHeight());
  
  } // storeDims

  /////////////////////////////////////////////////////////////////

  /**
   * Recalls the dimensions from the preferences using the specified key.
   *
   * @param key the key for storing.
   *
   * @return the dimensions or null if not available.
   *
   * @since 0.7
   */
  private Dimension2D recallDims (
    String key
  ) {

    Preferences prefs = Preferences.userNodeForPackage (getClass());
    int width = prefs.getInt (key + ".width", -1);
    int height = prefs.getInt (key + ".height", -1);
    Dimension2D dims = (width == -1 || height == -1 ? null : new Dimension2D (width, height));
    
    return (dims);
  
  } // recallDims

  /////////////////////////////////////////////////////////////////

  @Override
  public void start (Stage primaryStage) throws Exception {

    // Report platform and memory
    LOGGER.fine (System.getProperty ("java.vm.name") + " " + System.getProperty ("java.version") + " on " +
      System.getProperty ("os.name") + " (" + System.getProperty ("os.version") +
      ") " + System.getProperty ("os.arch"));
    LOGGER.fine ("JavaFX runtime " + System.getProperty ("javafx.runtime.version"));
    LOGGER.fine ("Starting VM with max memory " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB");

    // Load main FXML file
    FXMLLoader loader = new FXMLLoader (getClass().getResource ("ui/Vertigo.fxml"));
    try { loader.load(); }
    catch (IOException e) {
      LOGGER.log (Level.SEVERE, "Error loading Vertigo Project UI", e);
      throw new IllegalStateException();
    } // try

    // Create the scene
    Parent root = loader.getRoot();
    Scene scene = new Scene (root);
    primaryStage.setTitle ("Vertigo Project");
    primaryStage.setScene (scene);

    // Set the size if available and show
    var window = primaryStage;
    var dims = recallDims ("main.window");
    if (dims != null) {
      window.setWidth (dims.getWidth());
      window.setHeight (dims.getHeight());
    } // if
    primaryStage.show();
    
    // Check and adjust the window size
    LOGGER.fine ("Main application window opened with size " + (int)window.getWidth() + "x" + (int)window.getHeight());

    var screen = Screen.getScreensForRectangle (window.getX(), window.getY(),
      window.getWidth(), window.getHeight()).get (0);
    var bounds = screen.getVisualBounds();
    LOGGER.fine ("Visual screen bounds are " + (int)bounds.getWidth() + "x" + (int)bounds.getHeight());

    if (window.getWidth() > bounds.getWidth()) {
      LOGGER.warning ("Adjusting initial window width to " + (int) bounds.getWidth() + " pixels");
      window.setWidth (bounds.getWidth());
      window.setX (bounds.getMinX());
    } // if
    if (window.getHeight() > bounds.getHeight()) {
      LOGGER.warning ("Adjusting initial window height to " + (int) bounds.getHeight() + " pixels");
      window.setHeight (bounds.getHeight());
      window.setY (bounds.getMinY());
    } // if

    // Set up window size storage
    ChangeListener<Number> storeWindowSize = (obs, oldVal, newVal) -> {
      storeDims ("main.window", new Dimension2D (window.getWidth(), window.getHeight()));
    };
    window.widthProperty().addListener (storeWindowSize);
    window.heightProperty().addListener (storeWindowSize);
    
    // Exit on main window closed ??
//    window.setOnCloseRequest (event -> {
//      Platform.exit();
//    });
// Sometimes we get this on Mac after quitting during development, but we
// don't know why:
//     [exec] Java has been detached already, but someone is still trying to use it at -[GlassViewDelegate dealloc]:/Users/jenkins/workspace/OpenJFX-skara-mac/modules/javafx.graphics/src/main/native-glass/mac/GlassViewDelegate.m:198
//     [exec] 0   libglass.dylib                      0x000000010f11a3c2 -[GlassViewDelegate dealloc] + 290
//     [exec] 1   libglass.dylib                      0x000000010f1200dc -[GlassView3D dealloc] + 252
//     [exec] 2   Foundation                          0x00007fff21323b77 empty + 58
//     [exec] 3   Foundation                          0x00007fff211d07c0 dealloc + 38
//     [exec] 4   Foundation                          0x00007fff211d074b -[NSConcreteMapTable dealloc] + 54
//     [exec] 5   AppKit                              0x00007fff233139da ___NSTouchBarFinderSetNeedsUpdateOnMain_block_invoke_2 + 2221
//     [exec] 6   AppKit                              0x00007fff22d42d38 NSDisplayCycleObserverInvoke + 155
//     [exec] 7   AppKit                              0x00007fff22d428c2 NSDisplayCycleFlush + 937
//     [exec] 8   QuartzCore                          0x00007fff26bee09e _ZN2CA11Transaction19run_commit_handlersE18CATransactionPhase + 92
//     [exec] 9   QuartzCore                          0x00007fff26bece44 _ZN2CA11Transaction6commitEv + 382
//     [exec] 10  AppKit                              0x00007fff22df2272 __62+[CATransaction(NSCATransaction) NS_setFlushesWithDisplayLink]_block_invoke + 285
//     [exec] 11  AppKit                              0x00007fff23541085 ___NSRunLoopObserverCreateWithHandler_block_invoke + 41
//     [exec] 12  CoreFoundation                      0x00007fff20451dad __CFRUNLOOP_IS_CALLING_OUT_TO_AN_OBSERVER_CALLBACK_FUNCTION__ + 23
//     [exec] 13  CoreFoundation                      0x00007fff20451c3d __CFRunLoopDoObservers + 549
//     [exec] 14  CoreFoundation                      0x00007fff204510ed __CFRunLoopRun + 838
//     [exec] 15  CoreFoundation                      0x00007fff204506ce CFRunLoopRunSpecific + 563
//     [exec] 16  libjli.dylib                        0x000000010bcc5641 CreateExecutionEnvironment + 399
//     [exec] 17  libjli.dylib                        0x000000010bcc17d2 JLI_Launch + 1354
//     [exec] 18  java                                0x000000010bcb4ca1 main + 375
//     [exec] 19  libdyld.dylib                       0x00007fff20375621 start + 1

    // Configure controller
    VertigoController controller = loader.getController();
    controller.projectProperty().addListener ((obs, oldVal, newVal) -> {
      primaryStage.setTitle ("Vertigo Project" + (newVal != null ? " - " + newVal : ""));
    });
    Platform.runLater (() -> {
      controller.start();
      if (project != null) {
        controller.openProject (project);
      } // if
    });

  } // start

  /////////////////////////////////////////////////////////////////

  /** Merges the Vertigo-specific properties into the system properties. */
  private static void mergeProperties () {

    // Read the properties
    var file = "vertigo.properties";
    var properties = new Properties();
    try (var stream = Vertigo.class.getResourceAsStream (file)) {
      properties.load (stream);
    } // try
    catch (Exception e) {
      LOGGER.log (Level.WARNING, "Error reading " + file, e);
      properties = null;
    } // catch

    // Add them to the system
    if (properties != null) {
      properties.entrySet().forEach (entry -> {
        System.setProperty ((String)entry.getKey(), (String)entry.getValue());
      });
    } // if

  } // mergeProperties
  
  /////////////////////////////////////////////////////////////////

  public static void main (String[] args) {
  
  
    // To load custom fonts if needed
//    var font = javafx.scene.text.Font.loadFont ("file:/Users/phollema/Downloads/Phosphate.ttc", 10);
//    LOGGER.fine ("font = " + font);
  
  
    mergeProperties();



    // Starts a timer that shows the VM memory usage
//    if (LOGGER.isLoggable (Level.FINE)) {
//      var timer = new Timer (true);
//      timer.scheduleAtFixedRate (new TimerTask () {
//          public void run() {
//            var runtime = Runtime.getRuntime();
//            long total = runtime.totalMemory()/1024/1024;
//            long free = runtime.freeMemory()/1024/1024;
//            long used = total - free;
//            LOGGER.fine ("Total VM memory " + total + " Mb (" + used + " used + " + free + " free)");
//          } // run
//        }, 10000, 10000
//      );
//    } // if
    
    launch (args);
      
  } // main

  /////////////////////////////////////////////////////////////////

} // Vertigo class
