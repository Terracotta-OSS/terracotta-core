/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.exception;

/**
 * RMP-309
 */

public class ZapDirtyDbServerNodeException extends TCServerRestartException {
  public ZapDirtyDbServerNodeException(String message) {
    super(message);
  }
}