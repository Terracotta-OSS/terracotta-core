/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.distrunner;

public class ArgException extends Exception {

  public ArgException() {
    super();
  }

  public ArgException(String message) {
    super(message);
  }

  public ArgException(String message, Throwable cause) {
    super(message, cause);
  }

  public ArgException(Throwable cause) {
    super(cause);
  }

}
