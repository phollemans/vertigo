/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.HashMap;
import java.util.List;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;

import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableList;
import javafx.collections.MapChangeListener;

import javafx.fxml.FXML;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import javafx.event.EventHandler;
import javafx.event.Event;
import javafx.scene.input.MouseEvent;
import javafx.event.EventTarget;
import javafx.event.EventType;

import noaa.coastwatch.vertigo.ResourceStatus.State;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>DatasetListPaneController</code> class is the controller for
 * events in the dataset list pane displaying a tree of
 * {@link GeoSurfaceFactory} object groups and names.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class DatasetListPaneController {

  private static final Logger LOGGER = Logger.getLogger (DatasetListPaneController.class.getName());

  public static final EventType<DatasetEvent> INFO_REQUEST = new EventType<> ("info_request");

  @FXML
  private Pane rootPane;

  @FXML
  private TreeView<DatasetTreeNode> datasetTree;

  /** The map of surface name to status value. */
  private ObservableMap<GeoSurfaceFactory, ResourceStatus> statusMap = FXCollections.observableMap (new HashMap<>());

  /** The selected dataset property. */
  private ObjectProperty<GeoSurfaceFactory> selectedDatasetProp;

  /** The handler for dataset info request events. */
  private EventHandler<DatasetEvent> datasetInfoRequestHandler;

  /////////////////////////////////////////////////////////////////

  /** A container class for dataset node values. */
  private class DatasetTreeNode {
    public GeoSurfaceFactory factory;
    public String group;
    public DatasetTreeNode (GeoSurfaceFactory factory) { this.factory = factory; this.group = null; }
    public DatasetTreeNode (String group) { this.factory = null; this.group = group; }
    public boolean isGroup () { return (group != null); }
    public String getLabel() { return (isGroup() ? group : factory.getName()); }
    public String toString () { return (getLabel()); }
  } // DatasetTreeNode class

  /////////////////////////////////////////////////////////////////

  /** Holds the information for a dataset event. */
  public class DatasetEvent extends Event {
    private GeoSurfaceFactory dataset;
    public DatasetEvent (Object source, EventTarget target, EventType<? extends Event> eventType, GeoSurfaceFactory dataset) {
      super (source, target, eventType);
      this.dataset = dataset;
    } // DatasetEvent
    public GeoSurfaceFactory getDataset() { return (dataset); }
  } // DatasetEvent class

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the handler for when dataset info is requested in the list.
   *
   * @param handler the event handler.
   */
  public void setOnDatasetInfoRequest (EventHandler<DatasetEvent> handler) {

    datasetInfoRequestHandler = handler;
  
  } // setOnDatasetInfoRequest

  /////////////////////////////////////////////////////////////////

  /** Clears the items in the dataset list. This also clears the status map. */
  public void clear() {
  
    datasetTree.getRoot().getChildren().clear();
    statusMap.clear();

  } // clear

  /////////////////////////////////////////////////////////////////

  /** Inserts a new dataset into the tree. */
  private void insert (GeoSurfaceFactory dataset) {

    TreeItem<DatasetTreeNode> group = null;
    for (var item : datasetTree.getRoot().getChildren()) {
      if (item.getValue().getLabel().equals (dataset.getGroup())) { group = item; break; }
    } // for
    if (group == null) {
      group = new TreeItem<> (new DatasetTreeNode (dataset.getGroup()));
      group.setExpanded (true);
      datasetTree.getRoot().getChildren().add (group);
    } // if
    group.getChildren().add (new TreeItem<> (new DatasetTreeNode (dataset)));
  
  } // insert

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the items in the dataset list.  This also clears the status map.
   *
   * @param items the new dataset list items.
   */
  public void setDatasets (List<GeoSurfaceFactory> items) {

    datasetTree.getRoot().getChildren().clear();
    for (var dataset : items) insert (dataset);
    statusMap.clear();

  } // setDatasets

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the selected dataset property.
   *
   * @return the selected dataset property.
   */
  public ReadOnlyObjectProperty<GeoSurfaceFactory> selectedDatasetProperty() { return (selectedDatasetProp); }
  
  /////////////////////////////////////////////////////////////////

  /** Initializes the controller. */
  public void initialize () {

    TreeItem<DatasetTreeNode> root = new TreeItem<>();
    datasetTree.setRoot (root);

    datasetTree.setCellFactory (view -> new DatasetTreeCell());
    selectedDatasetProp = new SimpleObjectProperty<>();
    datasetTree.getSelectionModel().selectedItemProperty().addListener ((obs, oldVal, newVal) -> {
      if (newVal != null && newVal.isLeaf()) selectedDatasetProp.setValue (newVal.getValue().factory);
    });

  } // initialize
  
  /////////////////////////////////////////////////////////////////

  /**
   * Gets the status map that maps factory to resource status.
   *
   * @return the status map property.
   */
  public ObservableMap<GeoSurfaceFactory, ResourceStatus> getStatusMap() { return (statusMap); }

  /////////////////////////////////////////////////////////////////

  /** Displays a dataset tree cell as disabled while the surface is initializing. */
  private class DatasetTreeCell extends TreeCell<DatasetTreeNode> {

    private HBox box;
    private Label label;
    private ImageView icon;
    private MapChangeListener<GeoSurfaceFactory, ResourceStatus> statusListener;

    public DatasetTreeCell () {

      label = new Label();
      label.setMaxWidth (Double.MAX_VALUE);
      HBox.setHgrow (label, Priority.ALWAYS);

      icon = new ImageView();
      icon.setPickOnBounds (true);
      icon.getStyleClass().addAll ("available-status", "icon-shadow");

      box = new HBox (5);
      box.getChildren().addAll (label, icon);

      statusListener = change -> { if (getGraphic() != null) updateStatus(); };

    } // DatasetTreeCell

    /** Sets the icon to perform an event when clicked, or null to disable. */
    private void setIconAction (EventHandler<MouseEvent> handler) {
      icon.setOnMouseClicked (handler);
    } // setIconAction

    /** Shows a tooltip centered on the icon. */
    private void showTooltip (MouseEvent event, String message) {
      var tooltip = new Tooltip (message);
      tooltip.setAutoHide (true);
      tooltip.show (icon, event.getScreenX(), event.getScreenY());
    } // showTooltip

    /** Fires the information request event for the specified dataset. */
    private void fireInfoRequest (GeoSurfaceFactory factory) {
      if (datasetInfoRequestHandler != null) {
        datasetInfoRequestHandler.handle (new DatasetEvent (DatasetListPaneController.this,
          null, INFO_REQUEST, factory));
      } // if
    } // fireInfoRequest

    /** Replaces a matching CSS style in a node with a new style. */
    private void replaceStyle (Node node, String pattern, String replace) {
      var list = node.getStyleClass();
      for (int i = 0; i < list.size(); i++) {
        if (list.get (i).matches (pattern)) { list.set (i, replace); break; }
      } // for
    } // replaceStyle

    /** Updates the status of the cell by changing the icon and label opacity. */
    private void updateStatus () {
      var node = getTreeItem().getValue();
      if (!node.isGroup() && statusMap.containsKey (node.factory)) {
        var status = statusMap.get (node.factory);
        if (status.getState() == State.AVAILABLE) {
          label.setOpacity (1.0);
          var url = node.factory.getSourceUrl();
          if (url == null) {
            replaceStyle (icon, ".*-status", "available-status");
            setIconAction (null);
          } // if
          else {
            replaceStyle (icon, ".*-status", "available-info-status");
            setIconAction (event -> fireInfoRequest (node.factory));
          } // else
        } // if
        else if (status.getState() == State.UNAVAILABLE) {
          label.setOpacity (0.5);
          replaceStyle (icon, ".*-status", "disconnect-status");
          setIconAction (null);
        } // else
        else if (status.getState() == State.ERROR) {
          label.setOpacity (0.5);
          replaceStyle (icon, ".*-status", "warning-status");
          var errorMessage = status.getError().getMessage();
          if (errorMessage == null)
            errorMessage = "Received " + status.getError().getClass().getName() + " error";
          else
            errorMessage = errorMessage.replaceFirst ("^[^:]+:", "").trim();
          var message = "Failed to initialize dataset access.\n" +
            errorMessage.replaceFirst ("java\\.[^A-Z]+\\.", "") + ".";
          setIconAction (event -> showTooltip (event, message));
        } // else if
      } // if
      else {
        label.setOpacity (1.0);
        replaceStyle (icon, ".*-status", "available-status");
        setIconAction (null);
      } // else
    } // updateStatus

    @Override
    public void updateItem​ (DatasetTreeNode item, boolean empty) {

      super.updateItem (item, empty);
      if (item == null || empty) {
        setText (null);
        setGraphic (null);
        statusMap.removeListener (statusListener);
      } // if
      else {
        var treeItem = getTreeItem();
        if (treeItem.isLeaf()) {
          statusMap.addListener (statusListener);
          setText (null);
          label.setText (item.getLabel());
          updateStatus();
          setGraphic (box);
          double listWidth = getTreeView().getWidth();
          double boxWidth = listWidth - (30 + 18 + 8 + 8 + 2);
          box.setMaxWidth (boxWidth);
          box.setMaxWidth (boxWidth);
        } // if
        else {
          statusMap.removeListener (statusListener);
          setText (item.getLabel());
          setGraphic (null);
        } // else
      } // else
        
    } // updateItem

  } // DatasetTreeCell

  /////////////////////////////////////////////////////////////////

} // DatasetListPaneController

