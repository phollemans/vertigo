/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.io.File;
import java.util.List;

import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ListCell;

import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.collections.FXCollections;

import javafx.scene.image.WritableImage;
import javafx.scene.image.ImageView;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>DatasetSurfacePaneController</code> class is the controller for
 * events in the pane displaying a {@link DatasetSurfaceFactory} object.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class DatasetSurfacePaneController {

  private static final Logger LOGGER = Logger.getLogger (DatasetSurfacePaneController.class.getName());

  private static final int PALETTE_HEIGHT = 15;
  private static final int PALETTE_WIDTH = 45;

  @FXML
  private Pane rootPane;

  @FXML
  private TextField nameField;

  @FXML
  private TextField fileField;
  
  @FXML
  private ComboBox<String> variableCombo;

  @FXML
  private ComboBox<String> paletteCombo;

  @FXML
  private TextField minField;

  @FXML
  private TextField maxField;

  @FXML
  private CheckBox logCheck;

  /** The surface factory property. */
  private ObjectProperty<DatasetSurfaceFactory> factoryProp;

  /** The surface factory builder property. */
  private ObjectProperty<DatasetSurfaceFactoryBuilder> builderProp;

  /** The handler for factory build events. */
  private EventHandler<ActionEvent> buildEventHandler;

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the handler for when a new object should be obtained from the
   * builder.
   *
   * @param handler the event handler.
   */
  public void setOnBuild (EventHandler<ActionEvent> handler) {

    buildEventHandler = handler;
  
  } // setOnBuild

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the factory property (default null).
   *
   * @return the factory property.
   */
  public ObjectProperty<DatasetSurfaceFactory> factoryProperty () { return (factoryProp); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the builder property (default null).
   *
   * @return the builder property.
   */
  public ObjectProperty<DatasetSurfaceFactoryBuilder> builderProperty () { return (builderProp); }

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleOpenDataFileButton () {

    // Create and show the file chooser
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle ("Open Dataset");
    File file = fileChooser.showOpenDialog (rootPane.getScene().getWindow());

    // Open the dataset
    if (file != null) {


//      openProject (file.toURI().toString());


    } // if



  
  
  } // handleOpenDataFileButton

  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleFileField () {



  
  
  } // handleFileField
  
  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleApplyButton () {

    var builder = builderProp.getValue();
    builder.setProperty ("name", nameField.getText());
    builder.setProperty ("url", fileField.getText());
    builder.setProperty ("variable", variableCombo.getValue());
    builder.setProperty ("palette", paletteCombo.getValue());
    builder.setProperty ("min", Double.parseDouble (minField.getText()));
    builder.setProperty ("max", Double.parseDouble (maxField.getText()));
    builder.setProperty ("function", logCheck.isSelected() ? "log" : "linear");


    LOGGER.fine ("builder = " + builder);



    buildEventHandler.handle (new ActionEvent (this, null));

  } // handleApplyButton
  
  /////////////////////////////////////////////////////////////////

  @FXML
  private void handleRevertButton () {

    showFactory (factoryProp.getValue());
  
  } // handleRevertButton
  
  /////////////////////////////////////////////////////////////////

  /**
   * Shows the specified factory in the UI elements.
   *
   * @param factory the factory to show, or null to clear.
   */
  private void showFactory (DatasetSurfaceFactory factory) {
  
    if (factory != null) {

      var spec = factory.getSpec();

      nameField.setText ((String) spec.get ("name"));

      String url = (String) spec.get ("url");
      fileField.setText (url);
      fileField.setTooltip (new Tooltip (url));

      var variable = (String) spec.get ("variable");
      var varList = factory.isInitialized() ? factory.getVariableList() : List.of (variable);
      variableCombo.setItems (FXCollections.observableList (varList));
      variableCombo.getSelectionModel().select (variable);

      paletteCombo.setValue ((String) spec.get ("palette"));
      minField.setText (((Double) spec.get ("min")).toString());
      maxField.setText (((Double) spec.get ("max")).toString());

      var function = (String) spec.get ("function");
      logCheck.setSelected (function.equals ("log"));
  
    } // if

    else {

      nameField.setText (null);
      fileField.setText (null);
      variableCombo.setValue (null);
      paletteCombo.getSelectionModel().select (0);
      minField.setText (null);
      maxField.setText (null);
      logCheck.setSelected (false);
    
    } // else
  
  } // showFactory

  /////////////////////////////////////////////////////////////////

  /** Initializes the controller. */
  public void initialize () {

    // Set factory prop to update UI on new value
    factoryProp = new SimpleObjectProperty<> (this, "factory", null);
    factoryProp.addListener ((obs, oldVal, newVal) -> {
      showFactory (newVal);
    });

    // Create the builder property
    builderProp = new SimpleObjectProperty<> (this, "builder", null);

    // Set palette combo to show sample of palette
    paletteCombo.setCellFactory (box -> new PaletteListCell());
    paletteCombo.setButtonCell (new PaletteListCell());

  } // initialize
  
  /////////////////////////////////////////////////////////////////

  /** Displays a palette list cell as text and color scale. */
  private static class PaletteListCell extends ListCell<String> {

    @Override
    protected void updateItem (String item, boolean empty) {

      super.updateItem (item, empty);
      if (empty || item == null) {
        setText (null);
        setGraphic (null);
      } // if
      else {
      
        setText (item);

        var palette = Palette.getInstance (item);
        var colors = palette.getColors();
        var image = new WritableImage (PALETTE_WIDTH, PALETTE_HEIGHT);
        var writer = image.getPixelWriter();
        double m = ((double) (colors.size()-1)) / (PALETTE_WIDTH-1);
        for (int x = 0; x < PALETTE_WIDTH; x++) {
          int color = colors.get ((int) (m*x));
          for (int y = 0; y < PALETTE_HEIGHT; y++) writer.setArgb (x, y, color);
        } // for
        var view = new ImageView (image);
        view.setStyle ("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,1.0), 3, 0.0, 0, 0);");

        setGraphic (view);
        setGraphicTextGap (6);
        
      } // if

    } // updateItem

  } // PaletteListCell

  /////////////////////////////////////////////////////////////////

} // DatasetSurfacePaneController
