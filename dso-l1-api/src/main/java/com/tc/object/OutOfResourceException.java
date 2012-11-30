package com.tc.object;

/**
 * @author tim
 */
public class OutOfResourceException extends RuntimeException {
  public OutOfResourceException() {
  }

  public OutOfResourceException(final String message) {
    super(message);
  }

  public OutOfResourceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  public OutOfResourceException(final Throwable cause) {
    super(cause);
  }
}
