package noaa.coastwatch.vertigo.coord;

import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class EllipsoidMercatorTest {

  private EllipsoidMercator mercAtEquator = EllipsoidMercator.getInstance (
    0.0,
    0.0,
    Double.NaN,
    0.0,
    0.0,
    6378137.0,
    6356752.314245,
    Double.NaN,
    "m"
  );
  private EllipsoidMercator mercAt30 = EllipsoidMercator.getInstance (
    0.0,
    30.0,
    Double.NaN,
    0.0,
    0.0,
    6378137.0,
    6356752.314245,
    Double.NaN,
    "m"
  );
  
  private double lat = 48 + 39/60.0;
  private double lon = -(123 + 26/60.0);
  
  @Test
  void testCopyEqualsHash () {

    var copy = mercAtEquator.constructCopy();
    assertEquals (mercAtEquator, copy);
    assertEquals (mercAtEquator.hashCode(), copy.hashCode());

    assertNotEquals (mercAtEquator, mercAt30);

  }

  @Test
  void testProjection () {
  
    var latLonCoord = new LatLonPointImpl (lat, lon);
    var mapCoord = new ProjectionPointImpl();
    mercAtEquator.latLonToProj (latLonCoord, mapCoord);
    assertFalse (mapCoord.isInfinite());

    var latLonCoordNew = new LatLonPointImpl();
    mercAtEquator.projToLatLon (mapCoord, latLonCoordNew);
    assertFalse (Double.isInfinite (latLonCoordNew.getLatitude()));
    assertFalse (Double.isInfinite (latLonCoordNew.getLongitude()));
    assertTrue (latLonCoord.nearlyEquals (latLonCoordNew));

    var latLonCoordCorrupt = new LatLonPointImpl (lat, Double.NaN);
    var mapCoordCorrupt = new ProjectionPointImpl();
    mercAtEquator.latLonToProj (latLonCoordCorrupt, mapCoordCorrupt);
    assertTrue (mapCoordCorrupt.isInfinite());

    mapCoordCorrupt.setLocation (mapCoord.getX(), Double.NaN);
    latLonCoordCorrupt.set (0, 0);
    mercAtEquator.projToLatLon (mapCoordCorrupt, latLonCoordCorrupt);
    assertFalse (Double.isInfinite (latLonCoordCorrupt.getLatitude()));
    assertFalse (Double.isInfinite (latLonCoordCorrupt.getLongitude()));

  }
  
  @Test
  void testCrossSeam () {
  
    var latLonCoord1 = new LatLonPointImpl (lat, lon);
    var mapCoord1 = new ProjectionPointImpl();
    mercAtEquator.latLonToProj (latLonCoord1, mapCoord1);

    var latLonCoord2 = new LatLonPointImpl (lat, -lon);
    var mapCoord2 = new ProjectionPointImpl();
    mercAtEquator.latLonToProj (latLonCoord2, mapCoord2);

    assertTrue (mercAtEquator.crossSeam (mapCoord1, mapCoord2));

    mapCoord2.setX (mapCoord1.getX());
    mapCoord2.setY (-mapCoord1.getY());
    assertFalse (mercAtEquator.crossSeam (mapCoord1, mapCoord2));

    mapCoord2.setY (Double.POSITIVE_INFINITY);
    assertTrue (mercAtEquator.crossSeam (mapCoord1, mapCoord2));

  }

}

