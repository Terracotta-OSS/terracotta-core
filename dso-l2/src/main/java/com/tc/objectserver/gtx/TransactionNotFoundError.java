/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.gtx;

public class TransactionNotFoundError extends Error {

  public TransactionNotFoundError() {
    super();
  }

  public TransactionNotFoundError(String message) {
    super(message);
  }

  public TransactionNotFoundError(String message, Throwable cause) {
    super(message, cause);
  }

  public TransactionNotFoundError(Throwable cause) {
    super(cause);
  }

}
