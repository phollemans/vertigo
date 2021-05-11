/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;

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

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * The <code>Palette</code> class reads and writes color palette XML files.
 * The file has the structure:
 * <pre>
 *   palette name=... {
 *     color r=... g=... b=...
 *     color r=... g=... b=...
 *     color r=... g=... b=...
 *   }
 * </pre>
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class Palette extends BaseProjectObject {

  private static final Logger LOGGER = Logger.getLogger (Palette.class.getName());

  /** The set of palette previously created (possibly empty). */
  private static Map<String, Palette> paletteCache = new HashMap<>();

  /** The list of colors in this palette. */
  private List<Integer> colorList;

  /////////////////////////////////////////////////////////////////

  protected Palette () { }

  /////////////////////////////////////////////////////////////////

  /**
   * Reads a palette from an XML file.
   *
   * @param xmlFile the file to read.
   *
   * @return the palette containing the colors specified by the XML file.
   *
   * @throws IOException if an error occured reading the XML and translating
   * the data into colors.
   */
  private static Palette readFromXML (
    URL xmlFile
  ) throws IOException {

    Palette palette = new Palette();
    palette.colorList = new ArrayList<>();
    
    try (InputStream stream = xmlFile.openStream()) {

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
        palette.colorList.add (color);
      } // for

      expr = xpath.compile ("/palette");
      nodes = (NodeList) expr.evaluate (doc, XPathConstants.NODESET);
      Element element = (Element) nodes.item (0);
      palette.setName (element.getAttribute ("name"));

      LOGGER.fine ("Read palette " + palette.getName() + " with " + palette.colorList.size() + " colors");

    } // try
    
    catch (Exception e) { throw new IOException (e); }

    return (palette);

  } // readFromXML

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an instance of a palette by name.
   *
   * @param name the palette name.
   *
   * @return the palette or null if one could not be found.
   */
  public static Palette getInstance (
    String name
  ) {
  
    Palette palette = paletteCache.get (name);
    if (palette == null) {
      var xml = Palette.class.getResource ("palettes/" + name + ".xml");
      try {
        palette = readFromXML (xml);
        paletteCache.put (name, palette);
      } // try
      catch (IOException e) {
        LOGGER.log (Level.WARNING, "Read failed for XML file " + xml, e);
      } // catch
    } // if
    
    return (palette);

  } // getInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Gets an instance of a palette using its name and colors.  The palette is
   * also cached for later use.
   *
   * @param name the palette name.
   * @param colorList the color list as ARGB integer values.
   *
   * @return the new palette instance.
   */
  public static Palette getInstance (
    String name,
    List<Integer> colorList
  ) {

    Palette palette = new Palette();
    palette.setName (name);
    palette.colorList = new ArrayList<> (colorList);
    paletteCache.put (name, palette);
  
    return (palette);
  
  } // getInstance

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the colors in this palette as an unmodifiable list of ARGB integer
   * values.
   *
   * @return the unmodifiable list of colors.
   */
  public List<Integer> getColors() { return (Collections.unmodifiableList (colorList)); }

  /////////////////////////////////////////////////////////////////

} // Palette class


