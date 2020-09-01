/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

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

import java.net.URL;

import java.util.Date;
import java.util.TimeZone;
import java.util.List;
import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.text.ParseException;

import javafx.scene.paint.Color;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>Project</code> class reads and writes project XML files.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Project {

  private static final Logger LOGGER = Logger.getLogger (Project.class.getName());

  /** The format for dates in XML data. */
  private static SimpleDateFormat dateFormat;

  /** The list of objects in this project. */
  private List<Object> objectList;

  /////////////////////////////////////////////////////////////////

  static {

    dateFormat = new SimpleDateFormat ("yyyy/MM/dd HH:mm");
    dateFormat.setTimeZone (TimeZone.getTimeZone ("UTC"));

  } // static
  
  /////////////////////////////////////////////////////////////////

  protected Project () { objectList = new ArrayList<>(); }

  /////////////////////////////////////////////////////////////////

  /**
   * Reads a project from an XML file.
   *
   * @param xmlFile the file to read.
   * @param builders the list of builders to use for building objects found
   * in the XML file.
   *
   * @return the project containing the objects specified by the XML file.
   *
   * @throws IOException if an error occured reading the XML and translating
   * the data into objects.
   */
  public static Project readFromXML (
    URL xmlFile,
    List<ProjectObjectBuilder> builders
  ) throws IOException {

    Project project = new Project();
    
    try (InputStream stream = xmlFile.openStream()) {

      DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = builderFactory.newDocumentBuilder();
      Document doc = docBuilder.parse (stream);

      XPathFactory xpathFactory = XPathFactory.newInstance();
      XPath xpath = xpathFactory.newXPath();
      XPathExpression propertyExpr = xpath.compile ("property");
      XPathExpression configExpr = xpath.compile ("config/property");

      for (var builder : builders) {

        // For each builder, we get the type name and search for elements
        // off the document root with the type name of the builder.
        XPathExpression builderExpr = xpath.compile ("/vertigo/" + builder.getTypeName());
        NodeList builderNodes = (NodeList) builderExpr.evaluate (doc, XPathConstants.NODESET);
        for (int i = 0; i < builderNodes.getLength(); i++) {

          Element builderElement = (Element) builderNodes.item (i);

          // Inside each tag for the builder, there are "property" tag children
          // which have attributes "name" and "value".
          String name = null;
          NodeList propertyNodes = (NodeList) propertyExpr.evaluate (builderElement, XPathConstants.NODESET);
          for (int j = 0; j < propertyNodes.getLength(); j++) {
            Element propertyElement = (Element) propertyNodes.item (j);
            String propName = propertyElement.getAttribute ("name");
            String propValue = propertyElement.getAttribute ("value");
            String propType = propertyElement.getAttribute ("type");
            if (propName.equals ("name")) name = propValue;
            builder.setProperty (propName, toObject (propValue, propType));
          } // for

          // There may also be "config" tag children, each of which also have
          // a series of name/value property values.
          NodeList configPropertyNodes = (NodeList) configExpr.evaluate (builderElement, XPathConstants.NODESET);
          for (int j = 0; j < configPropertyNodes.getLength(); j++) {
            Element propertyElement = (Element) configPropertyNodes.item (j);
            String propName = propertyElement.getAttribute ("name");
            String propValue = propertyElement.getAttribute ("value");
            String propType = propertyElement.getAttribute ("type");
            builder.setProperty ("config." + propName, toObject (propValue, propType));
          } // for
          boolean hasConfig = (configPropertyNodes.getLength() != 0);

          LOGGER.fine ("Read " + builder.getTypeName() + " object named '" + name +
            "' with " + propertyNodes.getLength() +
            (hasConfig ? "+" + configPropertyNodes.getLength() : "") + " properties");

          // Finally, add the finished object to the list.
          project.objectList.add (builder.getObject());

        } // for

      } // for

      LOGGER.fine ("Found total of " + project.objectList.size() + " project object(s)");

    } // try
    
    catch (Exception e) { throw new IOException (e); }

    return (project);

  } // readFromXML

  /////////////////////////////////////////////////////////////////
  
  /**
   * Gets an object value based on an input string and type.
   *
   * @param value the value to convert.
   * @param type the object type to create.
   *
   * @return the object of the specified type created from the value.
   */
  private static Object toObject (
    String value,
    String type
  ) {
  
    Object obj;
    if (type.equals ("string")) obj = value;
    else if (type.equals ("boolean")) obj = Boolean.valueOf (value);
    else if (type.equals ("int")) obj = Integer.valueOf (value);
    else if (type.equals ("double")) obj = Double.valueOf (value);
    else if (type.equals ("date")) {
      int delta = Integer.MAX_VALUE;
      try { delta = Integer.parseInt (value); }
      catch (NumberFormatException e) { }
      if (delta != Integer.MAX_VALUE)
        obj = new Date (System.currentTimeMillis() + delta*60000);
      else {
        try { obj = dateFormat.parse (value); }
        catch (ParseException e) { throw new NumberFormatException ("Error parsing date value '" + value + "'"); }
      } // else
    } // else
    else if (type.equals ("color")) obj = Color.web (value);
    else throw new IllegalArgumentException ("Project contains unknown object type '" + type + "'");

    return (obj);
    
  } // toObject

  /////////////////////////////////////////////////////////////////

  /**
   * Gets a list of objects of a specified type from the collection of
   * objects contained in this project.
   *
   * @param objectClass the class of project objects to retrieve.
   *
   * @return the list of objects, possibly zero length if the project
   * contains no objects of the specified class.
   */
  public <T> List<T> getObjects (
    Class<T> objectClass
  ) {

    List<T> list = new ArrayList<>();
    for (var object : objectList) {
      if (objectClass.isAssignableFrom (object.getClass()))
        list.add (objectClass.cast (object));
    } // for

    return (list);

  } // getObjects

  /////////////////////////////////////////////////////////////////

} // Project class

