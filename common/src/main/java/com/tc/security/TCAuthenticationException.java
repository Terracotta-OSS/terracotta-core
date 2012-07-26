package com.tc.security;

import com.tc.exception.TCRuntimeException;

/**
 * @author Alex Snaps
 */
public class TCAuthenticationException extends TCRuntimeException {
  public TCAuthenticationException() {
  }

  public TCAuthenticationException(final String message) {
    super(message);
  }

  public TCAuthenticationException(final Throwable cause) {
    super(cause);
  }

  public TCAuthenticationException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
