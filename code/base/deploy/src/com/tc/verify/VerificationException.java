/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.verify;

import com.tc.exception.TCException;

/**
 * Thrown when verification fails.
 */
public class VerificationException extends TCException {

  public VerificationException() {
    super();
  }

  public VerificationException(String message) {
    super(message);
  }

  public VerificationException(Throwable cause) {
    super(cause);
  }

  public VerificationException(String message, Throwable cause) {
    super(message, cause);
  }

}
