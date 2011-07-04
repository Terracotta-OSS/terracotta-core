/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.api;

public class ShutdownError extends Error {

  public ShutdownError() {
    super();
  }

  public ShutdownError(String message) {
    super(message);
  }

  public ShutdownError(String message, Throwable cause) {
    super(message, cause);
  }

  public ShutdownError(Throwable cause) {
    super(cause);
  }

}
