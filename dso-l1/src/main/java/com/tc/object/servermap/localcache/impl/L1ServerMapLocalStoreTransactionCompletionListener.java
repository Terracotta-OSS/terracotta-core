/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * To be used only when a transaction is completed.
 */
public class L1ServerMapLocalStoreTransactionCompletionListener implements TransactionCompleteListener, EventContext {
  private static final AtomicInteger         listenerCount = new AtomicInteger();
  private static final TCLogger              logger        = TCLogging
                                                               .getLogger(L1ServerMapLocalStoreTransactionCompletionListener.class);
  private final ServerMapLocalCache          serverMapLocalCache;
  private final Object                       key;
  private final TransactionCompleteOperation transactionCompleteOperation;
  private final AbstractLocalCacheStoreValue value;

  public L1ServerMapLocalStoreTransactionCompletionListener(ServerMapLocalCache serverMapLocalCache, Object key,
                                                            AbstractLocalCacheStoreValue value,
                                                            TransactionCompleteOperation onCompleteOperation) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.key = key;
    this.transactionCompleteOperation = onCompleteOperation;
    this.value = value;
    if (listenerCount.incrementAndGet() % 50 == 0 && logger.isDebugEnabled()) {
      logger.debug("Number of active server map transation completion listeners: " + listenerCount.get());
    }
  }

  public void transactionComplete(TransactionID txnID) {
    if (transactionCompleteOperation == TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY) {
      postTransactionCallback();
    } else {
      // add it to sink
      serverMapLocalCache.transactionComplete(this);
    }
  }

  public void postTransactionCallback() {
    serverMapLocalCache
        .postTransactionCallback(key, value,
                                 transactionCompleteOperation == TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY);
    listenerCount.decrementAndGet();
  }

  public static enum TransactionCompleteOperation {
    UNPIN_ENTRY, UNPIN_AND_REMOVE_ENTRY;
  }
}
