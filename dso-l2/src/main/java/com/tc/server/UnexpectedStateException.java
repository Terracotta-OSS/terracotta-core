package com.tc.server;

import org.terracotta.monitoring.PlatformStopException;

public class UnexpectedStateException extends PlatformStopException {
  public UnexpectedStateException(String message) {
    super(message);
  }

  public UnexpectedStateException(String message, Throwable cause) {
    super(message, cause);
  }
}
