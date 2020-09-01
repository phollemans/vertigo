/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.Mesh;
import javafx.scene.shape.ObservableFaceArray;

import javafx.collections.ObservableFloatArray;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.Map;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Arrays;

/**
 * Creates various mesh objects for use in the scene graph.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class MeshObjectFactory {

  private static final int THETA = 0;
  private static final int PHI = 1;

  private static final int X = 0;
  private static final int Y = 1;
  private static final int Z = 2;

  private static final float PI = (float) Math.PI;
  private static final float TWO_PI = (float) (Math.PI * 2);

  private static MeshObjectFactory instance;

  /////////////////////////////////////////////////////////////////

  protected MeshObjectFactory () {}

  /////////////////////////////////////////////////////////////////
  
  public static MeshObjectFactory getInstance() {
  
    if (instance == null) instance = new MeshObjectFactory();
    return (instance);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

  private static void fromto (float[] p1, float[] p2, float[] p1p2) {

    for (int i = 0; i < 3; i++) p1p2[i] = p2[i] - p1[i];
  
  } // fromto

  /////////////////////////////////////////////////////////////////

  private static void cross (float[] v1, float[] v2, float[] cross) {

    cross[X] = v1[Y]*v2[Z] - v1[Z]*v2[Y];
    cross[Y] = v1[Z]*v2[X] - v1[X]*v2[Z];
    cross[Z] = v1[X]*v2[Y] - v1[Y]*v2[X];
  
  } // cross

  /////////////////////////////////////////////////////////////////

  private static float dot (float[] v1, float[] v2) {
    
    float dot = v1[X]*v2[X] + v1[Y]*v2[Y] + v1[Z]*v2[Z];
    return (dot);
  
  } // dot

  /////////////////////////////////////////////////////////////////

  private static float magnitude (float[] v) {
  
    float sum = 0;
    for (int i = 0; i < 3; i++) sum += v[i]*v[i];
    float mag = (float) Math.sqrt (sum);

    return (mag);

  } // magnitude
  
  /////////////////////////////////////////////////////////////////

  private static void normalize (float[] p) {

    normalize (p, p);

  } // normalize

  /////////////////////////////////////////////////////////////////

  private static void normalize (float[] p, float[] norm) {

    scale (p, 1/magnitude (p), norm);

  } // normalize

  /////////////////////////////////////////////////////////////////

  private static void scale (float[] v, float factor, float[] scaled) {

    for (int i = 0; i < 3; i++) scaled[i] = factor * v[i];
  
  } // scale

  /////////////////////////////////////////////////////////////////

  private static void scale (float[] v, float factor) {

    scale (v, factor, v);
  
  } // scale

  /////////////////////////////////////////////////////////////////

  private static void add (float[] v1, float[] v2, float[] sum) {

    for (int i = 0; i < 3; i++) sum[i] = v1[i] + v2[i];
  
  } // add

  /////////////////////////////////////////////////////////////////

  private static void addUnit (float[] v1, float[] unit, float len, float[] sum) {

    for (int i = 0; i < 3; i++) sum[i] = v1[i] + len*unit[i];
  
  } // addUnit

  /////////////////////////////////////////////////////////////////

  /** Holds a set of int values that can be used as a key into a hash map. */
  private static class PointKey {
  
    int[] point = new int[3];
    
    @Override
    public boolean equals (Object o) { return (Arrays.equals (point, ((PointKey) o).point)); }

    @Override
    public int hashCode() { return (Arrays.hashCode (point)); }

  } // PointKey class

  /////////////////////////////////////////////////////////////////

  /**
   * The TriangleGroup class holds data about a group of triangles.  The points
   * in the group pf triangles are stored in such a way as to avoid storing
   * duplicate points for triangles that share vertices.
   */
  private static class TriangleGroup {

    private static final float COORD_RESOLUTION = 1e-6f;
      
    private PointKey probeKey = new PointKey();
    private float[] xyzPoint = new float[3];
    private HashMap<PointKey, Integer> pointHash = new HashMap<>();
    
    List<float[]> points = new ArrayList<>();
    Map<Integer, Set<Triangle>> sharedPoints = new HashMap<>();

    public int insertPoint (float[] point) {

      sphereToPoint (point, 1, xyzPoint);
      for (int i = X; i <= Z; i++)
        probeKey.point[i] = Math.round (xyzPoint[i]/COORD_RESOLUTION);
      Integer pointIndex = pointHash.get (probeKey);
      
      if (pointIndex == null) {
        PointKey key = new PointKey();
        for (int i = X; i <= Z; i++) key.point[i] = probeKey.point[i];
        pointIndex = pointHash.size();
        pointHash.put (key, pointIndex);
        points.add (point);
      } // if

      return (pointIndex);
      
    } // insertPoint
    
    public void insertTriangle (Triangle tri) {
    
      for (int i = 0; i < 3; i++) {
        Set<Triangle> shared = sharedPoints.get (tri.corners[i]);
        if (shared == null) { shared = new HashSet<>(); sharedPoints.put (tri.corners[i], shared); }
        shared.add (tri);
      } // for
    
    } // insertTriangle

    public Triangle createTriangle (int p1Index, int p2Index, int p3Index) {

      Triangle tri = new Triangle (this, new int[] {p1Index, p2Index, p3Index});
      insertTriangle (tri);
      
      return (tri);

    } // createTriangle

    public Triangle createTriangle (float[] p1, float[] p2, float[] p3) {

      return (createTriangle (insertPoint (p1), insertPoint (p2), insertPoint (p3)));
      
    } // createTriangle

  } // TriangleGroup

  /////////////////////////////////////////////////////////////////

  /**
   * The Triangle class holds data about a triangle's corner points and
   * children if any.  The corner point coordinates are actually stored in
   * the TriangleGroup that the triangle belongs to, so that corner points
   * may be shared among triangles if needed.  This helps when building a
   * triangle mesh with many adjacent triangles.
   */
  private static class Triangle {

    TriangleGroup group;
    int[] corners;
    List<Triangle> children;

    public Triangle (
      TriangleGroup group,
      int[] corners
    ) {

      this.group = group;
      this.corners = corners;

    } // Triangle
    
    public float[] p1Coord() { return (group.points.get (corners[0])); }
    public float[] p2Coord() { return (group.points.get (corners[1])); }
    public float[] p3Coord() { return (group.points.get (corners[2])); }
    public int p1() { return (corners[0]); }
    public int p2() { return (corners[1]); }
    public int p3() { return (corners[2]); }

    @Override
    public String toString() {

      StringBuilder builder = new StringBuilder();
      builder.append ("Triangle[");
      for (int i = 0; i < 3; i++) {
        float[] p = group.points.get (corners[i]);
        builder.append ("p" + (i+1) + "=[" + Math.toDegrees (p[THETA]) + "," + Math.toDegrees (p[PHI]) + "]");
        if (i < 2) builder.append (",");
      } // for
      builder.append ("]");

      return (builder.toString());

    } // toString

  } // Triangle class

  /////////////////////////////////////////////////////////////////

  /**
   * Compute a point in spherical coordinates midway between two existing
   * points.
   *
   * @param p1 the first point as [theta, phi] in radians.
   * @param p2 the second point as [theta, phi] in radians.
   */
  private static float[] mid (
    float[] p1,
    float[] p2
  ) {

    // Find the midway point in cartesian coordinates
    double p1x = Math.sin (p1[THETA]) * Math.cos (p1[PHI]);
    double p1y = Math.sin (p1[THETA]) * Math.sin (p1[PHI]);
    double p1z = Math.cos (p1[THETA]);

    double p2x = Math.sin (p2[THETA]) * Math.cos (p2[PHI]);
    double p2y = Math.sin (p2[THETA]) * Math.sin (p2[PHI]);
    double p2z = Math.cos (p2[THETA]);

    double x = (p1x+p2x)/2;
    double y = (p1y+p2y)/2;
    double z = (p1z+p2z)/2;

    // Convert back to spherical
    double phi = Math.atan2 (y, x);
    double arg = z / Math.sqrt (x*x + y*y + z*z);
    if (arg > 1) arg = 1;
    double theta = Math.acos (arg);
    float[] mid = new float[] {(float) theta, (float) phi};
    
    return (mid);

  } // mid

  /////////////////////////////////////////////////////////////////

  /**
   * Subdivides a triangle into four children triangles by bisecting each side.
   *
   * @param tri the triangle to subdivide.
   * @param subList the list to put the children into.
   */
  private static void subdivideTriangle (
    Triangle tri,
    List<Triangle> subList
  ) {

    // The parent triangle will look something like this:
    //
    //      p1                        p1------p3
    //     /  \           or            \    /
    //    /    \                         \  /
    //  p2------p3                        p2
    //

    // Create a set of spherical coordinates along each edge and insert those
    // new coordinates into the group
    TriangleGroup group = tri.group;
    float[] mid12 = mid (tri.p1Coord(), tri.p2Coord());
    int p12 = group.insertPoint (mid12);
    float[] mid23 = mid (tri.p2Coord(), tri.p3Coord());
    int p23 = group.insertPoint (mid23);
    float[] mid31 = mid (tri.p3Coord(), tri.p1Coord());
    int p31 = group.insertPoint (mid31);

    // Create a list by adding the four children:
    //
    //                    p1
    //                   /  \
    //                  /    \
    //                p12----p31
    //                /  \   / \
    //               /    \ /   \
    //              p2----p23----p3
    
    tri.children = new ArrayList<>();
    tri.children.add (group.createTriangle (tri.p1(), p12, p31));
    tri.children.add (group.createTriangle (p12, tri.p2(), p23));
    tri.children.add (group.createTriangle (p31, p23, tri.p3()));
    tri.children.add (group.createTriangle (p31, p12, p23));

    subList.addAll (tri.children);

  } // subdivideTriangle

  /////////////////////////////////////////////////////////////////

  /**
   * Subdivides a triangle by a certain number of levels.  Each level
   * divides the triangle into four triangles of roughly equal area by
   * bisecting each side of the parent.
   *
   * @param tri the triangle to subdivide.
   * @param levels the number of levels of subdivision, starting at 0.
   *
   * @return the list of triangles resulting from the subdivision.
   */
  private static List<Triangle> subdivideTriangle (
    Triangle tri,
    int levels
  ) {

    // Create an initial list to subdivide
    List<Triangle> triList = new ArrayList<>();
    triList.add (tri);

    // For each level, subdivide each triangle in the current list and store
    // the results for the next iteration
    for (int i = 0; i < levels; i++) {
      List<Triangle> subdivided = new ArrayList<>();
      for (Triangle parent : triList)
        subdivideTriangle (parent, subdivided);
      triList = subdivided;
    } // for

    return (triList);

  } // subdivideTriangle
  
  /////////////////////////////////////////////////////////////////

  /**
   * Converts spherical coordinates to graphics space coordinates.
   *
   * @param sphere the spherical coordinates as [theta, phi] in radians.  Theta
   * is the angle down from the vertical axis, and phi is the angle around the
   * vertical axis, starting from the positive x axis towards the y axis
   * (right-handed system).
   * @param radius the radius of the surface to convert for.
   * @param point the output cartesian coordinates as [x,y,z].  In graphics
   * space, z is into the screen, y is down and x is to the right.
   */
  private static void sphereToPoint (
    float[] sphere,
    float radius,
    float[] point
  ) {
  
    // Extract the spherical angles
    double theta = sphere[THETA];
    double phi = sphere[PHI];
    
    // These are originally x, y, z but we have to rearrange for the
    // graphics space axes
    double z = - radius * Math.sin (theta) * Math.cos (phi);
    double x = radius * Math.sin (theta) * Math.sin (phi);
    double y = - radius * Math.cos (theta);

    point[X] = (float) x;
    point[Y] = (float) y;
    point[Z] = (float) z;
  
  } // sphereToPoint

  /////////////////////////////////////////////////////////////////

  /**
   * Adds the specified list of triangles to the mesh.
   *
   * @param mesh the mesh to add the triangles to.
   * @param group the group that the triangles belong to.
   * @param triangleList the list of triangles to add.
   * @param radius the radius to use for converting spherical coordinates
   * to cartesian.
   */
  private static void addTriangles (
    TriangleMesh mesh,
    TriangleGroup group,
    List<Triangle> triangleList,
    float radius
  ) {

    // We make an untextured surface here (we have an implementation for
    // textured surfaces but it's complicated so we omit it).
    ObservableFloatArray texCoords = mesh.getTexCoords();
    texCoords.addAll (0, 0);

    // Add all the points in the group to the mesh
    ObservableFloatArray meshPoints = mesh.getPoints();
    meshPoints.ensureCapacity (group.points.size() * 3);
    float[] point = new float[3];
    for (float[] sphPoint : group.points) {
      sphereToPoint (sphPoint, radius, point);
      meshPoints.addAll (point[X], point[Y], point[Z]);
    } // for

    // Add all the triangle faces, referencing the points we just added
    ObservableFaceArray meshFaces = mesh.getFaces();
    meshFaces.ensureCapacity (triangleList.size() * 6);
    for (Triangle tri : triangleList)
      meshFaces.addAll (tri.p1(), 0, tri.p2(), 0, tri.p3(), 0);

  } // addTriangles

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a spherical polyhedron made up of a series of subdivied triangles,
   * based initially on an icosahedron.
   *
   * @param radius the radius of the sphere.
   * @param levels the number of levels to subdivie each of the original
   * triangles, starting at 0 for no subdivision.
   *
   * @return the newly created mesh.
   */
  public TriangleMesh createGeodesicPolyhedron (
    float radius,
    int levels
  ) {
  
    // Initially create the parent triangles.  We start with five sides,
    // and each side is built from the top down, four triangles per side,
    // for 20 triangles total.  We use spherical coordinates (theta, phi) where
    // theta is the angle down from the vertical axis (ie: theta = yhat . v)
    // analagous to latitude on the Earth, and phi is the angle around the
    // vertical axis, analagous to longitude.
    TriangleGroup group = new TriangleGroup();
    List<Triangle> parentList = new ArrayList<>();
    float theta = PI/2 - (float) Math.atan (0.5);
    float phi = TWO_PI/5;
    for (int side = 0; side < 5; side++) {
      parentList.add (group.createTriangle (
        new float[] {0, (side+0.5f)*phi},
        new float[] {theta, side*phi},
        new float[] {theta, (side+1)*phi}
      ));
      parentList.add (group.createTriangle (
        new float[] {theta, side*phi},
        new float[] {PI-theta, side*phi + phi/2},
        new float[] {theta, (side+1)*phi}
      ));
      parentList.add (group.createTriangle (
        new float[] {PI-theta, side*phi + phi/2},
        new float[] {PI-theta, (side+1)*phi + phi/2},
        new float[] {theta, (side+1)*phi}
      ));
      parentList.add (group.createTriangle (
        new float[] {PI-theta, side*phi + phi/2},
        new float[] {PI, (side+0.5f)*phi + phi/2},
        new float[] {PI-theta, (side+1)*phi + phi/2}
      ));
    } // for

    // Next we subdivide each of the parent triangles to some number
    // of levels.
    List<Triangle> childList = new ArrayList<>();
    for (Triangle tri : parentList)
      childList.addAll (subdivideTriangle (tri, levels));

    // Finally, we make a triangular mesh by converting all the triangles
    // to (x,y,z) points on the surface of a sphere with a certain radius.
    TriangleMesh mesh = new TriangleMesh();
    addTriangles (mesh, group, childList, radius);

    return (mesh);
  
  } // createGeodesicPolyhedron

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a polyline mesh by connecting together a series of points with
   * line segments that follow along the surface of a sphere.
   *
   * @param width the width of the line segments.
   * @param radius the radius of the sphere.
   * @param pointList the list of points to connect as spherical
   * coordinates [theta, phi] in radians.
   * @param mesh the mesh to add points and faces to, or null to create a new
   * mesh.
   *
   * @return the newly created mesh.
   */
  public TriangleMesh createSphericalPolyline (
    float width,
    float radius,
    List<float[]> pointList,
    TriangleMesh mesh
  ) {

    // We check the point list here and then initialize a mesh if needed
    if (pointList.size() < 2) throw new IllegalArgumentException ("Point list size < 2");
    if (mesh == null) {
      mesh = new TriangleMesh();
      mesh.getTexCoords().addAll (0, 0);
    } // if

    // The counts of various items
    int points = pointList.size();
    int segments = points - 1;
    int cornersPerSegment = 4;
    int trianglesPerSegment = 2;
    
    // Make sure there is capacity for the new points and faces
    ObservableFloatArray meshPoints = mesh.getPoints();
    meshPoints.ensureCapacity (meshPoints.size() + segments * cornersPerSegment * 3);
    ObservableFaceArray meshFaces = mesh.getFaces();
    meshFaces.ensureCapacity (meshFaces.size() + segments * trianglesPerSegment * 6);

    // Initialize the process of converting each point
    float[] p1 = new float[3];    // first point in current segment
    float[] p2 = new float[3];    // second point in current segment
    float[] p1p2 = new float[3];  // vector from p1 to p2
    float[] q = new float[3];     // vector perpendicular to both p1p2 and surface
    float[] c = new float[3];     // corner point of polyline ribbon
    int pointIndex = meshPoints.size() / 3;

    // Process through each segment and create triangles aligned along
    // the line between points like this:
    //
    //      c1 +-------------------------------+ c3   ^
    //         |                ________/      |      | q
    //      p1 +        _______/               + p2   |
    //         |      /                        |
    //      c2 +-------------------------------+ c4
    //
    sphereToPoint (pointList.get (0), radius, p1);
    for (float[] sphere : pointList.subList (1, pointList.size())) {

      // Compute q unit vector which points from c1 to c2
      sphereToPoint (sphere, radius, p2);
      fromto (p1, p2, p1p2);
      cross (p1, p1p2, q);
      normalize (q);

      // Compute c1 and c2, the corners either side of p1
      addUnit (p1, q, width/2, c);
      meshPoints.addAll (c[X], c[Y], c[Z]);
      int c1 = pointIndex;
      
      addUnit (p1, q, -width/2, c);
      meshPoints.addAll (c[X], c[Y], c[Z]);
      int c2 = pointIndex + 1;

      // Compute c3 and c4, the corners either side of p2
      addUnit (p2, q, width/2, c);
      meshPoints.addAll (c[X], c[Y], c[Z]);
      int c3 = pointIndex + 2;

      addUnit (p2, q, -width/2, c);
      meshPoints.addAll (c[X], c[Y], c[Z]);
      int c4 = pointIndex + 3;

      // Create two triangles (counter clockwise winding order)
      meshFaces.addAll (c1, 0, c2, 0, c3, 0);
      meshFaces.addAll (c4, 0, c3, 0, c2, 0);

      // Get ready for the next segment
      pointIndex += cornersPerSegment;
      float[] temp = p1;
      p1 = p2;
      p2 = temp;


      // TODO: We really need to connect segments better.  The way it is,
      // there are little gaps visible when you zoom in.


    } // for

    return (mesh);

  } // createSphericalPolyline

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a set of graticule lines of 10 degree spacing over the surface
   * of a sphere.
   *
   * @param width the width of the line segments.
   * @param radius the radius of the sphere.
   *
   * @return the newly created mesh.
   */
  public TriangleMesh createSphericalGraticule (
    float width,
    float radius
  ) {

    TriangleMesh mesh = null;
    
    // Make lines of constant latitude
    for (int lat = -80; lat <= 80; lat += 10) {
      List<float[]> pointList = new ArrayList<>();
      int segments = (int) Math.round (10*Math.cos (Math.toRadians (lat)));
      float deg = 10.0f/segments;
      for (int lon = 0; lon < 360; lon += 10) {
        for (int segment = 0; segment < segments; segment++) {
          pointList.add (new float[] {
            (float) Math.toRadians (90 - lat),
            (float) Math.toRadians (lon + segment*deg)
          });
        } // for
      } // for
      pointList.add (new float[] {
        (float) Math.toRadians (90 - lat), 0
      });
      mesh = createSphericalPolyline (width, radius, pointList, mesh);
    } // for

    // Make lines of constant longitude
    for (int lon = 0; lon < 360; lon += 10) {
      List<float[]> pointList = new ArrayList<>();
      int latMax = (lon == 0 || lon == 90 || lon == 180 || lon == 270 ? 90 : 80);
      for (int lat = -latMax; lat < latMax; lat += 10) {
        for (int segment = 0; segment < 10; segment++) {
          pointList.add (new float[] {
            (float) Math.toRadians (90 - (lat + segment)),
            (float) Math.toRadians (lon)
          });
        } // for
      } // for
      pointList.add (new float[] {
        (float) Math.toRadians (90 - latMax),
        (float) Math.toRadians (lon)
      });
      mesh = createSphericalPolyline (width, radius, pointList, mesh);
    } // for

    return (mesh);

  } // createSphericalGraticule

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a spherical patch of a specified angle spacing over the surface
   * of a sphere.
   *
   * @param start the starting location of the patch as [theta, phi] in radians.
   * @param count the count of boxes in each direction as [thetaCount, phiCount].
   * @param size the size of mesh boxes as [theta, phi] in radians.
   * @param radius the radius of the sphere.
   *
   * @return the newly created mesh.
   */
  public TriangleMesh createSphericalPatch (
    float[] start,
    int[] count,
    float[] size,
    float radius
  ) {

    TriangleMesh mesh = new TriangleMesh();

    // Create an untextured surface
    ObservableFloatArray texCoords = mesh.getTexCoords();
    texCoords.addAll (0, 0);

    // Create the bounding points for each box
    ObservableFloatArray meshPoints = mesh.getPoints();
    float[] sphere = new float[2];
    float[] point = new float[3];
    for (int i = 0; i <= count[THETA]; i++) {
      sphere[THETA] = start[THETA] + i*size[THETA];
      for (int j = 0; j <= count[PHI]; j++) {
        sphere[PHI] = start[PHI] + j*size[PHI];
        sphereToPoint (sphere, radius, point);
        meshPoints.addAll (point[X], point[Y], point[Z]);
      } // for
    } // for

    // Add all the triangle faces, referencing the points we just added
    ObservableFaceArray meshFaces = mesh.getFaces();
    for (int i = 0; i < count[THETA]; i++) {
      for (int j = 0; j < count[PHI]; j++) {
        int p1 = i*(count[PHI]+1) + j;
        int p2 = p1+1;
        int p3 = p1 + (count[PHI]+1);
        int p4 = p3+1;
        meshFaces.addAll (p1, 0, p3, 0, p2, 0);
        meshFaces.addAll (p2, 0, p3, 0, p4, 0);
      } // for
    } // for

    return (mesh);
  
  } // createSphericalPatch

  /////////////////////////////////////////////////////////////////

} // MeshObjectFactory
