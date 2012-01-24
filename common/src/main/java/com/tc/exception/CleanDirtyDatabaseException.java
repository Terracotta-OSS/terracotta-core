/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.exception;

/**
 * RMP-309
 */

public class CleanDirtyDatabaseException extends TCServerRestartException {
  public CleanDirtyDatabaseException(String message) {
    super(message);
  }
}
