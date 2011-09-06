/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.gtx;

public class UnknownTransactionError extends Error {

  public UnknownTransactionError() {
    super();
  }

  public UnknownTransactionError(String message) {
    super(message);
  }

  public UnknownTransactionError(String message, Throwable cause) {
    super(message, cause);
  }

  public UnknownTransactionError(Throwable cause) {
    super(cause);
  }

}
