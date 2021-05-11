/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>FacetDataSource</code> class holds the factories needed to
 * create instances of {@link Facet} objects.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class FacetDataSource {

  // Variables
  // ---------

  /** The number of facets in this data source. */
  private int facets;
  
  /** The factory that produces mesh objects for the facets. */
  private MeshFactory meshFactory;
  
    /** The factory that produces texture images for the facets. */
  private TextureFactory textureFactory;
  
  /////////////////////////////////////////////////////////////////
  
  /**
   * Gets the mesh factory for facet data.
   *
   * @return the mesh factory.
   */
  public MeshFactory getMeshFactory() { return (meshFactory); }

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the texture factory for facet data.
   *
   * @return the texture factory.
   */
  public TextureFactory getTextureFactory() { return (textureFactory); }

  /////////////////////////////////////////////////////////////////

  protected FacetDataSource () {}

  /////////////////////////////////////////////////////////////////

  /**
   * Initializes this data source, for use by subclasses.
   *
   * @param facts the number of facets produced by the factories.
   * @param meshFactory the mesh factory for facet mesh data.
   * @param textureFactory the texture factory for facet image data.
   */
  protected void init (
    int facets,
    MeshFactory meshFactory,
    TextureFactory textureFactory
  ) {

    this.facets = facets;
    this.meshFactory = meshFactory;
    this.textureFactory = textureFactory;

  } // init

  /////////////////////////////////////////////////////////////////

  /**
   * Gets the number of facets produced by the factories.
   *
   * @return the facet count.
   */
  public int getFacets() { return (facets); }

  /////////////////////////////////////////////////////////////////

} // FacetDataSource class

