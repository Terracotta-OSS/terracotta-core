/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.gtx;

public class ApplyNotStartedError extends Error {

  public ApplyNotStartedError() {
    super();
  }

  public ApplyNotStartedError(String message) {
    super(message);
  }

  public ApplyNotStartedError(String message, Throwable cause) {
    super(message, cause);
  }

  public ApplyNotStartedError(Throwable cause) {
    super(cause);
  }

}
