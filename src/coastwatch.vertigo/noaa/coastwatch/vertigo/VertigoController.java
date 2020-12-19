/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.net.URL;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Desktop;
import java.net.URI;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.Date;
import java.util.TimeZone;
import java.util.function.ToDoubleBiFunction;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

import java.text.SimpleDateFormat;

import javafx.scene.control.ComboBox;
import javafx.scene.control.Slider;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ListCell;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

import javafx.scene.input.KeyCombination;

import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import javafx.scene.transform.Scale;

import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;

import javafx.stage.Screen;
import javafx.stage.Modality;
import javafx.stage.FileChooser;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.SnapshotParameters;
import javafx.scene.web.WebView;

import javafx.geometry.Pos;
import javafx.geometry.Dimension2D;
import javafx.geometry.Point3D;

import javafx.scene.shape.Arc;

import javafx.animation.TranslateTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Interpolator;

import javafx.util.Duration;
import javafx.event.ActionEvent;
import javafx.application.Platform;
import javafx.stage.Window;
import javafx.stage.Stage;

import javafx.embed.swing.SwingFXUtils;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.property.StringProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;

import javafx.fxml.FXML;

import noaa.coastwatch.vertigo.ResourceStatus.State;

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.Extension;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>VertigoController</code> class handles the interactions in the
 * main Vertigo window.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class VertigoController {

  private static final Logger LOGGER = Logger.getLogger (VertigoController.class.getName());

  // Menu items

  @FXML
  private Menu fileMenu;

  @FXML
  private CheckMenuItem autoDateTimeMenuItem;

  @FXML
  private CheckMenuItem graticuleMenuItem;

  // Toolbar controls

  @FXML
  private Pane dateTimeControlPane;

  @FXML
  private Pane previousTimestepPane;

  @FXML
  private ComboBox<Date> dateTimeCombo;

  @FXML
  private Pane nextTimestepPane;

  @FXML
  private Pane levelControlPane;

  @FXML
  private ComboBox<Double> levelCombo;

  @FXML
  private Slider zoomSlider;

  // Main view pane
  
  @FXML
  private Pane viewPane;

  // Status bar labels

  @FXML
  private Label indexLabel;

  @FXML
  private Label creditLabel;

  @FXML
  private Label cursorLabel;

  // Progress meter

  @FXML
  private FloatProperty progressProp;
  
  @FXML
  private Arc progressArc;

  @FXML
  private Pane progressMeter;

  @FXML
  private Label progressLabel;

  // Legend

  @FXML
  private Pane legendPane;

  @FXML
  private Pane logoPane;

  // View control

  @FXML
  private Pane viewControlPane;

  @FXML
  private DatasetListPaneController datasetListPaneController;

  @FXML
  private AreaListPaneController areaListPaneController;

  // Details control

  @FXML
  private Pane detailsEditorPane;

  @FXML
  private DatasetSurfacePaneController datasetSurfacePaneController;

  // Navigation buttons

  @FXML
  private Pane navControlPane;
  
  /** The transition for showing/hiding the view controls. */
  private TranslateTransition viewControlTransition;

  /** The transition for showing/hiding the editor controls. */
  private TranslateTransition detailsEditorTransition;

  /** The controller for the world view and model. */
  private WorldController worldController;

  /** The scene that shows the world view. */
  private SubScene worldScene;

  /** The project controller, or null for no project. */
  private ProjectController projController;

  /** The dynamic surface legend (possibly null). */
  private Region surfaceLegend;

  /** The format of dates in the date/time combo. */
  private static SimpleDateFormat dateFormat;

  /** The units of measure for the current level combo, or null for none. */
  private String levelUnits;

  /** The last active date (for use in automatic date/time mode). */
  private Date lastActiveDate;

  /** The map of surface to time index (use in non-automatic date/time mode). */
  private Map<String, Integer> surfaceTimeIndexMap = new HashMap<>();

  /** The map of surface to level index. */
  private Map<String, Integer> surfaceLevelIndexMap = new HashMap<>();

  /** The function for measureing distance between dates. */
  private static ToDoubleBiFunction<Date,Date> dateDistFunc;
  
  /** The function for measureing distance between levels. */
  private static ToDoubleBiFunction<Double,Double> levelDistFunc;

  /** The flag that indicates the surface is currently being changed. */
  private boolean surfaceIsAdjusting;

  /** The currently open project file. */
  private StringProperty projectProp = new SimpleStringProperty (this, "project", null);

  /** The preferred width of the logo pane. */
  private Double prefLogoPaneWidth;

  /** The list of builders for project objects. */
  private List<ProjectObjectBuilder> projectObjectBuilderList;

  /** The handler for showing/hiding surface for this controller. */
  private GeoSurfaceHandler surfaceHandler;

  /** The help dialog. */
  private Dialog helpDialog;

  /** The about dialog. */
  private Dialog aboutDialog;

  /////////////////////////////////////////////////////////////////

  static {
    dateFormat = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
    dateFormat.setTimeZone (TimeZone.getTimeZone ("UTC"));
    dateDistFunc = (a,b) -> Math.abs (a.getTime() - b.getTime());
    levelDistFunc = (a,b) -> Math.abs (a-b);
  } // static

  /////////////////////////////////////////////////////////////////

  /** Opens a browser location. */
  public static void openBrowser (String location) {
  
    try {
//    if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported (Desktop.Action.BROWSE)) {
      Desktop.getDesktop().browse (new URI (location));
    } // try
    catch (Exception e) {
      LOGGER.log (Level.WARNING, "Cannot open browser location " + location, e);
    } // catch

  } // openBrowser
  
  /////////////////////////////////////////////////////////////////

  /**
   * Gets the project file property (default null).
   *
   * @return the project file property.
   */
  public ReadOnlyStringProperty projectProperty () { return (projectProp); }

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleViewControlButton (ObservableValue value, Boolean oldVal, Boolean newVal) {
  
    viewControlTransition.setToX (newVal ? 0 : -viewControlPane.getWidth());
    viewControlTransition.play();
  
  } // handleViewControlButton

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleDetailsEditorButton (ObservableValue value, Boolean oldVal, Boolean newVal) {
  
    detailsEditorTransition.setToX (newVal ? 0 : detailsEditorPane.getWidth());
    detailsEditorTransition.play();
  
  } // handleDetailsEditorButton

  /////////////////////////////////////////////////////////////////

  /** Gets the current stage for the view pane. */
  private Stage getStage() { return ((Stage) viewPane.getScene().getWindow()); }

  /////////////////////////////////////////////////////////////////

  /** Gets the current screen for the view pane. */
  private Screen getScreen() {
  
    var stage = getStage();
    var screen = Screen.getScreensForRectangle (stage.getX(), stage.getY(),
      stage.getWidth(), stage.getHeight()).get (0);
  
    return (screen);

  } // getScreen
  
 /////////////////////////////////////////////////////////////////

  @FXML
  private void handleSnapshotMenuItem () {

    // Take the snapshot.  We do this using a scale transform that accounts for
    // high DPI displays.  The image then captures the true view of the display
    // as seen by the user.
    var screen = getScreen();
    double scaleX = screen.getOutputScaleX();
    double scaleY = screen.getOutputScaleY();
    int width = (int) (viewPane.getWidth() * scaleX);
    int height = (int) (viewPane.getHeight() * scaleY);
    var image = new WritableImage (width, height);
    var params = new SnapshotParameters();
    params.setTransform (new Scale (scaleX, scaleY, 0, 0));

    navControlPane.setVisible (false);
    updateCursorLabel (null);
    viewPane.snapshot (params, image);
    navControlPane.setVisible (true);

    // Generate a useful default file name
    var defaultFormat = "jpg";
    String dataset = datasetListPaneController.selectedDatasetProperty().getValue().getName();
    if (dataset == null) dataset = "";
    String dateTime = dateTimeCombo.isDisabled() ? "" : formatDateItem (dateTimeCombo.getValue());
    String level = levelCombo.isDisabled() ? "" : formatLevelItem (levelCombo.getValue());

    String name = dataset + " " + dateTime + " " + level;
    name = name.trim();
    if (name.length() == 0) name = "Untitled";
    name = name.replaceAll ("\\/", "-").replaceAll (":", ".") + "." + defaultFormat;

    // Create and show the file chooser
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle ("Save Snapshot");
    fileChooser.setInitialFileName (name);
    File file = fileChooser.showSaveDialog (getStage());

    if (file != null) {
      try {

        // Get format from the file extension
        int index = file.toString().lastIndexOf ('.');
        String format = (index == -1 ? defaultFormat : file.toString().substring (index+1));

        // Convert and flatten the image
        var bufferedImage = SwingFXUtils.fromFXImage (image, null);
        bufferedImage = flatten (bufferedImage);

        // Write to the file
        var status = ImageIO.write (bufferedImage, format, file);
        if (!status) throw new IOException ("No image writer found for format '" + format + "'");

      } catch (IOException e) {
        LOGGER.log (Level.WARNING, "Failed to write snapshow image to file " + file, e);
        showError ("Failed to write snapshot image to file " + file, e);
      } // try
    } // if

  } // handleSnapshotMenuItem

  /////////////////////////////////////////////////////////////////

  /** Shows an error alert dialog. */
  private void showError (String message, Exception e) {
  
    var alert = new Alert (Alert.AlertType.ERROR, message + ".  " + e.getMessage() + ".");
    var sheets = viewPane.getScene().getRoot().getStylesheets();
    alert.getDialogPane().getStylesheets().addAll (sheets);
    alert.initOwner (getStage());
    alert.showAndWait();
  
  } // showError

  /////////////////////////////////////////////////////////////////

  /** Flattens a buffered image from ARGB to RGB. */
  private static BufferedImage flatten (BufferedImage image) {

    BufferedImage flatImage = new BufferedImage (image.getWidth(), image.getHeight(),
      BufferedImage.OPAQUE);
    var graphics = flatImage.createGraphics();
    graphics.drawImage (image, 0, 0, null);
    graphics.dispose();
    
    return (flatImage);

  } // flatten
  
  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleFileOpenMenuItem () {

    // Create and show the file chooser
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle ("Open Project");
    File file = fileChooser.showOpenDialog (getStage());

    // Open the project
    if (file != null) { openProject (file.toURI().toString()); }

  } // handleFileOpenMenuItem
  
  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleFileCloseMenuItem () {

    projectProp.setValue (null);
    clearSurface();
    datasetListPaneController.clear();
    areaListPaneController.clear();
    projController = null;
    progressProp.unbind();
    progressProp.setValue (1);

  } // handleFileCloseMenuItem

   /////////////////////////////////////////////////////////////////

  @FXML
  private void handleViewSizeMenuItem (ActionEvent event) {

    var stage = getStage();
    if (!stage.isFullScreen()) {

      var item = (MenuItem) event.getSource();
      Dimension2D dims = (Dimension2D) item.getUserData();

      double newWidth = stage.getWidth() + (dims.getWidth() - viewPane.getWidth());
      double newHeight = stage.getHeight() + (dims.getHeight() - viewPane.getHeight());

      resizeWindow (stage, newWidth, newHeight);

    } // if
    
  } // handleViewSizeMenuItem

  /////////////////////////////////////////////////////////////////

  /** Performs an animated resizing of a window width and height. */
  private void resizeWindow (
    Window window,
    double newWidth,
    double newHeight
  ) {

    // Check request against visual bounds
    var screen = Screen.getScreensForRectangle (window.getX(), window.getY(),
      window.getWidth(), window.getHeight()).get (0);
    var bounds = screen.getVisualBounds();
    
    if (newWidth > bounds.getWidth()) {
      newWidth = bounds.getWidth();
      LOGGER.warning ("Truncating new window width at " + newWidth);
    } // if

    if (newHeight > bounds.getHeight()) {
      newHeight = bounds.getHeight();
      LOGGER.warning ("Truncating new window height at " + newHeight);
    } // if
    
    // Perform window size change
    var widthProperty = new SimpleDoubleProperty (window.getWidth());
    widthProperty.addListener ((obs, oldVal, newVal) -> {
      double width = Math.round (newVal.doubleValue());
      if (width != window.getWidth()) Platform.runLater (() -> window.setWidth (width));
    });

    var heightProperty = new SimpleDoubleProperty (window.getHeight());
    heightProperty.addListener ((obs, oldVal, newVal) -> {
      double height = Math.round (newVal.doubleValue());
      if (height != window.getHeight()) Platform.runLater (() -> window.setHeight (height));
    });

    animateDoubleProperty (widthProperty, newWidth);
    animateDoubleProperty (heightProperty, newHeight);

  } // resizeWindow

  /////////////////////////////////////////////////////////////////

  /** Animates a double property value to a new target value. */
  private void animateDoubleProperty (
    DoubleProperty prop,
    double target
  ) {

    var keyValue = new KeyValue (prop, target, Interpolator.EASE_BOTH);
    var frame = new KeyFrame (Duration.millis (500), keyValue);
    var timeline = new Timeline (frame);
    timeline.play();

  } // animateDoubleProperty

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleDarkModeMenuItem (ActionEvent event) {
    
    var item = (CheckMenuItem) event.getSource();
    var sheets = viewPane.getScene().getRoot().getStylesheets();

    String sheet = item.isSelected() ? "vertigo_dark.css" : "vertigo_light.css";
    sheets.set (0, getClass().getResource (sheet).toExternalForm());
    
  } // handleFullScreenMenuItem

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleFullScreenMenuItem () {
  
    var stage = getStage();
    stage.setFullScreenExitHint ("");
    stage.setFullScreen (!stage.isFullScreen());

  } // handleFullScreenMenuItem

  /////////////////////////////////////////////////////////////////

  /** Updates the label showing the currently selected date/time and level. */
  private void updateIndexLabel () {

    String dateTime = dateTimeCombo.isDisabled() ? "" : "Date: " +
      formatDateItem (dateTimeCombo.getValue()).replaceFirst (" ", "   |   Time: ");
    String level = levelCombo.isDisabled() ? "" : "Level: " + formatLevelItem (levelCombo.getValue());
    String text = dateTime + (dateTime.length() != 0 && level.length() != 0 ? "   |   " : "") + level;
    indexLabel.setText (text);
  
  } // updateIndexLabel

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleDateTimeValueChange (ObservableValue obs, Date oldVal, Date newVal) {
  
    if (!surfaceIsAdjusting) {
      if (newVal != null) {

        String name = datasetListPaneController.selectedDatasetProperty().getValue().getName();
        int timeIndex = dateTimeCombo.getSelectionModel().getSelectedIndex();

        var itemCount = dateTimeCombo.getItems().size();
        previousTimestepPane.setDisable (timeIndex == 0);
        nextTimestepPane.setDisable (timeIndex == itemCount-1);

        int levelIndex = levelCombo.isDisabled() ? -1 : levelCombo.getSelectionModel().getSelectedIndex();
        surfaceTimeIndexMap.put (name, timeIndex);
        lastActiveDate = newVal;
        projController.showSurface (name, timeIndex, levelIndex);
        updateIndexLabel();
        LOGGER.fine ("Date/time updated to " + dateFormat.format (newVal) + " UTC at index " + timeIndex);

      } // if
    } // if

  } // handleDateTimeValueChange

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleLevelValueChange (ObservableValue obs, Double oldVal, Double newVal) {
  
    if (!surfaceIsAdjusting) {
      if (newVal != null) {
        String name = datasetListPaneController.selectedDatasetProperty().getValue().getName();
        int timeIndex = dateTimeCombo.isDisabled() ? -1 : dateTimeCombo.getSelectionModel().getSelectedIndex();
        int levelIndex = levelCombo.getSelectionModel().getSelectedIndex();
        surfaceLevelIndexMap.put (name, levelIndex);
        projController.showSurface (name, timeIndex, levelIndex);
        updateIndexLabel();
        LOGGER.fine ("Level updated to " + newVal + " at index " + levelIndex);
      } // if
    } // if

  } // handleLevelValueChange

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleNavAction (ActionEvent event) {

    var source = event.getSource();
    String actionType;
    if (source instanceof MenuItem) actionType = (String) ((MenuItem) source).getUserData();
    else if (source instanceof Node) actionType = (String) ((Node) source).getUserData();
    else throw new IllegalStateException();
    if (actionType.equals ("UP")) worldController.shiftNorth();
    if (actionType.equals ("DOWN")) worldController.shiftSouth();
    if (actionType.equals ("LEFT")) worldController.shiftWest();
    if (actionType.equals ("RIGHT")) worldController.shiftEast();
    if (actionType.equals ("ZOOM_IN")) worldController.zoomIn();
    if (actionType.equals ("ZOOM_OUT")) worldController.zoomOut();

  } // handleNavAction

  /////////////////////////////////////////////////////////////////

  /** Initializes the controller. */
  public void initialize () {

    // Set up transition for view control pane
    viewControlTransition = new TranslateTransition (Duration.millis (350), viewControlPane);
    viewControlPane.translateXProperty().addListener ((obs, oldVal, newVal) -> {
      AnchorPane.setLeftAnchor (viewPane, viewControlPane.getWidth() + newVal.doubleValue());
    });

    // Set up transition for details editor pane
    detailsEditorTransition = new TranslateTransition (Duration.millis (350), detailsEditorPane);
    detailsEditorPane.translateXProperty().addListener ((obs, oldVal, newVal) -> {
      AnchorPane.setRightAnchor (viewPane, detailsEditorPane.getWidth() - newVal.doubleValue());
    });
    
    // Create main view
    worldController = new WorldController();
    var worldView = worldController.getView();
    worldScene = worldView.getScene();
    viewPane.getChildren().add (0, worldScene);

    worldScene.heightProperty().bind (viewPane.heightProperty());
    worldScene.widthProperty().bind (viewPane.widthProperty());
    worldScene.setManaged (false);

    // Connect the zoom slider
    zoomSlider.setMin (worldView.minZoomLevel());
    zoomSlider.setMax (worldView.maxZoomLevel());
    worldView.cameraZoomProperty().bindBidirectional (zoomSlider.valueProperty());

    // Set up cursor label updating
    worldController.globeIntersectProperty().addListener ((obs, oldVal, newVal) -> updateCursorLabel (newVal));

    // Set up dataset list selection
    datasetListPaneController.selectedDatasetProperty().addListener ((obs, oldVal, newVal) -> {
      if (newVal != null) {

        // Show the surface
        var status = datasetListPaneController.getStatusMap().get (newVal);
        if (status != null && status.getState() == State.AVAILABLE) {
          showSurface (newVal.getName());
        } // if

        // Send the surface to the details editor
        var factory = newVal;
        if (factory instanceof DatasetSurfaceFactory)
          datasetSurfacePaneController.factoryProperty().setValue ((DatasetSurfaceFactory) factory);
        else
          datasetSurfacePaneController.factoryProperty().setValue (null);

      } // if
    });
    datasetListPaneController.setOnDatasetInfoRequest (event -> {
      openBrowser (event.getDataset().getSourceUrl());    
    });

    // Create list of builders for project objects
    var areaBuilder = new AreaBuilder();
    surfaceHandler = new GeoSurfaceHandler (worldController);
    var context = surfaceHandler.getContext();
    var datasetBuilder = new DatasetSurfaceFactoryBuilder (context);
    var webMapBuilder = new WebMapSurfaceFactoryBuilder (context);
    projectObjectBuilderList = List.of (areaBuilder, datasetBuilder, webMapBuilder);

    // Set up dataset surface controller
    datasetSurfacePaneController.builderProperty().setValue (datasetBuilder);
    datasetSurfacePaneController.setOnBuild (event -> {
      var name = datasetListPaneController.selectedDatasetProperty().getValue();
      datasetBuilder.setProperty ("name", name);
      datasetBuilder.setProperty ("config.selectable", true);
      datasetBuilder.setProperty ("config.layer", 0);
      datasetBuilder.setProperty ("config.time", new Date());
      datasetBuilder.setProperty ("config.level", 0);
      var factory = datasetBuilder.getObject();
//      datasetSurfacePaneController.factoryProperty().setValue ((DatasetSurfaceFactory) factory);



// The issue here is that the project controller holds onto a cache of
// dynamic surfaces that have already been created using this factory.  Just
// setting the factory has no effect, we also need to clear out the cache of
// surfaces created.  We also should probably come up with a way of updating
// a surface factory.  For example, if the variable changes we need to
// re-initialize the factory but can re-use it.  If the color scale changes,
// we only need to update the color converter and we can re-use the factory.
// In either case, we still need to clear the cache of surfaces created
// because they won't match the new settings.  One idea is to create a copy
// method in the DatasetSurfaceFactory that copies over certain parts of
// a factory that can be re-used.
      projController.getProject().addObject (factory);


      initSurface (name);




    });

    // Set up area list selection
    areaListPaneController.setOnAreaActivated (event -> {
      var area = areaListPaneController.getSelectedArea();
      if (area != null) projController.showArea (area);
    });
//    areaList.getSelectionModel().selectedItemProperty().addListener ((obs, oldVal, newVal) -> {
//      if (newVal != null) {
//        selectedObjectProp.setValue (projController.getProject().getObject (Area.class, newVal));
//      } // if
//    });

    // Set up date/time combo
    dateTimeCombo.setCellFactory (combo -> new CustomListCell<Date> (this::formatDateItem));
    dateTimeCombo.setButtonCell (new CustomListCell<Date> (this::formatDateItem));

    // Set up level combo
    levelCombo.setCellFactory (combo -> new CustomListCell<Double> (this::formatLevelItem));
    levelCombo.setButtonCell (new CustomListCell<Double> (this::formatLevelItem));

    // Set up graticule menu item
    worldController.graticuleVisibleProperty().bind (graticuleMenuItem.selectedProperty());

    // Add a Quit item if not on the Mac
    if (System.getProperty ("os.name").toLowerCase().indexOf ("mac") == -1) {
      var quitItem = new MenuItem ("Quit Vertigo");
      quitItem.setOnAction (event -> Platform.exit());
      quitItem.setAccelerator (KeyCombination.valueOf ("Shortcut+Q"));
      fileMenu.getItems().addAll (new SeparatorMenuItem(), quitItem);
    } // if

    // Set up the progress meter.  When the progress gets to 100%, we create
    // a fade transition to fully transparent and then hide the meter.
    progressArc.lengthProperty().bind (progressProp.multiply (-360));
    var progressFade = new FadeTransition (Duration.millis (300), progressMeter);
    progressFade.setFromValue (0.6);
    progressFade.setToValue (0);
    progressFade.setOnFinished (event -> progressMeter.setVisible (false));
    progressProp.addListener ((obs, oldVal, newVal) -> {
      if (newVal.doubleValue() == 1) {
        if (progressMeter.isVisible()) progressFade.play();
      } // if
      else {
        if (!progressMeter.isVisible()) {
          progressMeter.setOpacity (0.6);
          progressMeter.setVisible (true);
        } // if
        else {
          progressFade.stop();
          progressMeter.setOpacity (0.6);
        } // else
      } // else
    });
    
    // Set up a detector for when the progress of updating the view stalls
    var stallDetector = new ProgressStallDetector();
    progressProp.addListener ((obs, oldVal, newVal) -> stallDetector.progressChanged (newVal.doubleValue()));

    progressProp.setValue (1);

  } // initialize

  /////////////////////////////////////////////////////////////////

  /** Detects if the progress property has stalled waiting for new progress. */
  private class ProgressStallDetector {
  
    private ScheduledExecutorService scheduler;
    private Runnable stallTask;
    private Future stallTaskFuture;
    private boolean progressStalled;

    public ProgressStallDetector () {
      scheduler = Executors.newScheduledThreadPool (1, DaemonThreadFactory.getInstance());
      stallTask = () -> {
        Platform.runLater (() -> {
          progressStalled = true;
          updateProgressLabel();
        });
      };
    } // ProgressStallDetector

    public void progressChanged (double progress) {
      progressStalled = false;
      if (stallTaskFuture != null) {
        stallTaskFuture.cancel (false);
        stallTaskFuture = null;
      } // if
      if (progress != 1) {
        stallTaskFuture = scheduler.schedule (stallTask, 5, TimeUnit.SECONDS);
      } // if
      updateProgressLabel();
    } // progressChanged

    private void updateProgressLabel () {
      if (progressStalled) {
        progressLabel.setText ("Waiting for server response ...");
        progressLabel.setVisible (true);
      } // if
      else {
        progressLabel.setVisible (false);
      } // else
    } // updateProgressLabel

  } // ProgressStallDetector

  /////////////////////////////////////////////////////////////////

  /**
   * Finds the closest value in a list to a specified value.
   *
   * @param itemList the list of items to search.
   * @param value the value to search for.
   * @param func the distance function to compare values.
   *
   * @return the index into the list of item whose value is closest to that
   * requested, or -1 if the list is empty.
   *
   * @since 0.6
   */
  public static <T> int closestIndex (
    List<T> itemList,
    T value,
    ToDoubleBiFunction<T,T> func
  ) {
  
    double minDiff = Double.MAX_VALUE;
    int index = -1;
    for (int i = 0; i < itemList.size(); i++) {
      double diff = func.applyAsDouble (itemList.get (i), value);
      if (diff < minDiff) { minDiff = diff; index = i; }
      if (diff == 0) break;
    } // for

    return (index);

  } // closestIndex

  /////////////////////////////////////////////////////////////////

  /** Shows the specified surface and updates the combo boxes and legend. */
  private void showSurface (String name) {

    surfaceIsAdjusting = true;
    var factory = getSurfaceFactory (name);

    // Determine which time index we should use and update the date/time
    // controls.
    int timeIndex = -1;
    if (factory.hasTimes()) {

      var timeList = factory.getTimes();

      // In automatic date/time mode, we pick the closest time index to the last
      // known active date.
      if (autoDateTimeMenuItem.isSelected()) {
        if (lastActiveDate == null) lastActiveDate = factory.getConfigDate ("time");
        timeIndex = closestIndex (timeList, lastActiveDate, dateDistFunc);
      } // if
      else {

        // In non-automatic mode, we pick the time index from the last time
        // we looked at this surface.  If that's not available, we pick the
        // time index that the surface was configured to start with.
        var index = surfaceTimeIndexMap.get (name);
        if (index == null) {
          Date date = factory.getConfigDate ("time");
          index = closestIndex (timeList, date, dateDistFunc);
        } // if
        timeIndex = index;
      
      } // else

      // Update the date/time controls.
      dateTimeCombo.setItems (FXCollections.observableList (timeList));
      dateTimeCombo.getSelectionModel().select (timeIndex);
      surfaceTimeIndexMap.put (name, timeIndex);
      lastActiveDate = timeList.get (timeIndex);
      dateTimeControlPane.setDisable (false);

      var itemCount = dateTimeCombo.getItems().size();
      previousTimestepPane.setDisable (timeIndex == 0);
      nextTimestepPane.setDisable (timeIndex == itemCount-1);

    } // if
    else {
      dateTimeCombo.setValue (null);
      dateTimeControlPane.setDisable (true);
    } // else

    // Determine which level index we should use and update the level controls.
    int levelIndex = -1;
    if (factory.hasLevels()) {

      var levelList = factory.getLevels();

      var index = surfaceLevelIndexMap.get (name);
      if (index == null) {
        Double level = factory.getConfigDouble ("level");
        index = closestIndex (levelList, level, levelDistFunc);
      } // if
      levelIndex = index;

      levelUnits = factory.getLevelUnits();
      levelCombo.setItems (FXCollections.observableList (levelList));
      levelCombo.getSelectionModel().select (levelIndex);
      surfaceLevelIndexMap.put (name, levelIndex);
      levelControlPane.setDisable (false);

    } // if
    else {
      levelUnits = null;
      levelCombo.setValue (null);
      levelControlPane.setDisable (true);
    } // else
    
    // Show the surface in the view and update the legend.
    LOGGER.fine ("Showing '" + name + "' at time index " + timeIndex + ", level index " + levelIndex);
    projController.showSurface (name, timeIndex, levelIndex);
    updateSurfaceLegend (name);
    String credit = factory.getCredit();
    creditLabel.setText (credit == null ? null : "Data: " + credit);
    updateIndexLabel();

    surfaceIsAdjusting = false;

  } // showSurface

  /////////////////////////////////////////////////////////////////

  /** Clears the active surface and resets the combo boxes and legend. */
  private void clearSurface () {

    surfaceIsAdjusting = true;

    projController.clearSurface();
    updateTimes (null);
    updateLevels (null);
    updateSurfaceLegend (null);
    creditLabel.setText (null);
    updateIndexLabel();

    surfaceIsAdjusting = false;

  } // clearSurface

  /////////////////////////////////////////////////////////////////

  /** Updates the cursor position label. */
  private void updateCursorLabel (Point3D point) {

    if (point != null) {
      double[] latLon = new double[2];
      var trans = worldController.coordTransProperty().getValue();
      trans.translate (point.getX(), point.getY(), point.getZ(), latLon);
      cursorLabel.setText (
        SphereFunctions.degMinSec (latLon[0]) + (latLon[0] < 0 ? "S" : "N") + "   " +
        SphereFunctions.degMinSec (latLon[1]) + (latLon[1] < 0 ? "W" : "E")
      );
    } // if
    else {
      cursorLabel.setText (" ");
    } // else

  } // updateCursorLabel

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handlePreviousTimestepButton () {

    dateTimeCombo.getSelectionModel().selectPrevious();

  } // handlePreviousTimestepButton
  
  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleNextTimestepButton () {

    dateTimeCombo.getSelectionModel().selectNext();
  
  } // handleNextTimestepButton

  /////////////////////////////////////////////////////////////////

  /**
   * Starts this controller.  This method should be called prior to any
   * other method.
   */
  public void start () {
  
    worldController.start();
  
  } // start

  /////////////////////////////////////////////////////////////////

  /**
   * Opens and applies the specified project.  If the project cannot be opened,
   * no operation is performed and an alert message is shown.
   *
   * @param project the project URL string to open.
   */
  public void openProject (String project) {

    LOGGER.fine ("Opening project at " + project);

    // Read the project file
    ProjectController newProjController = null;
    try {
      var projectURL = new URL (project);
      newProjController = ProjectController.getInstance (projectURL, worldController,
        surfaceHandler, projectObjectBuilderList);
      projectProp.setValue (projectURL.getFile().replaceFirst ("^.*/([^/]+)$", "$1"));
    } // try
    catch (IOException e) {
      LOGGER.log (Level.WARNING, "Failed to read project file " + project, e);
      showError ("Failed to read project file " + project, e);
    } // catch

    if (newProjController != null) {

      if (projController != null) clearSurface();
      projController = newProjController;

      // Set up dataset list
//      var surfaces = projController.getProject().getObjectNames (GeoSurfaceFactory.class);
//      var datasetItems = FXCollections.observableList (surfaces);
//      datasetListPaneController.setDatasets (datasetItems);
      var surfaces = projController.getProject().getObjects (GeoSurfaceFactory.class);
      datasetListPaneController.setDatasets (surfaces);
      for (var surface : surfaces) initSurface (surface);

      // Set up area list
//      var areas = projController.getProject().getObjectNames (Area.class);
//      var areaItems = FXCollections.observableList (areas);
//      areaListPaneController.setAreas (areaItems);
      var areas = projController.getProject().getObjects (Area.class);
      areaListPaneController.setAreas (areas);

      // Link the progress property to the new project controller
      progressProp.bind (projController.progressProperty());

    } // if
  
  } // openProject

  /////////////////////////////////////////////////////////////////

  /** Initializes the specified surface. */
  private void initSurface (GeoSurfaceFactory surface) {

    datasetListPaneController.getStatusMap().put (surface, ResourceStatus.unavailable());
    Consumer<Exception> consumer = except -> {
      ResourceStatus status;
      if (except == null) { status = ResourceStatus.available(); }
      else {
        status = ResourceStatus.error (except);
        LOGGER.log (Level.WARNING, "Failed to initialize surface '" + surface + "'", except);
      } // if
      datasetListPaneController.getStatusMap().put (surface, status);
    };
    projController.initSurface (surface.getName(), consumer);

  } // initSurface
  
  /////////////////////////////////////////////////////////////////

  /** Formats a date for display in a list cell. */
  private String formatDateItem (Date item) {
  
    return (dateFormat.format (item) + " UTC");
  
  } // formatDateItem

  /////////////////////////////////////////////////////////////////

  /** Formats a level for display in a list cell. */
  private String formatLevelItem (Double item) {

    String text = item.toString();
    if (text.endsWith (".0")) text = text.substring (0, text.length()-2);
    if (levelUnits != null) text += " " + levelUnits;
  
    return (text);
  
  } // formatLevelItem

  /////////////////////////////////////////////////////////////////

  /** Displays a list cell formatted using a custom formatter. */
  private class CustomListCell<T> extends ListCell<T> {

    private Function<T,String> formatter;

    public CustomListCell (Function<T,String> formatter) { this.formatter = formatter; }

    @Override
    protected void updateItem (T item, boolean empty) {

      super.updateItem (item, empty);
      if (empty || item == null) {
        setText (null);
        setGraphic (null);
      } // if
      else {
        setText (formatter.apply (item));
        setGraphic (null);
      } // if

    } // updateItem

  } // CustomListCell

  /////////////////////////////////////////////////////////////////

  /** Gets the named surface factory from the project. */
  private GeoSurfaceFactory getSurfaceFactory (String name) {
    
    return (projController.getProject().getObject (GeoSurfaceFactory.class, name));
    
  } // getSurfaceFactory

  /////////////////////////////////////////////////////////////////

  /**
   * Updates the legend for the specified surface as needed.
   *
   * @param name the name of the surface to update the legend, or null to
   * remove the legend.
   */
  private void updateSurfaceLegend (
    String name
  ) {

    var duration = Duration.millis (300);

    // Remove old legend
    if (surfaceLegend != null) {
      var oldSurfaceLegend = surfaceLegend;
      var fade = new FadeTransition (duration, oldSurfaceLegend);
      fade.setFromValue (1);
      fade.setToValue (0);
      fade.setOnFinished (event -> legendPane.getChildren().remove (oldSurfaceLegend));
      fade.play();
    } // if

    // Add new legend
    Region legend = (name != null ? getSurfaceFactory (name).getLegend() : null);
    if (legend != null) {
    
      legend.getStyleClass().add ("legend");
      legend.setMaxWidth (Region.USE_PREF_SIZE);
      legend.setMaxHeight (Region.USE_PREF_SIZE);
      legend.setOpacity (0);

      StackPane.setAlignment (legend, Pos.CENTER);
      legendPane.getChildren().add (legend);

      var fade = new FadeTransition (duration, legend);
      fade.setFromValue (0);
      fade.setToValue (1);
      fade.play();

    } // if

    surfaceLegend = legend;

  } // updateSurfaceLegend

  /////////////////////////////////////////////////////////////////

  /**
   * Updates the date/time combo for the specified surface.
   *
   * @param name the name of the surface to update the date/time combo, or null
   * to reset the date/time combo.
   */
  private void updateTimes (
    String name
  ) {

    var factory = (name == null ? null : getSurfaceFactory (name));
    if (factory != null && factory.hasTimes()) {
      dateTimeCombo.setItems (FXCollections.observableList (factory.getTimes()));
      dateTimeControlPane.setDisable (false);
    } // if
    else {
      dateTimeCombo.setValue (null);
      dateTimeControlPane.setDisable (true);
    } // else

  } // updateTimes

  /////////////////////////////////////////////////////////////////

  /**
   * Updates the level combo for the specified surface.
   *
   * @param name the name of the surface to update the level combo.
   */
  private void updateLevels (
    String name
  ) {

    var factory = (name == null ? null : getSurfaceFactory (name));
    if (factory != null && factory.hasLevels()) {
      levelUnits = factory.getLevelUnits();
      levelCombo.setItems (FXCollections.observableList (factory.getLevels()));
      levelControlPane.setDisable (false);
    } // if
    else {
      levelUnits = null;
      levelCombo.setValue (null);
      levelControlPane.setDisable (true);
    } // else
    
  } // updateLevels

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

  /** Translates the specified Markdown text into HTML. */
  private String translateMarkdown (String mdText) {

    List<Extension> extensions = List.of (TablesExtension.create());
    Parser parser = Parser.builder().extensions (extensions).build();
    var document = parser.parse (mdText);
    HtmlRenderer renderer = HtmlRenderer.builder().extensions (extensions).build();
    var html = "<body>\n" + renderer.render (document) + "\n</body>";

    return (html);

  } // translateMarkdown

  /////////////////////////////////////////////////////////////////

  /** Shows the specified Markdown text and CSS in the webview. */
  private void showMarkdownText (String mdText, String cssFile, WebView view) {
    
    var engine = view.getEngine();
    engine.setUserStyleSheetLocation (getClass().getResource (cssFile).toString());
    engine.loadContent (translateMarkdown (mdText));
    
  } // showMarkdownText

  /////////////////////////////////////////////////////////////////

  /** Shows the specified Markdown file and CSS in the webview. */
  private void showMarkdownFile (String mdFile, String cssFile, WebView view) {
    
    String mdText = null;
    URL location = getClass().getResource (mdFile);
    if (location == null) {
      LOGGER.warning ("File " + mdFile + " not found");
    } // if
    else {
      try { mdText = readURL (location); }
      catch (IOException e) {
        LOGGER.log (Level.WARNING, "Error reading " + mdFile, e);
      } // catch
    } // else

    if (mdText != null) showMarkdownText (mdText, cssFile, view);
    
  } // showMarkdownFile

  /////////////////////////////////////////////////////////////////

  /** Creates a dialog to display the specified Markdown with view size. */
  private Dialog createDialog (String title, int width, int height) {
  
    // Create a dialog to show the Markdown
    Dialog dialog = new Dialog();
    var sheets = viewPane.getScene().getRoot().getStylesheets();
    dialog.getDialogPane().getStylesheets().addAll (sheets);
    dialog.setTitle (title);

    // Create and populate the web view
    var view = new WebView();
    view.setPrefWidth (width);
    view.setPrefHeight (height);

    // Set up the dialog
    dialog.getDialogPane().setContent (view);
    dialog.getDialogPane().getButtonTypes().add (ButtonType.CLOSE);
    dialog.initModality (Modality.NONE);
    dialog.initOwner (getStage());
  
    return (dialog);
    
  } // createDialog

  /////////////////////////////////////////////////////////////////

  /** Shows the specified dialog centered on the main window. */
  private void showDialog (Dialog dialog) {

    Stage stage = getStage();
    dialog.setX (stage.getX() + stage.getWidth()/2 - dialog.getWidth()/2);
    dialog.setY (stage.getY() + stage.getHeight()/2 - dialog.getHeight()/2);
    dialog.show();
  
  } // showDialog

  /////////////////////////////////////////////////////////////////

  @FXML
  private void showHelp() {

    if (helpDialog == null) {
      helpDialog = createDialog ("Vertigo Project Help", 700, 394);
      showMarkdownFile ("help.md", "markdown.css", (WebView) helpDialog.getDialogPane().getContent());
    } // if
    if (!helpDialog.isShowing()) showDialog (helpDialog);
        
  } // showHelp

  /////////////////////////////////////////////////////////////////

  @FXML
  private void showAbout() {

    if (aboutDialog == null) {
      var mdText =
        "**Vertigo Project** | " + System.getProperty ("vertigo.version") + "." + System.getProperty ("vertigo.build.number") + "\n" +
        ":--- | :---\n" +
        "**Build Date** | " + System.getProperty ("vertigo.build.date") + "\n" +
        "**Java Runtime** | " + System.getProperty ("java.vm.name") + " " + System.getProperty ("java.version") + " (" + System.getProperty ("java.version.date") + ")\n" +
        "**Operating System** | " + System.getProperty ("os.name") + " (" + System.getProperty ("os.version") + ") " + System.getProperty ("os.arch") + "\n" +
        "**JavaFX Runtime** | " + System.getProperty ("javafx.runtime.version") + "\n" +
        "**Maximum Memory** | " + Runtime.getRuntime().maxMemory()/1024/1024 + " MB\n" +
        "**Open-Source Components** | OpenJFX (openjfx.io)\n" +
        " | CommonMark (commonmark.org)\n" +
        " | jsoup (jsoup.org)\n" +
        " | NetCDF Java (www.unidata.ucar.edu)\n";
      aboutDialog = createDialog ("About Vertigo", 550, 225);
      showMarkdownText (mdText, "markdown.css", (WebView) aboutDialog.getDialogPane().getContent());
    } // if
    if (!aboutDialog.isShowing()) showDialog (aboutDialog);

  } // showAbout

  /////////////////////////////////////////////////////////////////

} // VertigoController
