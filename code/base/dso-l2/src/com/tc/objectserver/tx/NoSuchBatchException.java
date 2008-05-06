/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

public class NoSuchBatchException extends Exception {

  public NoSuchBatchException() {
    super();
  }

  public NoSuchBatchException(String message) {
    super(message);
  }

  public NoSuchBatchException(String message, Throwable cause) {
    super(message, cause);
  }

  public NoSuchBatchException(Throwable cause) {
    super(cause);
  }

}
