/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

public class BatchDefinedException extends Exception {

  public BatchDefinedException() {
    super();
  }

  public BatchDefinedException(String message) {
    super(message);
  }

  public BatchDefinedException(String message, Throwable cause) {
    super(message, cause);
  }

  public BatchDefinedException(Throwable cause) {
    super(cause);
  }

}
