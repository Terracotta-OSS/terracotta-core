package org.terracotta.testing.master;


/**
 * An exception used to signify a test failure.  It contains the human-readable description of the test failure, potentially
 *  including an underlying cause.
 */
public class GalvanFailureException extends Exception {
  private static final long serialVersionUID = 1L;


  public GalvanFailureException(String message) {
    super(message);
  }

  public GalvanFailureException(String message, Throwable cause) {
    super(message, cause);
  }
}
