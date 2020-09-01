/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.TreeSet;
import java.util.List;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;

/**
 * The <code>PaletteBuilder</code> class builds a {@link Palette} from a series
 * of property values.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class PaletteBuilder extends BaseProjectObjectBuilder {

  private static final String TYPE = "palette";
  
  /////////////////////////////////////////////////////////////////

  @Override
  public String getTypeName() { return (TYPE); }

  /////////////////////////////////////////////////////////////////

  /** Converts a color to an ARGB value. */
  private int colorToRGB (Color color) {

    int intValue = 0;
    intValue = intValue | (((int) Math.round (color.getOpacity()*255)) << 24);
    intValue = intValue | (((int) Math.round (color.getRed()*255)) << 16);
    intValue = intValue | (((int) Math.round (color.getGreen()*255)) << 8);
    intValue = intValue | ((int) Math.round (color.getBlue()*255));

    return (intValue);
  
  } // colorToRGB

  /////////////////////////////////////////////////////////////////

  @Override
  public Object getObject() {

    String name = (String) require ("name");
    List<Integer> colorList = propertyMap.keySet().stream()
      .filter (key -> key.matches ("color[0-9]+"))
      .map (key -> Integer.parseInt (key.substring (5)))
      .collect (Collectors.toCollection (TreeSet::new))
      .stream()
      .map (key -> colorToRGB ((Color) propertyMap.get ("color" + key)))
      .collect (Collectors.toList());
    Object obj = Palette.getInstance (name, colorList);

    propertyMap.clear();
    return (obj);

  } // getObject

  /////////////////////////////////////////////////////////////////

} // PaletteBuilder class





