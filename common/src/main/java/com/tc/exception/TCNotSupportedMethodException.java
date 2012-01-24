/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * Thrown when someone tries to call an unimplemented feature.
 */
public class TCNotSupportedMethodException extends TCRuntimeException {
  public final static String  CLASS_SLASH = "com/tc/exception/TCNotSupportedMethodException";
  
  private static final ExceptionWrapper wrapper = new ExceptionWrapperImpl();

  private static final String PRETTY_TEXT = "You have attempted to invoke an unsupported API in this Terracotta product. \n"
                                            + "Please consult the product documentation, or email support@terracottatech.com for assistance.";

  public TCNotSupportedMethodException() {
    this(PRETTY_TEXT);
  }

  public TCNotSupportedMethodException(String message) {
    super(wrapper.wrap(message));
  }

  public TCNotSupportedMethodException(Throwable cause) {
    this(PRETTY_TEXT, cause);
  }

  public TCNotSupportedMethodException(String message, Throwable cause) {
    super(wrapper.wrap(message), cause);
  }

}
