/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.EventContext;

import java.io.IOException;

public interface TransactionBatchReaderFactory {
  public TransactionBatchReader newTransactionBatchReader(EventContext ctxt) throws IOException;
}
