/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;

/**
 * The <code>BaseProjectObjectBuilder</code> class is a useful base class
 * for project object builders.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public abstract class BaseProjectObjectBuilder implements ProjectObjectBuilder {

  /** The list of properties collected for the next object production. */
  protected Map<String, Object> propertyMap = new HashMap<>();

  /////////////////////////////////////////////////////////////////

  @Override
  public void setProperty (
    String name,
    Object value
  ) {
  
    propertyMap.put (name, value);
  
  } // setProperty

  /////////////////////////////////////////////////////////////////

  /**
   * Retrieves a required property value from the map.
   *
   * @param property the property value to retrieve.
   *
   * @return the property value.
   *
   * @throws RuntimeException if the property was not found.
   */
  protected Object require (
    String property
  ) {

    return (require (property, null));

  } // require
  
  /////////////////////////////////////////////////////////////////

  /**
   * Retrieves a required property value from the map.
   *
   * @param property the property value to retrieve.
   * @param accepted the accepted values for the property, or null for any.
   *
   * @return the property value.
   *
   * @throws RuntimeException if the property was not found, or if its value
   * isn't in the list of accepted values.
   */
  protected Object require (
    String property,
    Set<Object> accepted
  ) {
    
    if (!propertyMap.containsKey (property))
      throw new RuntimeException ("Object builder " + getTypeName() + " requires missing property '" + property + "'");
  
    Object value = propertyMap.get (property);
    if (accepted != null && !accepted.contains (value))
      throw new RuntimeException ("Object builder " + getTypeName() + " property '" + property + "' has invalid value '" + value + "'");
    
    return (value);
  
  } // require

  /////////////////////////////////////////////////////////////////

} // BaseProjectObjectBuilder class





