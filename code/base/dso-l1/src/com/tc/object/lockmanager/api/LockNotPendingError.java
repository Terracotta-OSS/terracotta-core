/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.lockmanager.api;

public class LockNotPendingError extends Error {

  public LockNotPendingError() {
    super();
  }

  public LockNotPendingError(String message) {
    super(message);
  }

  public LockNotPendingError(String message, Throwable cause) {
    super(message, cause);
  }

  public LockNotPendingError(Throwable cause) {
    super(cause);
  }

}
