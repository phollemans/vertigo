module coastwatch.vertigo {

  requires javafx.controls;
  requires javafx.fxml;
  requires javafx.swing;
  requires javafx.web;
  requires java.desktop;
  requires java.logging;  
  requires com.github.albfernandez.javadbf;
  requires shapefilereader;
  requires netcdfAll;
  requires org.jsoup;
  requires org.commonmark;

  opens noaa.coastwatch.vertigo to javafx.fxml;
  exports noaa.coastwatch.vertigo;

}
