/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * RMP-309 : SplitBrain and other Zap node requests
 */

public class ZapServerNodeException extends TCServerRestartException {
  public ZapServerNodeException(String message) {
    super(message);
  }
}
