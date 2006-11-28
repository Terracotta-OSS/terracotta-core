/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
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
