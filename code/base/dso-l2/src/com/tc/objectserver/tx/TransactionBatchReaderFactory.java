/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;

import java.io.IOException;

public interface TransactionBatchReaderFactory {
  public TransactionBatchReader newTransactionBatchReader(EventContext ctxt) throws IOException;
}
