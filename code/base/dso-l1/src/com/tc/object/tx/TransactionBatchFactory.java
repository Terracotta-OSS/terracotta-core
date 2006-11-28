/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.tx;

public interface TransactionBatchFactory {
  public ClientTransactionBatch nextBatch();
}
