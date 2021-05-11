/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ProjectObjectBuilder</code> interface is for classes that create
 * project objects in a step-by-step build process.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public interface ProjectObjectBuilder {

  /**
   * Gets the object type name that this builder creates.
   *
   * @return the type name.
   */
  String getTypeName();
  
  /**
   * Sets a property value for this builder to use for creating the object.
   *
   * @param name the property name.
   * @param value the property value.
   */
  void setProperty (
    String name,
    Object value
  );

  /**
   * Gets the finished project object created by this builder.  After this
   * method is called, the builder is initialized to create another object.
   *
   * @return the object created from the properties passed to the builder.
   *
   * @throws RuntimeException if the set of properties most recently passed
   * to the builder cannot be used to create an object.
   */
  ProjectObject getObject();

} // ProjectObjectBuilder interface



