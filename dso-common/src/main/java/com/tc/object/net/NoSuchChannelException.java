/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.net;

public class NoSuchChannelException extends Exception {

  public NoSuchChannelException() {
    super();
  }

  public NoSuchChannelException(String message) {
    super(message);
  }

  public NoSuchChannelException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoSuchChannelException(Throwable cause) {
    super(cause);
  }

}
