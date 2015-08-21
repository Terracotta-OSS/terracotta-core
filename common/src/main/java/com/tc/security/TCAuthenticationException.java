package com.tc.security;

import com.tc.exception.TCRuntimeException;

/**
 * @author Alex Snaps
 */
public class TCAuthenticationException extends TCRuntimeException {
  public TCAuthenticationException() {
  }

  public TCAuthenticationException(String message) {
    super(message);
  }

  public TCAuthenticationException(Throwable cause) {
    super(cause);
  }

  public TCAuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
