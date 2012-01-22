/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCRuntimeException;

public class TCDataFileLockingException extends TCRuntimeException {

  public TCDataFileLockingException() {
    super();
  }

  public TCDataFileLockingException(String message) {
    super(message);
  }

  public TCDataFileLockingException(Throwable cause) {
    super(cause);
  }

  public TCDataFileLockingException(String message, Throwable cause) {
    super(message, cause);
  }

}
