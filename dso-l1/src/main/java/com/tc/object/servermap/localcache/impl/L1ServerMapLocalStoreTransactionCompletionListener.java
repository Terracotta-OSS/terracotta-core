/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.EventContext;
import com.tc.object.TCObjectSelf;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.object.tx.TransactionID;

/**
 * To be used only when a transaction is completed.
 */
public class L1ServerMapLocalStoreTransactionCompletionListener implements TransactionCompleteListener, EventContext {
  private final ServerMapLocalCache          serverMapLocalCache;
  private final Object                       key;
  private final TransactionCompleteOperation transactionCompleteOperation;
  private final AbstractLocalCacheStoreValue value;
  private final Object                       actualValue;

  public L1ServerMapLocalStoreTransactionCompletionListener(ServerMapLocalCache serverMapLocalCache, Object key,
                                                            Object actualValue, AbstractLocalCacheStoreValue value,
                                                            TransactionCompleteOperation onCompleteOperation) {
    this.serverMapLocalCache = serverMapLocalCache;
    this.key = key;
    this.actualValue = actualValue;
    this.transactionCompleteOperation = onCompleteOperation;
    this.value = value;
    if (actualValue instanceof TCObjectSelf) {
      ((TCObjectSelf) actualValue).touch();
    }
  }

  public void transactionComplete(TransactionID txnID) {
    serverMapLocalCache.addToSink(this);
  }

  public void postTransactionCallback() {
    serverMapLocalCache.transactionComplete(key, value, this);
    serverMapLocalCache.unpinEntry(key, value);
    if (actualValue instanceof TCObjectSelf) {
      ((TCObjectSelf) actualValue).untouch();
    }
    if (transactionCompleteOperation == TransactionCompleteOperation.UNPIN_AND_REMOVE_ENTRY) {
      serverMapLocalCache.removeFromLocalCache(key, value);
    }
  }

  public static enum TransactionCompleteOperation {
    UNPIN_ENTRY, UNPIN_AND_REMOVE_ENTRY;
  }
}
