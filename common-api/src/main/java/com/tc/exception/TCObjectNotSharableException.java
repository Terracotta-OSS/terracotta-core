/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

public class TCObjectNotSharableException extends TCRuntimeException {

  public TCObjectNotSharableException() {
    super();
  }

  public TCObjectNotSharableException(String message) {
    super(wrap(message));
  }

  private static String wrap(String message) {
    return "\n**************************************************************************************\n" + message
           + "\n**************************************************************************************\n";
  }

  public TCObjectNotSharableException(Throwable cause) {
    super(cause);
  }

  public TCObjectNotSharableException(String message, Throwable cause) {
    super(wrap(message), cause);
  }

}
