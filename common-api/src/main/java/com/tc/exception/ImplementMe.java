/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class ImplementMe extends TCRuntimeException {

  private static final String PRETTY_TEXT = "You've attempted to use an unsupported feature in this Terracotta product. Please consult "
                                            + "the product documentation, or email support@terracottatech.com for assistance.";

  /**
   * Construct new with default text
   */
  public ImplementMe() {
    this(PRETTY_TEXT);
  }

  /**
   * Construct with specified text
   * @param message The message
   */
  public ImplementMe(String message) {
    super(message);
  }

  /**
   * Construct with exception and use default text
   * @param cause The cause
   */
  public ImplementMe(Throwable cause) {
    super(PRETTY_TEXT, cause);
  }

  /**
   * Construct with specified message and cause
   * @param message Specified message
   * @param cause Cause
   */
  public ImplementMe(String message, Throwable cause) {
    super(message, cause);
  }

}
