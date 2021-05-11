module coastwatch.vertigo {

  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.swing;
  requires javafx.web;
  requires java.desktop;
  requires java.prefs;
  requires java.logging;  
//  requires com.github.albfernandez.javadbf;
//  requires shapefilereader;
//  requires cdm;
  requires org.jsoup;
  requires org.commonmark;
  requires proj4j;
  requires cdm.core;
  
  opens noaa.coastwatch.vertigo to javafx.fxml;
  opens noaa.coastwatch.vertigo.coord to cdm.core;
  exports noaa.coastwatch.vertigo;

}
