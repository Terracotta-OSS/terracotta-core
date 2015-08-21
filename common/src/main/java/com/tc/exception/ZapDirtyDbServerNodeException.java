/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * RMP-309 : Zap Node which joined with dirty db 
 */

public class ZapDirtyDbServerNodeException extends TCServerRestartException {
  public ZapDirtyDbServerNodeException(String message) {
    super(message);
  }
}
