/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
