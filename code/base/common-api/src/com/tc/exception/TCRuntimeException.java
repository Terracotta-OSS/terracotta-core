/**
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.exception;

/**
 * The base class for all runtime (non-checked) Terracotta exceptions. Normal production code should never catch this.
 */
public class TCRuntimeException extends RuntimeException {

  public TCRuntimeException() {
    super();
  }

  public TCRuntimeException(String message) {
    super(message);
  }

  public TCRuntimeException(Throwable cause) {
    super(cause);
  }

  public TCRuntimeException(String message, Throwable cause) {
    super(message, cause);
  }

}