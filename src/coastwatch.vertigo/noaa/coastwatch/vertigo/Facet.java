/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.logging.Logger;
import java.util.logging.Level;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import java.util.Map;
import java.util.HashMap;
 
import java.util.function.Consumer;

import javafx.geometry.Point3D;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Group;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.image.Image;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;
import javafx.scene.shape.DrawMode;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

/**
 * The <code>Facet</code> class holds information about a single facet element
 * managed by a {@link DynamicSurface} object.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Facet {

  private static final Logger LOGGER = Logger.getLogger (Facet.class.getName());

  // Variables
  // ---------

  /** The source used for creating mesh and texture data for this facet. */
  private FacetDataSource source;

  /** The index of this facet within the set of facets. */
  private int index;
  
  /** The mesh resolution level currently being used by this facet. */
  private int meshLevel;
  
  /** The texture resolution level currently being used by this facet. */
  private int textureLevel;
  
  /** The current triangle mesh that this facet is using, or null if not initialized. */
  private TriangleMesh activeMesh;
  
  /** The current texture image that this facet is using, or null if not initialized. */
  private Image activeTexture;
  
  /** The node for this facet. */
  private ObjectProperty<Node> nodeProp = new SimpleObjectProperty<> (this, "node");
  public final Node getNode() { return (nodeProp.get()); }
  public final ReadOnlyObjectProperty<Node> nodeProperty() { return (nodeProp); }

  /** The color to use for this facet when no texture is set yet. */
  private ObjectProperty<Color> colorProp = new SimpleObjectProperty<> (this, "color", Color.color (0.35, 0.35, 0.35));
  public final Color getColor() { return (colorProp.get()); }
  public final void setColor (Color color) { colorProp.set (color); }
  public final ObjectProperty<Color> colorProperty() { return (colorProp); }

  /** The center point of the facet mesh. */
  private Point3D center;

  /** The factory used to generate responses to facet update requests. */
  private FacetUpdateResponseFactory responseFactory;

  /** The execution service to use for updating facets in the background. */
  private static ExecutorService executor;

  /** The consumer called when a facet has an update to the scene graph. */
  private Consumer<Runnable> updateConsumer;
  
  /////////////////////////////////////////////////////////////////

  static {
  
    // Create a thread factory tha makes daemon threads based on the
    // default factory.  This is used so that the program exits correctly (ie:
    // there are no non-daemon threads lying around).
    ThreadFactory daemonFactory = new ThreadFactory() {
      ThreadFactory defaultFactory = Executors.defaultThreadFactory();
      public Thread newThread​ (Runnable r) {
        Thread thread = defaultFactory.newThread (r);
        thread.setDaemon (true);
        return (thread);
      } // newThread
    };
  
    // Set up and executor that spans all facets and uses half of the available
    // processors.  We want to make sure that we don't starve the application
    // thread.
    int maxThreads = Math.max (1, Runtime.getRuntime().availableProcessors()-3);
//    int maxThreads = 16;
    LOGGER.fine ("Using " + maxThreads + " background threads for facet updates");
    executor = Executors.newFixedThreadPool (maxThreads, daemonFactory);

  } // static

  /////////////////////////////////////////////////////////////////

  /**
   * Sets the consumer for updates to the scene graph for this facet.  If not
   * set, updates the scene graph will be made directly from the facet.
   *
   * @param consumer the update consumer to use.
   */
  public void setUpdateConsumer (
    Consumer<Runnable> consumer
  ) {

    updateConsumer = consumer;
  
  } // setUpdateConsumer

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the index of this facet.
   *
   * @return the facet index within the group of facets.
   */
  public int getIndex () { return (index); }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new facet using a source and index.  The facet is initialized
   * with empty mesh and texture until a call to {@link #update} starts
   * the process of updating.
   *
   * @param source the data source for facet mesh and texture.
   * @param index the index of this new facet in the source.
   */
  public Facet (
    FacetDataSource source,
    int index
  ) {
  
    // Facets are initially created with no view or center point or texture,
    // until an update is called on them.  Then they update themselves and
    // a view is available.
    this.source = source;
    this.index = index;
    this.meshLevel = -1;
    this.textureLevel = -1;

    // The response factory does the facet data fetching in a background service
    // and then completes the update on the application thread by switching
    // the single child in a group node to the new mesh and texture.
    responseFactory = new FacetUpdateResponseFactory();
    responseFactory.setExecutor (executor);
    responseFactory.setOnSucceeded (event -> {
      completeUpdate (responseFactory.getValue());
    });

  } // Facet

  /////////////////////////////////////////////////////////////////

  /**
   * A <code>FacetUpdateRequest</code> holds the mesh and texture level
   * update request values for use by the response factory.
   */
  private static class FacetUpdateRequest {
  
    public int activeMeshLevel, newMeshLevel;
    public int activeTextureLevel, newTextureLevel;
    public TriangleMesh activeMesh;
    public Image activeTexture;
  
    /**
     * Checks if a request is asking for the same new mesh and texture levels
     * as this request.
     *
     * @param request the request to check for a match (may be null).
     *
     * @return true if the specified request is the same as this one,
     * or false if not.  Also returns false if the specified request is null.
     */
    public boolean matches (FacetUpdateRequest request) {
      return (request != null && request.newMeshLevel == newMeshLevel &&
        request.newTextureLevel == newTextureLevel);
    } // matches

  } // FacetUpdateRequest class

  /////////////////////////////////////////////////////////////////

  /**
   * The <code>FacetUpdateResponse</code> holds the data fetched and assembled
   * by the response factory to update a facet.
   */
  private static class FacetUpdateResponse {
  
    public FacetUpdateRequest request;
    public TriangleMesh mesh;
    public Image texture;
    public MeshView view;

  } // FacetUpdateResponse class

  /////////////////////////////////////////////////////////////////

  /** Creates a copy of a triangle mesh. */
  private static TriangleMesh meshCopy (TriangleMesh mesh) {
    
    TriangleMesh copy = new TriangleMesh (mesh.getVertexFormat());
    copy.getPoints().setAll (mesh.getPoints());
    copy.getNormals().setAll (mesh.getNormals());
    copy.getFaces().setAll (mesh.getFaces());
    copy.getTexCoords().setAll (mesh.getTexCoords());

    return (copy);
  
  } // meshCopy

  /////////////////////////////////////////////////////////////////

  /**
   * The <code>FacetUpdateResponseFactory</code> runs a service that takes
   * a request and fetches the data in a background thread to create
   * a response value.
   */
  private class FacetUpdateResponseFactory extends Service<FacetUpdateResponse> {
  
    /** The request that will be performed when a new task is started. */
    public FacetUpdateRequest request;

    /** The cache of texture level to image for this facet. */
    private Map<Integer, Image> textureCache = new HashMap<>();

    /** The cache of texture level to mesh for this facet. */
    private Map<Integer, TriangleMesh> meshCache = new HashMap<>();

    @Override
    protected Task<FacetUpdateResponse> createTask() {
      final FacetUpdateRequest taskRequest = request;
      return (new Task<FacetUpdateResponse>() {
        protected FacetUpdateResponse call () throws Exception {

          // Either retrieve the mesh or copy the values over from the
          // active mesh.  Note that if there is no current mesh,
          // and the mesh is not requested here, that's an error.
          TriangleMesh mesh;
          if (taskRequest.newMeshLevel != -1) {
            mesh = meshCache.get (taskRequest.newMeshLevel);
            if (mesh == null) {
              mesh = source.getMeshFactory().create (index, taskRequest.newMeshLevel, this::isCancelled);
              if (mesh != null) meshCache.put (taskRequest.newMeshLevel, mesh);
            } // if
            if (mesh != null) mesh = meshCopy (mesh);
          } // if
          else {
            TriangleMesh oldMesh = taskRequest.activeMesh;
            if (oldMesh == null)
              throw new RuntimeException ("No active mesh to use in response for facet " + index);
            mesh = meshCopy (oldMesh);
          } // else

          // After that bit of work, check if we are cancelled.
          if (isCancelled()) return (null);

          // Either retrieve the texture or use the active texture.  Note that
          // image data can be used in multiple places with no issue.  If there
          // is a texture, the mesh needs to have its texture points set up
          // to use it correctly.
          Image texture;
          if (taskRequest.newTextureLevel != -1) {
            texture = textureCache.get (taskRequest.newTextureLevel);
            if (texture == null) {
              double aspect = source.getMeshFactory().getAspectRatio (index);
              texture = source.getTextureFactory().create (index, aspect, taskRequest.newTextureLevel, this::isCancelled);
              if (texture != null) textureCache.put (taskRequest.newTextureLevel, texture);
            } // if
          } // if
          else
            texture = taskRequest.activeTexture;

          // After that bit of work, check if we are cancelled.
          if (isCancelled()) return (null);

          if (texture != null) {
            int textureWidth = (int) texture.getWidth();
            int textureHeight = (int) texture.getHeight();
            int textureMeshLevel = (taskRequest.newMeshLevel != -1 ?
              taskRequest.newMeshLevel : taskRequest.activeMeshLevel);
            source.getMeshFactory().setTexturePoints (mesh, index,
              textureMeshLevel, textureWidth, textureHeight);
          } // if

          // After that bit of work, check if we are cancelled.
          if (isCancelled()) return (null);

          // Now create the mesh view using the mesh and either a texture
          // or solid colour.  In FINER logging mode, draw lines for
          // the mesh.
          MeshView view = new MeshView (mesh);
          if (LOGGER.isLoggable (Level.FINER)) view.setDrawMode (DrawMode.LINE);
          PhongMaterial material = new PhongMaterial();
          if (texture == null)
            material.setDiffuseColor (colorProp.get());
          else
            material.setDiffuseMap (texture);
          view.setMaterial (material);
          
          // Finally, create the response and populate it with the newly created
          // view.
          FacetUpdateResponse response = new FacetUpdateResponse();
          response.request = taskRequest;
          response.mesh = mesh;
          response.texture = texture;
          response.view = view;

          return (response);

        } // call
      });
    } // createTask
  
    @Override
    protected void failed() {
      LOGGER.log (Level.WARNING, "Update failed for facet " + index, getException());
    } // failed

  } // FacetUpdateResponseFactory class

  /////////////////////////////////////////////////////////////////

  /**
   * Starts an update of the mesh and texture in this facet.  The update is
   * performed in a background thread and this method returns immediately.
   * The update will be completed by either (i) if the facet is newly
   * constructed and not yet initialized, the node property is null and
   * will be assigned a value when the facet data is available as a result
   * of the update call, or (ii) the node's internal structure will be
   * updated with the new data.
   *
   * @param meshLevel the new mesh level or -1 to not update.
   * @param textureLevel the new texture level or -1 to not update.
   */
  public void update (
    int meshLevel,
    int textureLevel
  ) {

    boolean updateMesh = (meshLevel != -1 && this.meshLevel != meshLevel);
    boolean updateTexture = (textureLevel != -1 && this.textureLevel != textureLevel);

    if (updateMesh || updateTexture) {
      FacetUpdateRequest request = new FacetUpdateRequest();
      request.activeMeshLevel = this.meshLevel;
      request.newMeshLevel = (updateMesh ? meshLevel : -1);
      request.activeTextureLevel = this.textureLevel;
      request.newTextureLevel = (updateTexture ? textureLevel : -1);
      request.activeMesh = activeMesh;
      request.activeTexture = activeTexture;
      requestUpdate (request);
    } // if

  } // update

  /////////////////////////////////////////////////////////////////

  /** Stops any facet mesh or texture update in progress. */
  public void stopUpdate () {

    switch (responseFactory.getState()) {

    case READY:
    case RUNNING:
    case SCHEDULED:
      responseFactory.cancel();
      if (LOGGER.isLoggable (Level.FINER)) LOGGER.finer ("Cancelled update for facet " + index);
      break;

    } // switch

  } // stopUpdate

  /////////////////////////////////////////////////////////////////
  
  /**
   * Submits a request to the response factory.  If a request is already
   * pending, its response is discarded.
   *
   * @param request the request to submit.
   */
  private void requestUpdate (FacetUpdateRequest request) {

    // Check the currently running/scheduled request here. If it matches
    // the current request then don't do anything.  If not, then cancel and
    // restart with the new request.  If the service isn't running, then
    // just submit the new request.
    switch (responseFactory.getState()) {

    case READY:
    case RUNNING:
    case SCHEDULED:
      if (!request.matches (responseFactory.request)) {
        responseFactory.request = request;
        responseFactory.restart();
      } // if
      break;
        
    case CANCELLED:
    case FAILED:
    case SUCCEEDED:
      responseFactory.request = request;
      responseFactory.reset();
      responseFactory.start();
      break;
    
    } // switch

    LOGGER.finer ("Requested update for facet " + index);

  } // requestUpdate

  /////////////////////////////////////////////////////////////////

  /**
   * Completes an update by applying the response data to the facet.
   *
   * @param response the response to apply.
   */
  private void completeUpdate (FacetUpdateResponse response) {

    // Unpack the response data into the facet, first replacing
    // the active data and levels
    if (response.request.newMeshLevel != -1) meshLevel = response.request.newMeshLevel;
    activeMesh = response.mesh;

    if (response.request.newTextureLevel != -1) textureLevel = response.request.newTextureLevel;
    activeTexture = response.texture;

    // Secondly, give the node a value if it doesn't have one, or replace
    // the value if it does.  We also set the center if needed, since this
    // may be the first time the center of the facet is known.  Note that
    // when replacing a value, if there's a consumer for scene graph updates
    // available, we use it.  This can help when there are many scene graph
    // updates to perform.
    if (nodeProp.get() == null) {
      Bounds bounds = response.view.getBoundsInLocal();
      center = new Point3D (bounds.getCenterX(), bounds.getCenterY(), bounds.getCenterZ());
      Group group = new Group();
      group.getChildren().add (response.view);
      nodeProp.set (group);
    } // if
    else {
      Group group = (Group) nodeProp.get();
      if (updateConsumer == null) {
        group.getChildren().set (0, response.view);
        LOGGER.finer ("Completed direct scene graph update for facet " + index);
      } // if
      else {
        updateConsumer.accept (() -> group.getChildren().set (0, response.view));
        LOGGER.finer ("Submitted scene graph update for facet " + index);
      } // else
    } // else

  } // completeUpdate

  /////////////////////////////////////////////////////////////////

  /**
   * Determines if this facet matches a given mesh and texture level.
   *
   * @param meshLevel the mesh level to check for.
   * @param textureLevel the texture level to check for.
   *
   * @return true if this facet matches the specified mesh and texture levels,
   * or false if not.
   */
  public boolean matches (
    int meshLevel,
    int textureLevel
  ) {
  
    return (this.meshLevel == meshLevel && this.textureLevel == textureLevel);
  
  } // matches

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the center point of this facet determined from the bounds.
   *
   * @return the center point.
   */
  public Point3D getCenter() { return (center); }

  /////////////////////////////////////////////////////////////////

} // Facet class

