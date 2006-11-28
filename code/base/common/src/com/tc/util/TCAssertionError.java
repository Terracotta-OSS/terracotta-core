/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.util;

import com.tc.exception.TCRuntimeException;

public class TCAssertionError extends TCRuntimeException {

  public TCAssertionError() {
    super();
  }

  public TCAssertionError(String message) {
    super(message);
  }

  public TCAssertionError(Throwable cause) {
    super(cause);
  }

  public TCAssertionError(String message, Throwable cause) {
    super(message, cause);
  }

}