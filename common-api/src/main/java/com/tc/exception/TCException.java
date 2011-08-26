/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;


/**
 * The base class for all standard, checked Terracotta exceptions.
 */
public class TCException extends Exception {

  public TCException() {
    super();
  }

  public TCException(String message) {
    super(message);
  }

  public TCException(Throwable cause) {
    super(cause);
  }

  public TCException(String message, Throwable cause) {
    super(message, cause);
  }

}