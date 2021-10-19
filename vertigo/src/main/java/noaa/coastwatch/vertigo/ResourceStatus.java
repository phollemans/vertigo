/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>ResourceStatus</code> class holds a resource's state and error
 * data.
 */
public class ResourceStatus {

  /** The possible values for access status type. */
  public enum State { UNAVAILABLE, AVAILABLE, ERROR; }

  /** The current state of the resource status. */
  private State state;

  /** The error value if in the error state. */
  private Exception error;

  /////////////////////////////////////////////////////////////////

  /**
   * Creates a new instance with state UNAVAILABLE.
   *
   * @return the new instance.
   */
  public static ResourceStatus unavailable() { return (new ResourceStatus (State.UNAVAILABLE)); }

  /**
   * Creates a new instance with state AVAILABLE.
   *
   * @return the new instance.
   */
  public static ResourceStatus available() { return (new ResourceStatus (State.AVAILABLE)); }

  /**
   * Creates a new instance with state ERROR.
   *
   * @param error the error to use for the instance.
   * 
   * @return the new instance.
   */
  public static ResourceStatus error (Exception error) { return (new ResourceStatus (error)); }

  protected ResourceStatus (State state) { this.state = state; }
  protected ResourceStatus (Exception error) { this.state = State.ERROR; this.error = error; }

  /**
   * Gets the status state.
   *
   * @return the state value.
   */
  public State getState() { return (state); }

  /**
   * Gets the status error object.
   *
   * @return the error value.
   */
  public Exception getError() { return (error); }

  /////////////////////////////////////////////////////////////////

} // ResourceStatus class
