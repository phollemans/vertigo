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
  private Map<String, Object> propertyMap = new HashMap<>();

  /** The list of properties that have been requested. */
  private Map<String, Object> specMap = new HashMap<>();

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
    
    specMap.put (property, value);
    
    return (value);
  
  } // require

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the current set of property names in the map.
   *
   * @return the property names.
   *
   * @since 0.6
   */
  protected Set<String> properties () { return (propertyMap.keySet()); }

  /////////////////////////////////////////////////////////////////

  /**
   * Completes the builder operation by clearing the property map and
   * transferring the build specifications to the object.
   *
   * @param object the object to complete building.
   *
   * @since 0.6
   */
  protected void complete (
    ProjectObject object
  ) {
  
    propertyMap.clear();
    object.setSpec (specMap);
    specMap.clear();
    
  } // complete

  /////////////////////////////////////////////////////////////////

  @Override
  public String toString () {

    StringBuilder builder = new StringBuilder();
    builder.append ("BaseProjectObjectBuilder[");
    if (propertyMap.size() != 0) {
      propertyMap.keySet().forEach (key -> builder.append (key + "=" + propertyMap.get (key) + ","));
      builder.deleteCharAt (builder.length()-1);
    } // if
    builder.append ("]");
    return (builder.toString());
  
  } // toString

  /////////////////////////////////////////////////////////////////

} // BaseProjectObjectBuilder class





