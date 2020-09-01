/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>RowIterator</code> class iterates over a single row of image data.
 *
 * @author Peter Hollemans
 * @since 0.5
 */
public class RowIterator implements ImageCoordinateIterator {

  /** The current position and image width. */
  private int x, y, width;

  @Override
  public boolean hasNext() { return (x < width-1); }

  @Override
  public void next() { x++; }

  @Override
  public int getX() { return (x); }

  @Override
  public int getY() { return (y); }

  @Override
  public void reset() { this.x = -1; }

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new row iterator.  The row defaults to 0.  The {@link #setRow}
   * method should be called prior to iteration to set the row.
   *
   * @param width the width of the row.
   */
  public RowIterator (
    int width
  ) {
  
    this.width = width;
    reset();
    
  } // RowInterator

  /////////////////////////////////////////////////////////////////

  /**
   * Sets a new row value and resets the iterator.
   *
   * @param y the new row value to use.
   */
  public void setRow (int y) { this.y = y; reset(); }

  /////////////////////////////////////////////////////////////////

} // RowIterator class
