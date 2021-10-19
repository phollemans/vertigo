/*
 * Vertigo Project
 * Copyright (c) 2020 National Oceanic and Atmospheric Administration
 * All rights reserved.
 */

package noaa.coastwatch.vertigo;

/**
 * The <code>IllegalLogarithmicRangeException</code> class is used for exceptions
 * thrown by {@link LogarithmicAxis} when a bound value isn't supported by the axis.
 * The code was accessed on Oct 10, 2020 from the
 * <a href="http://blog.dooapp.com/2013/06/logarithmic-scale-strikes-back-in.html">doapp.com</a>
 * website and modified as needed.
 *
 * @author Kevin Senechal
 *
 * @since 0.6
 */
public class IllegalLogarithmicRangeException extends Exception {

	/**
   * Creates a new range exception.
   *
	 * @param message the exception message.
	 */
	public IllegalLogarithmicRangeException (String message) {

		super (message);

	} // IllegalLogarithmicRangeException

}

