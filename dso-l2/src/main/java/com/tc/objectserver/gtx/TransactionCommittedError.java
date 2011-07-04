/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.gtx;

public class TransactionCommittedError extends Error {

  public TransactionCommittedError() {
    super();
  }

  public TransactionCommittedError(String message) {
    super(message);
  }

  public TransactionCommittedError(String message, Throwable cause) {
    super(message, cause);
  }

  public TransactionCommittedError(Throwable cause) {
    super(cause);
  }

}
