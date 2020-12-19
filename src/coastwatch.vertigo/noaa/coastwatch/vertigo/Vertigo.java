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
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.fxml.FXMLLoader;

import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import java.io.IOException;

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

  @Override
  public void start (Stage primaryStage) throws Exception {

    // Load main FXML file
    FXMLLoader loader = new FXMLLoader (getClass().getResource ("Vertigo.fxml"));
    try { loader.load(); }
    catch (IOException e) {
      LOGGER.log (Level.SEVERE, "Error loading Vertigo Project UI", e);
      throw new IllegalStateException();
    } // try

    // Create and show the scene
    Parent root = loader.getRoot();
    Scene scene = new Scene (root);
    primaryStage.setTitle ("Vertigo Project");
    primaryStage.setScene (scene);
    primaryStage.show();

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

  /** Inserts the Vertigo-specific properties into the system properties. */
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
  
    mergeProperties();
    LOGGER.fine (System.getProperty ("java.vm.name") + " " + System.getProperty ("java.version") + " on " +
      System.getProperty ("os.name") + " (" + System.getProperty ("os.version") +
      ") " + System.getProperty ("os.arch"));
    LOGGER.fine ("JavaFX runtime " + System.getProperty ("javafx.runtime.version"));
    LOGGER.fine ("Starting VM with max memory " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB");

    if (LOGGER.isLoggable (Level.FINE)) {
      var timer = new Timer (true);
      timer.scheduleAtFixedRate (new TimerTask () {
          public void run() {
            var runtime = Runtime.getRuntime();
            long total = runtime.totalMemory()/1024/1024;
            long free = runtime.freeMemory()/1024/1024;
            long used = total - free;
            LOGGER.fine ("Total VM memory " + total + " Mb (" + used + " used + " + free + " free)");
          } // run
        }, 10000, 10000
      );
    } // if
    
    launch (args);
      
  } // main

  /////////////////////////////////////////////////////////////////

} // Vertigo class
