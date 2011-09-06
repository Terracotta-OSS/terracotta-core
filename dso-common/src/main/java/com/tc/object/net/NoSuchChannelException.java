/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
