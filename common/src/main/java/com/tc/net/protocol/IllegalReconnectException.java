/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
