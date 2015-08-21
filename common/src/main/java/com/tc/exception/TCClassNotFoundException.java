/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

/**
 * Terracotta equivalent of ClassNotFoundException - class is not available on this VM.
 */
public class TCClassNotFoundException extends TCRuntimeException {

  public TCClassNotFoundException() {
    super();
  }

  public TCClassNotFoundException(String message) {
    super(message);
  }

  public TCClassNotFoundException(Throwable cause) {
    super(cause);
  }

  public TCClassNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }

}
