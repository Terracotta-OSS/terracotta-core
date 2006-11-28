/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
