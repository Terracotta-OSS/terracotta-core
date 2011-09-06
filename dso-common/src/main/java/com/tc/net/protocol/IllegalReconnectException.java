/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

public class IllegalReconnectException extends Exception {

  public IllegalReconnectException() {
    super();
  }

  public IllegalReconnectException(String message, Throwable cause) {
    super(message, cause);
  }

  public IllegalReconnectException(String message) {
    super(message);
  }

  public IllegalReconnectException(Throwable cause) {
    super(cause);
  }

}
