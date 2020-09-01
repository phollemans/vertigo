
/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ImageAccessResult</code> class holds data about an access to
 * image data and the results and assists in retrival of the result values.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class ImageAccessResult {

  /** The access used to create this result. */
  public ImageAccess access;

  /** The result context to use in retrieving data. */
  public Object context;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new image access result.
   *
   * @param access the access used to create the context of the result.
   * @param context the context object for this result.
   */
  public ImageAccessResult (
    ImageAccess access,
    Object context
  ) {
  
    this.access = access;
    this.context = context;
  
  } // ImageAccessResult

  /////////////////////////////////////////////////////////////////

} // ImageAccessResult class
