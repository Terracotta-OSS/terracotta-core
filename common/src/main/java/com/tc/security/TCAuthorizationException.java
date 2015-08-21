package com.tc.security;

import com.tc.exception.TCRuntimeException;

/**
 * @author Ludovic Orban
 */
public class TCAuthorizationException extends TCRuntimeException {
  public TCAuthorizationException() {
  }

  public TCAuthorizationException(String message) {
    super(message);
  }

  public TCAuthorizationException(Throwable cause) {
    super(cause);
  }

  public TCAuthorizationException(String message, Throwable cause) {
    super(message, cause);
  }
}
