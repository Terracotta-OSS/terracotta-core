package com.tc.security;

import com.tc.exception.TCRuntimeException;

/**
 * @author Ludovic Orban
 */
public class TCAuthorizationException extends TCRuntimeException {
  public TCAuthorizationException() {
  }

  public TCAuthorizationException(final String message) {
    super(message);
  }

  public TCAuthorizationException(final Throwable cause) {
    super(cause);
  }

  public TCAuthorizationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
