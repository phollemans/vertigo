/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.application.Application;
import javafx.application.Application.Parameters;
import javafx.application.Platform;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Priority;
import javafx.scene.Scene;
import javafx.scene.SubScene;

import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import javafx.stage.Stage;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;

import java.net.URL;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>Vertigo</code> class launches the main Vertigo viewer application.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Vertigo extends Application {

  private static final Logger LOGGER = Logger.getLogger (Vertigo.class.getName());

  // Variables
  // ---------

  /** The controller for the world view and model. */
  private WorldController controller;

  /** The scene that shows the world view. */
  private SubScene worldScene;

  /** The runnables to run after the application has started. */
  private List<Runnable> afterLaunch;

  /** The project controller, or null for no project. */
  private ProjectController projController;

  /////////////////////////////////////////////////////////////////

  @Override
  public void init() throws Exception {

    // Process the parameters
    Map<String, String> params = getParameters().getNamed();
    int patches = (
      params.containsKey ("patches") ?
      Integer.parseInt (params.get ("patches")) :
      0
    );
    boolean axes = (
      params.containsKey ("axes") ?
      params.get ("axes").equals ("true") :
      false
    );
    boolean tiling = (
      params.containsKey ("tiling") ?
      params.get ("tiling").equals ("true") :
      false
    );
    boolean demoMode = (
      params.containsKey ("demoMode") ?
      params.get ("demoMode").equals ("true") :
      false
    );
    boolean lineMode = (
      params.containsKey ("lineMode") ?
      params.get ("lineMode").equals ("true") :
      false
    );
    String texture = params.get ("texture");
    String netcdf = params.get ("netcdf");
    String webmap = params.get ("webmap");
    String project = params.get ("project");
    boolean demoProject = (
      params.containsKey ("demoProject") ?
      params.get ("demoProject").equals ("true") :
      false
    );

    // Create the controller
    controller = WorldController.getInstance();
    controller.setLineMode (lineMode);
    worldScene = controller.getView().getScene();
    
    // Add the test objects
    WorldTester tester = WorldTester.getInstance();
    if (patches != 0) tester.addPatches (controller, patches);
    if (axes) tester.addAxes (controller);
    if (tiling) tester.addTiling (controller);

    // Read the project
    URL projectURL = null;
    try {
      if (project != null)
        projectURL = new URL (project);
      else if (demoProject)
        projectURL = getClass().getResource ("demo.vrtx");
      if (projectURL != null)
        projController = ProjectController.getInstance (projectURL);
    } // try
    catch (IOException e) {
      LOGGER.log (Level.WARNING, "Read failed for project " + projectURL, e);
    } // catch

    afterLaunch = new ArrayList<>();
    afterLaunch.add (() -> {

      controller.start();

      if (demoMode) controller.startDemoMode();

      if (texture != null) tester.addDynamicSurface (controller, texture);

      if (netcdf != null) {
        String[] args = netcdf.split (",");
        tester.addNetCDFSurface (controller, args[0], args[1],
          Double.parseDouble (args[2]), Double.parseDouble (args[3]), args[4],
          args[5]);
      } // if
      
      if (webmap != null) {
        String[] args = webmap.split (",");
        tester.addWebMapSurface (controller, args[0], Integer.parseInt (args[1]),
          Integer.parseInt (args[2]), Double.parseDouble (args[3]),
          Double.parseDouble (args[4]));
      } // if

    });
    
  } // init

  /////////////////////////////////////////////////////////////////

  /** Gets a list of surface selection buttons using the project controller. */
  private List<ToggleButton> surfaceButtons () {
  
    var surfaceGroup = new ToggleGroup();
    surfaceGroup.selectedToggleProperty().addListener ((obs, oldToggle, newToggle) -> {
      if (newToggle == null)
        Platform.runLater (() -> surfaceGroup.selectToggle (oldToggle));
      else
        projController.showSurface ((String) newToggle.getUserData());
    });
    List<ToggleButton> buttons = new ArrayList<>();
    for (var name : projController.getSurfaces()) {
      var button = new ToggleButton (name);
      button.setUserData (name);
      button.setToggleGroup (surfaceGroup);
      button.setDisable (true);
      buttons.add (button);
      afterLaunch.add (() -> {
        Consumer<Exception> consumer = except -> {
          if (except == null) button.setDisable (false);
          else LOGGER.log (Level.WARNING, "Failed to initialize surface " + name, except);
        };
        projController.initSurface (name, consumer);
      });
    } // for

    return (buttons);
  
  } // surfaceButtons

  /////////////////////////////////////////////////////////////////

  /** Gets a list of area selection buttons using the project controller. */
  private List<Button> areaButtons () {

    List<Button> buttons = new ArrayList<>();
    for (var name : projController.getAreas()) {
      var button = new Button (name);
      button.setUserData (name);
      button.setOnAction (event -> projController.showArea (name));
      buttons.add (button);
    } // for

    return (buttons);

  } // areaButtons

  /////////////////////////////////////////////////////////////////

//  /** Gets a list of labels explaining the view controls. */
//  private List<Label> controlLabels () {
//
//    List<Label> labels = new ArrayList<>();
//    String[] controls = new String[] {
////      "Scroll ↑↓ to zoom",
////      "Drag to reposition",
////      "Double-click to zoom area",
//      "← ↑ ↓ → to rotate",
//      "A/Z to zoom"
//    };
//    for (var control : controls) labels.add (new Label (control));
//
//    return (labels);
//
//  } // controlLabels

  /////////////////////////////////////////////////////////////////

  /** Gets an on-screen display styled box of nodes. */
  private VBox onScreenBox (
    List<? extends Node> nodes
  ) {

    var box = new VBox();
    box.getStyleClass().add ("vbox-on-screen");

    for (Node node : nodes) {
      node.getStyleClass().add ("on-screen");
      box.getChildren().add (node);
    } // for

    return (box);
  
  } // onScreenBox

  /////////////////////////////////////////////////////////////////

  @Override
  public void start (Stage primaryStage) throws Exception {

    // Create the pane that contains the world scene
    StackPane worldPane = new StackPane (worldScene);
    worldScene.heightProperty().bind (worldPane.heightProperty());
    worldScene.widthProperty().bind (worldPane.widthProperty());
    worldScene.setManaged (false);
    
    // Create the pane that contains the world pane and possibly others
    GridPane gridPane = new GridPane();
    gridPane.add (worldPane, 0, 0);
    gridPane.setVgrow (worldPane, Priority.ALWAYS);
    gridPane.setHgrow (worldPane, Priority.ALWAYS);
        
    // Create the control buttons specified by the project
    if (projController != null) {

      // Create the right side control box showing the datasets to view.
      var nodes = new ArrayList<Node>();
      nodes.add (new Label ("Select dataset:"));
      var surfaceButtons = surfaceButtons();
      for (var button : surfaceButtons) {
        button.setFocusTraversable (false);
        button.setMaxWidth (Double.MAX_VALUE);
        nodes.add (button);
      } // for

      var rightBox = onScreenBox (nodes);
      rightBox.setTranslateX (-25);
      rightBox.setMaxSize (Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
      StackPane.setAlignment (rightBox, Pos.CENTER_RIGHT);
      worldPane.getChildren().add (rightBox);
      
      // Create the left side control box showing the areas of interest.
      nodes.clear();
      nodes.add (new Label ("Select area:"));
      var areaButtons = areaButtons();
      for (var button : areaButtons) {
        button.setFocusTraversable (false);
        button.setMaxWidth (Double.MAX_VALUE);
        nodes.add (button);
      } // for

      var leftBox = onScreenBox (nodes);
      leftBox.setTranslateX (25);
      leftBox.setMaxSize (Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
      StackPane.setAlignment (leftBox, Pos.CENTER_LEFT);
      worldPane.getChildren().add (leftBox);

    } // if

    // Add the help button
    var helpButton = new Button();
    helpButton.setFocusTraversable (false);
    helpButton.setOnAction (event -> showHelp());
    helpButton.getStyleClass().add ("on-screen-help");
    StackPane.setAlignment (helpButton, Pos.BOTTOM_LEFT);
    worldPane.getChildren().add (helpButton);


//    var timeLabel = new javafx.scene.control.Label ("Time:");
//    timeLabel.getStyleClass().addAll ("on-screen");
//    leftBox.getChildren().addAll (timeLabel);
//
//    var timeBox = new javafx.scene.layout.HBox();
//    timeBox.setSpacing (10);
////    timeBox.setMaxWidth (Double.MAX_VALUE);
//
//    var backButton = new javafx.scene.control.Button ("◀︎");
//    backButton.setFocusTraversable (false);
//    backButton.getStyleClass().addAll ("on-screen");
//    timeBox.getChildren().addAll (backButton);
//
//    var timeButton = new javafx.scene.control.Button ("Dec 6, 2020");
//    timeButton.setFocusTraversable (false);
//    timeButton.getStyleClass().addAll ("on-screen");
//    timeBox.getChildren().addAll (timeButton);
//
//    var foreButton = new javafx.scene.control.Button ("▶︎");
//    foreButton.setFocusTraversable (false);
//    foreButton.getStyleClass().addAll ("on-screen");
//    timeBox.getChildren().addAll (foreButton);
//
//    leftBox.getChildren().addAll (timeBox);

    // Add the menu ??
    


    // Set the stage and show
    Scene scene = new Scene (gridPane, 1200, 700);
    scene.getStylesheets().add (getClass().getResource ("styles.css").toExternalForm());

    primaryStage.setScene (scene);
    primaryStage.setTitle ("Vertigo");
    primaryStage.show();

    for (var task : afterLaunch) Platform.runLater (task);
    
  } // start

  /////////////////////////////////////////////////////////////////

  /** Reads and returns text lines from a URL. */
  private String readURL (
    URL location
  ) throws IOException {

    var builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader (new InputStreamReader (location.openStream()))) {
      String line = null;
      while ((line = reader.readLine()) != null) {
        builder.append (line);
        builder.append ("\n");
      } // while
    } // try
    
    return (builder.toString());

  } // readURL

  /////////////////////////////////////////////////////////////////

  /** Shows the help dialog. */
  private void showHelp() {

    // Load the text for the help dialog from a resource file.
    String helpText = null;
    URL location = getClass().getResource ("help.txt");
    if (location == null) LOGGER.warning ("No help file found");
    else {
      try { helpText = readURL (location); }
      catch (IOException e) { LOGGER.log (Level.WARNING, "Error reading help file", e); }
    } // else

    // Create a dialog to show the text.
    if (helpText != null) {

      Dialog dialog = new Dialog();
      dialog.setTitle ("Vertigo Help");

      var area = new TextArea();
      area.setEditable (false);
      area.setPrefRowCount (20);
      area.setText (helpText);

      dialog.getDialogPane().setContent (area);
      dialog.getDialogPane().getButtonTypes().add (ButtonType.CLOSE);
      dialog.showAndWait();

    } // if
    
  } // showHelp

  /////////////////////////////////////////////////////////////////

  public static void main (String[] args) {

    launch (args);
      
  } // main

  /////////////////////////////////////////////////////////////////

} // Vertigo class
