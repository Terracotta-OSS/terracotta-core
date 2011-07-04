/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
