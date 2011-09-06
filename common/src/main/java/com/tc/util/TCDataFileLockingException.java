/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
