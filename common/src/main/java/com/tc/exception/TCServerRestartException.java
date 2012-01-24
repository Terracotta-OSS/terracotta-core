/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * RMP-309
 */

public class TCServerRestartException extends TCRuntimeException {
  public TCServerRestartException(String message) {
    super(message);
  }
}
