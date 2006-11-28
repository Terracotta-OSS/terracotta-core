/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
