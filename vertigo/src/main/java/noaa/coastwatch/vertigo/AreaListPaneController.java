/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;

import javafx.scene.input.MouseEvent;
import javafx.scene.control.ListCell;
import javafx.scene.control.Button;
import javafx.scene.control.TreeView;
import javafx.scene.control.TreeItem;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import javafx.collections.ObservableList;

import javafx.fxml.FXML;

import javafx.beans.property.ReadOnlyObjectProperty;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>AreaListPaneController</code> class is the controller for
 * events in the area list pane displaying a list of {@link Area} object names.
 *
 * @author Peter Hollemans
 * @since 0.6
 */
public class AreaListPaneController {

  private static final Logger LOGGER = Logger.getLogger (AreaListPaneController.class.getName());

  @FXML
  private Pane rootPane;

  @FXML
  private TreeView<String> areaTree;

  /** The handler for area activation events. */
  private EventHandler<ActionEvent> areaActivatedHandler;

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the handler for when an area is activated in the list.
   *
   * @param handler the event handler.
   */
  public void setOnAreaActivated (EventHandler<ActionEvent> handler) {

    areaActivatedHandler = handler;
  
  } // setOnAreaActivated

  /////////////////////////////////////////////////////////////////

  /** Clears the items in the area list. */
  public void clear() {
  
    areaTree.getRoot().getChildren().clear();

  } // clear

  /////////////////////////////////////////////////////////////////

  /** Inserts a new area into the tree. */
  private void insert (Area area) {

    TreeItem<String> group = null;
    for (var item : areaTree.getRoot().getChildren()) {
      if (item.getValue().equals (area.getGroup())) { group = item; break; }
    } // for
    if (group == null) {
      group = new TreeItem<> (area.getGroup());
      group.setExpanded (true);
      areaTree.getRoot().getChildren().add (group);
    } // if
    group.getChildren().add (new TreeItem<> (area.getName()));
  
  } // insert

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the items in the area list.
   *
   * @param items the new area list items.
   */
  public void setAreas (List<Area> items) {

    areaTree.getRoot().getChildren().clear();
    for (var area : items) insert (area);

  } // setAreas

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the selected area name.
   *
   * @return the selected area name.
   */
  public String getSelectedArea() { return (areaTree.getSelectionModel().getSelectedItem().getValue()); }

  /////////////////////////////////////////////////////////////////

  /** Initializes the controller. */
  public void initialize () {

    // Set up area activated
    areaTree.addEventHandler (MouseEvent.MOUSE_CLICKED, event -> {
      if (event.getClickCount() == 2) {
        var item = areaTree.getSelectionModel().getSelectedItem();
        if (item != null && item.isLeaf() && areaActivatedHandler != null)
          areaActivatedHandler.handle (new ActionEvent (AreaListPaneController.this, null));
      } // if
    });

    TreeItem<String> root = new TreeItem<>();
    areaTree.setRoot (root);

//    areaList.setCellFactory (view -> new AreaListCell());

  } // initialize
  
  /////////////////////////////////////////////////////////////////

//  /** Displays an area list cell with clickable icon to activate. */
//  private class AreaListCell extends ListCell<String> {
//
//    private ImageView solidIcon;
//    private ImageView inactiveIcon;
//    private Button button;
//
//    public AreaListCell () {
//
//      var image = new Image (getClass().getResource ("nav_right.png").toExternalForm());
//      solidIcon = new ImageView (image);
//      solidIcon.getStyleClass().add ("icon-shadow");
//
//      inactiveIcon = new ImageView (image);
//      inactiveIcon.setOpacity (0.25);
//
//      button = new Button();
//      button.setStyle ("-fx-background-color: transparent; -fx-padding: 0;");
//      button.setMaxSize (16, 16);
//      button.setMinSize (16, 16);
//      button.setFocusTraversable (false);
//
//      button.setOnAction (event -> {
//        var item = areaList.getItems().get (getIndex());
////        projController.showArea (item);
//      });
//
//      setOnMouseEntered (event -> { if (getGraphic() != null) button.setGraphic (solidIcon); });
//      setOnMouseExited (event -> { if (getGraphic() != null) button.setGraphic (inactiveIcon); });
//
//      setGraphicTextGap (5);
//
//    } // AreaListCell
//
//    @Override
//    protected void updateItem (String item, boolean empty) {
//
//      super.updateItem (item, empty);
//      if (empty || item == null) {
//        setText (null);
//        setGraphic (null);
//      } // if
//      else {
//        setText (item);
//        button.setGraphic (isSelected() ? solidIcon : inactiveIcon);
//        setGraphic (button);
//      } // if
//
//    } // updateItem
//
//  } // AreaListCell

  /////////////////////////////////////////////////////////////////

} // AreaListPaneController


