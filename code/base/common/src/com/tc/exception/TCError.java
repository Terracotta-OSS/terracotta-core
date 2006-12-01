/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;


/**
 * The base class for all Terracotta errors.
 */
public class TCError extends Error {

  public TCError() {
    super();
  }

  public TCError(String message) {
    super(message);
  }

  public TCError(Throwable cause) {
    super(cause);
  }

  public TCError(String message, Throwable cause) {
    super(message, cause);
  }

}