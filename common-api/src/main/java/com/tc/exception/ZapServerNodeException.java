/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
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
