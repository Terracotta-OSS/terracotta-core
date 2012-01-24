/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

public class TCClassNotAdaptableException extends TCRuntimeException {

  public TCClassNotAdaptableException() {
    super();
  }

  public TCClassNotAdaptableException(String message) {
    super(wrap(message));
  }

  private static String wrap(String message) {
    return "\n**************************************************************************************\n" + message
           + "\n**************************************************************************************\n";
  }

  public TCClassNotAdaptableException(Throwable cause) {
    super(cause);
  }

  public TCClassNotAdaptableException(String message, Throwable cause) {
    super(wrap(message), cause);
  }

}
