/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

public class IndexException extends Exception { 
  

  public IndexException() {
    super();
  }

  public IndexException(Throwable cause) {
    super(cause);
  }

  public IndexException(String message) {
    super(message);
  }

  public IndexException(String message, Throwable cause) {
    super(message, cause);
  }

}
