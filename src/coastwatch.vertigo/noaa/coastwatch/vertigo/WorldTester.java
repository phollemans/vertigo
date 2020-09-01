/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.DoubleToIntFunction;
import java.util.function.BooleanSupplier;
import java.util.Date;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import java.nio.IntBuffer;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.IOException;

import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.paint.Color;
import javafx.scene.shape.Box;
import javafx.scene.shape.Sphere;

import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.image.PixelFormat;

import javafx.geometry.Point3D;

import javafx.application.Platform;

/**
 * The <code>WorldTester</code> class adds various test objects to a
 * <code>WorldController</code>.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class WorldTester {

  private static final double LOG10 = Math.log (10);

  private static final Logger LOGGER = Logger.getLogger (WorldTester.class.getName());

  private static WorldTester instance = new WorldTester();

  /////////////////////////////////////////////////////////////////

  public static WorldTester getInstance() { return (instance); }

  /////////////////////////////////////////////////////////////////

  protected WorldTester () {}

  /////////////////////////////////////////////////////////////////

  /** Adds random coloured test patches to the controller. */
  public void addPatches (WorldController controller, int count) {

    int faces = 0;
    for (int i = 0; i < count; i++) {
      MeshView meshView = createTestPatch (controller);
      faces += ((TriangleMesh) meshView.getMesh()).getFaces().size() / 6;
      controller.addObject (meshView);
    } // for

    LOGGER.fine ("Running with " + count + " test patches");
    LOGGER.fine ("Test patch mesh faces = " + faces);

  } // addPatches

  /////////////////////////////////////////////////////////////////

  /** Adds x,y,z axes to the controller. */
  public void addAxes (WorldController controller) {
  
    double radius = controller.getView().worldRadius();
    Box xAxis = new Box (radius*2.5, radius/25, radius/25);
    xAxis.setMaterial (new PhongMaterial (Color.RED));
    Sphere xBall = new Sphere (radius/25*2);
    xBall.setMaterial (new PhongMaterial (Color.RED));
    xBall.setTranslateX (radius*1.25);

    Box yAxis = new Box (radius/25, radius*2.5, radius/25);
    yAxis.setMaterial (new PhongMaterial (Color.GREEN));
    Sphere yBall = new Sphere (radius/25*2);
    yBall.setMaterial (new PhongMaterial (Color.GREEN));
    yBall.setTranslateY (radius*1.25);

    Box zAxis = new Box (radius/25, radius/25, radius*2.5);
    zAxis.setMaterial (new PhongMaterial (Color.BLUE));
    Sphere zBall = new Sphere (radius/25*2);
    zBall.setMaterial (new PhongMaterial (Color.BLUE));
    zBall.setTranslateZ (radius*1.25);

    controller.addObject (xAxis);
    controller.addObject (xBall);
    controller.addObject (yAxis);
    controller.addObject (yBall);
    controller.addObject (zAxis);
    controller.addObject (zBall);

  } // addAxes

  /////////////////////////////////////////////////////////////////

  /** Adds a spherical tiling to the controller. */
  public void addTiling (WorldController controller) {

    List<MeshView> viewList = createTestTiling (controller);
    int faces = 0;
    for (MeshView meshView : viewList) {
      faces += ((TriangleMesh) meshView.getMesh()).getFaces().size() / 6;
      controller.addObject (meshView);
    } // for

    LOGGER.fine ("Running with " + viewList.size() + " test tiles");
    LOGGER.fine ("Test tile mesh faces = " + faces);

  } // addTestObjects

  ////////////////////////////////////////////////////////////

  /** Creates a coloured square of random location and size for testing. */
  private MeshView createTestPatch (WorldController controller) {

    Point3D centerPoint = new Point3D (
      Math.random()*2 - 1,
      Math.random()*2 - 1,
      Math.random()*2 - 1
    );
    double[] center = new double[2];
    SphereFunctions.pointToSphere (centerPoint, center);
    double patchRadius = Math.toRadians (2);
    float[] start = new float[] {
      (float) Math.max (center[0] - patchRadius, 0),
      (float) (center[1] - patchRadius)
    };
    int[] counts = new int[] {
      (int) Math.round (Math.random()*15) + 5,
      (int) Math.round (Math.random()*15) + 5
    };
    float[] size = new float[] {
      (float) Math.toRadians (0.5),
      (float) (Math.toRadians (0.5)/Math.cos (Math.PI/2 - center[0]))
    };
    while (start[0] + counts[0]*size[0] > Math.PI) counts[0]--;
    float radius = (float) controller.getView().worldRadius();
    MeshView node = new MeshView (MeshObjectFactory.getInstance().createSphericalPatch (
      start, counts, size, radius
    ));
    node.setMaterial (new PhongMaterial (Color.hsb (Math.random()*360, 1, 0.8)));

    return (node);
    
  } // createTestPatch

  ////////////////////////////////////////////////////////////

  /** Computes the distance metric for the test tiling. */
  private static double computeDist (int[] points, double radius) {
  
    double dist;

    double lon1 = Math.toRadians (points[0]*0.01 + 0.005);
    double lat1 = Math.toRadians (90 - points[1]*0.01 - 0.005);
    double lon2 = Math.toRadians (points[2]*0.01 + 0.005);
    double lat2 = Math.toRadians (90 - points[3]*0.01 - 0.005);

    double dLon = Math.abs (lon1 - lon2);
    if (dLon > Math.PI) dLon = Math.PI*2 - dLon;
    double dRho = Math.acos (Math.sin (lat1)*Math.sin (lat2) +
      Math.cos(lat1)*Math.cos (lat2)* Math.cos (dLon));

    dist = dRho * radius;
    
    return (dist);
  
  } // computeDist

  ////////////////////////////////////////////////////////////

  /** Creates a list of views that tile the sphere. */
  private List<MeshView> createTestTiling (WorldController controller) {

    double maxDist = 1000;
    ImageTilingTree tree1 = new ImageTilingTree (
      new int[] {0, 1000},
      new int[] {18000 - 1, 17000 - 1},
      points -> computeDist (points, 6371),
      maxDist
    );
    tree1.summarize();

    ImageTilingTree tree2 = new ImageTilingTree (
      new int[] {18000, 1000},
      new int[] {36000 - 1, 17000 - 1},
      points -> computeDist (points, 6371),
      maxDist
    );
    tree2.summarize();

    List<MeshView> nodeList = new ArrayList<>();

    List<ImageTilingTree> leafNodes = tree1.getLeafNodes();
    leafNodes.addAll (tree2.getLeafNodes());

    float radius = (float) controller.getView().worldRadius();

    int leafIndex = 0;
    for (ImageTilingTree leaf : leafNodes) {
    
      int[] leafStart = leaf.getStart();
      int[] leafEnd = leaf.getEnd();


//int[] leafSize = new int[] {leafEnd[0] - leafStart[0] + 1, leafEnd[1] - leafStart[1] + 1};
//System.out.println ("Rendering leaf index " + leafIndex + " with size " + java.util.Arrays.toString (leafSize));
//leafIndex++;


      double phi1 = Math.toRadians (leafStart[0]*0.01);
      double theta1 = Math.toRadians (leafStart[1]*0.01);
      double phi2 = Math.toRadians ((leafEnd[0]+1)*0.01);
      double theta2 = Math.toRadians ((leafEnd[1]+1)*0.01);

      float[] start = new float[] {
        (float) theta1,
        (float) phi1
      };
      int[] counts = new int[] {1, 1};
      float[] size = new float[] {
        (float) (theta2 - theta1),
        (float) (phi2 - phi1)
      };


//System.out.println ("Adding patch at (theta,phi) = " + Math.toDegrees (start[THETA]) +
//  "," + Math.toDegrees (start[PHI]));
//System.out.println ("With size = (dTheta,dPhi) = " + Math.toDegrees (size[THETA]) +
//  "," + Math.toDegrees (size[PHI]));



      MeshView node = new MeshView (MeshObjectFactory.getInstance().createSphericalPatch (
        start, counts, size, radius
      ));
      node.setMaterial (new PhongMaterial (Color.hsb (Math.random()*360, 1, 0.8)));
      
      nodeList.add (node);
      
    } // for

    return (nodeList);
  
  } // createTestTiling

  /////////////////////////////////////////////////////////////////

  /** Adds a dynamic surface to the controller. */
  public void addDynamicSurface (WorldController controller, String texture) {

    double centerLat = 0;
    double centerLon = 0;
    double radius = controller.getView().worldRadius();
    SphereTranslator sphere = new SphereTranslator (radius);
    Image image = new Image ("file:" + texture);
    int width = (int) image.getWidth();
    double resolution = 360.0/width;
    int startY = (int) Math.round (10/resolution);
    int height = ((int) image.getHeight()) - startY*2;
    
    PixelReader reader = image.getPixelReader();

    ImageCoordinateSource coordSource = new ImageCoordinateSource() {

      public ImageAccessResult access (
        ImageAccess access,
        BooleanSupplier cancelled
      ) {
      
        return (new ImageAccessResult (access, null));
      
      } // access

      public void get (
        ImageAccessResult result,
        int x,
        int y,
        double[] data
      ) {
      
        ImageAccess access = result.access;
        if (x < 0 || x > access.getWidth()-1) throw new IndexOutOfBoundsException (x);
        int imageX = access.tile.minX + access.strideX * x;
        if (y < 0 || y > access.getHeight()-1) throw new IndexOutOfBoundsException (y);
        int imageY = access.tile.minY + access.strideY * y;
        double lat = centerLat + resolution*height/2 - resolution*imageY;
        double lon = centerLon - resolution*width/2 + resolution*imageX;
        sphere.translate (lat, lon, data);
      
      } // get

      public void getMany (
        ImageAccessResult result,
        ImageCoordinateIterator iter,
        double[] data
      ) {


      } // getMany
    
    };

    ImageTileWriter tileWriter = new ImageTileWriter() {
    
      public void write (
        ImageTile tile,
        WritableImage image,
        int startX,
        int startY,
        int width,
        int height,
        BooleanSupplier cancelled
      ) {

        WritablePixelFormat<IntBuffer> pixelFormat = PixelFormat.getIntArgbInstance();
        PixelWriter writer = image.getPixelWriter();

        int strideX = tile.width / width;
        int strideY = tile.height / height;

        int[] tileData = new int[tile.width];
        int[] imageData = new int[width];

        for (int y = 0; y < height; y++) {
          int tileLine = y*strideY;
          reader.getPixels (tile.minX, startY + tile.minY + tileLine, tile.width, 1, pixelFormat, tileData, 0, 0);
          for (int x = 0; x < width; x++) imageData[x] = tileData[x*strideX];
          writer.setPixels (startX, startY + y, width, 1, pixelFormat, imageData, 0, width);
        } // for
      
      } // write
    
    };

    // We create a thread to perform the creation of the surface in the
    // background, because the initial work by the FacetDataSource takes time.
    Thread surfaceThread = new Thread (() -> {

      FacetDataSource source;
      try {
        source = new TiledImageFacetDataSource (width, height,
          coordSource, tileWriter, (a,b) -> SphereFunctions.delta (a, b, radius),
          controller.getView().getProperties());
      } // try
      catch (IOException e) { throw new RuntimeException (e); }

      // After that's done, the surface can be created and then we listen for
      // facets to be ready and update the controller when each one becomes
      // available.
      Platform.runLater (() -> {

        DynamicSurface surface = new DynamicSurface (source);
        surface.setFacetConsumer (facet -> controller.addObject (facet.getNode()));
        surface.setUpdateConsumer (update -> controller.addSceneGraphChange (update));
        surface.setCameraPosition (controller.getView().cameraPositionProperty().get());

        controller.getView().cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
          surface.cameraPositionProperty().setValue (newVal);
        });

      });

    });
    surfaceThread.start();

  } // addDynamicSurface

  /////////////////////////////////////////////////////////////////

  /** Gets a list of color values extracted from the XML file. */
  private List<Integer> getColorList (String name) throws Exception {
  
    List<Integer> colorList = new ArrayList<>();
    try (InputStream stream = new FileInputStream (new File (name))) {

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse (stream);

      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();

      XPathExpression expr = xpath.compile ("/palette/color");
      NodeList nodes = (NodeList) expr.evaluate (doc, XPathConstants.NODESET);
      for (int i = 0; i < nodes.getLength(); i++) {
        Element element = (Element) nodes.item (i);
        int color = 0xff000000;
        color = color | Integer.parseInt (element.getAttribute ("r")) << 16;
        color = color | Integer.parseInt (element.getAttribute ("g")) << 8;
        color = color | Integer.parseInt (element.getAttribute ("b"));
        colorList.add (color);
      } // for

    } // try

    return (colorList);

  } // getColorList

  /////////////////////////////////////////////////////////////////

  /** Computes the log base 10 of a number. */
  private double log10 (double value) { return (Math.log (value) / LOG10); }

  /////////////////////////////////////////////////////////////////

  /** Adds a NetCDF surface to the controller. */
  public void addNetCDFSurface (WorldController controller,
    String ncFile, String ncVar, double min, double max, String funcType, String palette) {

    // We create a thread to perform the creation of the surface in the
    // background, because the initial work by the NetCDFDataset and
    // FacetDataSource takes time.
    Thread surfaceThread = new Thread (() -> {


      // Needed from the controller:
      
      // sphere (GeoCoordinateTranslator) -- converter function to go from geographic to model
      // coordinates --> used by the Dataset to provide an ImageCoordinateSource
      // that can produce model coordinates from lat/lon
      //
      // delta (ToDoubleBiFunction<double[], double[]>) -- function that is specific to the surface that helps determine
      // how to break up the image data into tiles and resolutions so that the
      // user has the best visual experience
      //
      // viewProps (ViewProperties) -- properties of the view that supplies near and far camera
      // distances and distance functions to be used with the delta function
      
      double radius = controller.getView().worldRadius();
      SphereTranslator sphere = new SphereTranslator (radius);

      Dataset dataset = new NetCDFDataset (ncFile, sphere);

      ImageCoordinateSource coordSource;
      ImageDataSource<double[]> dataSource;
      int width, height;
      
      try {

      LOGGER.fine ("Opened NetCDF dataset " + ncFile + " and found " + dataset.getVariables().size() + " variable(s)");
      if (!dataset.getVariables().contains (ncVar))
        throw new RuntimeException ("NetCDF dataset does not contain requested variable " + ncVar);
      LOGGER.fine ("NetCDF variable " + ncVar + ":");

      var timeList = dataset.getTimes (ncVar);
      int steps = timeList.size();
      SimpleDateFormat fmt = new SimpleDateFormat ("yyyy/MM/dd");
      fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
      String timeRange;
      if (steps == 0) timeRange = "No time information";
      else if (steps == 1) timeRange = fmt.format (timeList.get (0)) + " (1 timestep)";
      else timeRange = fmt.format (timeList.get (0)) + " to " + fmt.format (timeList.get (steps-1)) + " (" + timeList.size() + " steps)";
      LOGGER.fine ("  Time range -> " + timeRange);

      int levels = dataset.getLevels (ncVar).size();
      LOGGER.fine ("  Vertical levels -> " + levels);

      int[] dims = dataset.getDimensions (ncVar);
      width = dims[0];
      height = dims[1];
      LOGGER.fine ("  Width, height -> " + width + ", " + height);

      coordSource = dataset.getCoordinateSource (ncVar);

      int timeStep = Math.max (steps-1, 0);
      dataSource = dataset.getDataSource (ncVar, timeStep, 0);

      } // try
      catch (IOException e) {
        throw new RuntimeException (e);
      } // catch

      List<Integer> colorList;
      try { colorList = getColorList (palette); }
      catch (Exception e) {
        LOGGER.severe ("Failed reading color map");
        throw new RuntimeException (e);
      } // catch
      int nColors = colorList.size();
      LOGGER.fine ("Using color palette " + palette + " with " + nColors + " colors");
      colorList.add (0, 0xff000000);
  //    colorList.add (0, 0x00000000);
      int[] map = colorList.stream().mapToInt (i -> i).toArray();

      DoubleToIntFunction func;
      LOGGER.fine ("Using " + funcType + " function type with min " + min + " and max " + max);
      if (funcType.equals ("linear")) {
        func = value -> {
          int intValue;
          if (Double.isNaN (value)) {
            intValue = 0;
          } // if
          else {
            double norm = (value - min) / (max - min);
            if (norm < 0) norm = 0;
            else if (norm > 1) norm = 1;
            intValue = (int) Math.round (norm * (nColors-1)) + 1;
          } // else
          return (intValue);
        };
      } // if
      else if (funcType.equals ("log")) {
        double slope = 1.0 / (log10 (max) - log10 (min));
        double inter = -slope * log10 (min);
        func = value -> {
          int intValue;
          if (Double.isNaN (value)) {
            intValue = 0;
          } // if
          else {
            double norm = slope*log10 (value) + inter;
            if (norm < 0) norm = 0;
            else if (norm > 1) norm = 1;
            intValue = (int) Math.round (norm * (nColors-1)) + 1;
          } // else
          return (intValue);
        };
      } // else if
      else throw new RuntimeException ("Unknown function type " + funcType);
    
      DoubleToColorConverter converter = new DoubleToColorConverter (map, func);
      ImageTileWriter tileWriter = new ColorTileWriter<double[]> (dataSource, converter);

      FacetDataSource source;
      try {
        source = new TiledImageFacetDataSource (width, height,
          coordSource, tileWriter, (a,b) -> SphereFunctions.delta (a, b, radius),
          controller.getView().getProperties());
      } // try
      catch (IOException e) { throw new RuntimeException (e); }

      // After that's done, the surface can be created and then we listen for
      // facets to be ready and update the controller when each one becomes
      // available.
      Platform.runLater (() -> {

        DynamicSurface surface = new DynamicSurface (source);
        surface.setFacetConsumer (facet -> controller.addObject (facet.getNode()));
        surface.setUpdateConsumer (update -> controller.addSceneGraphChange (update));
        surface.setCameraPosition (controller.getView().cameraPositionProperty().get());

        controller.getView().cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
          surface.cameraPositionProperty().setValue (newVal);
        });

      });

    });
    surfaceThread.start();

  } // addNetCDFSurface

  /////////////////////////////////////////////////////////////////

  /** Adds a tiled web map surface to the controller. */
  public void addWebMapSurface (WorldController controller,
    String urlPattern, int levels, int tileSize,
    double startLat, double startLon) {

    double radius = controller.getView().worldRadius();
    SphereTranslator sphere = new SphereTranslator (radius);

    List<Date> timeList = new ArrayList<>();
    Calendar cal = Calendar.getInstance (TimeZone.getTimeZone ("UTC"));
    int days = 14;
    cal.add (Calendar.DAY_OF_MONTH, -days);
    for (int i = 0; i < days; i++) {
      timeList.add (cal.getTime());
      cal.add (Calendar.DAY_OF_MONTH, 1);
    } // for

    WebMap webmap = new SimpleWebMap (urlPattern, levels, timeList, tileSize, startLat, startLon, sphere);
    LOGGER.fine ("Created tiled web map with URL pattern " + urlPattern + " and " + tileSize + "x" + tileSize + " tiles");

    int steps = timeList.size();
    SimpleDateFormat fmt = new SimpleDateFormat ("yyyy/MM/dd");
    fmt.setTimeZone (TimeZone.getTimeZone ("UTC"));
    String timeRange;
    if (steps == 0) timeRange = "No time information";
    else if (steps == 1) timeRange = fmt.format (timeList.get (0)) + " (1 timestep)";
    else timeRange = fmt.format (timeList.get (0)) + " to " + fmt.format (timeList.get (steps-1)) + " (" + timeList.size() + " steps)";
    LOGGER.fine ("  Time range -> " + timeRange);

    int[] dims = webmap.getDimensions();
    int width = dims[0];
    int height = dims[1];
    LOGGER.fine ("  Width, height -> " + width + ", " + height);

    ImageCoordinateSource coordSource = webmap.getCoordinateSource();
    ImageDataSource<int[]> dataSource = webmap.getDataSource (steps-2);
    ImageTileWriter tileWriter = new ColorTileWriter<int[]> (dataSource, new IntegerPassThroughConverter());

    // We create a thread to perform the creation of the surface in the
    // background, because the initial work by the FacetDataSource takes time.
    Thread surfaceThread = new Thread (() -> {

      FacetDataSource source;
      try {
        source = new TiledImageFacetDataSource (width, height,
          coordSource, tileWriter, (a,b) -> SphereFunctions.delta (a, b, radius),
          controller.getView().getProperties());
      } // try
      catch (IOException e) { throw new RuntimeException (e); }

      // After that's done, the surface can be created and then we listen for
      // facets to be ready and update the controller when each one becomes
      // available.
      Platform.runLater (() -> {

        DynamicSurface surface = new DynamicSurface (source);
        surface.setFacetConsumer (facet -> controller.addObject (facet.getNode()));
        surface.setUpdateConsumer (update -> controller.addSceneGraphChange (update));
        surface.setCameraPosition (controller.getView().cameraPositionProperty().get());

        controller.getView().cameraPositionProperty().addListener ((obs, oldVal, newVal) -> {
          surface.cameraPositionProperty().setValue (newVal);
        });

      });

    });
    surfaceThread.start();

  } // addWebMapSurface

  /////////////////////////////////////////////////////////////////

} // WorldModel class
