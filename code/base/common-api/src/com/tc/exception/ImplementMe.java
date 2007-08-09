/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class ImplementMe extends TCRuntimeException {

  private static final String PRETTY_TEXT = "You've attempted to use an unsupported feature in this Terracotta product. Please consult "
                                            + "the product documentation, or email support@terracottatech.com for assistance.";

  public ImplementMe() {
    this(PRETTY_TEXT);
  }

  public ImplementMe(String message) {
    super(message);
  }

  public ImplementMe(Throwable cause) {
    super(PRETTY_TEXT, cause);
  }

  public ImplementMe(String message, Throwable cause) {
    super(message, cause);
  }

}