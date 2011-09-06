/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

public class L1ServerMapTransactionCompletionHandler extends AbstractEventHandler {
  @Override
  public void handleEvent(EventContext context) {
    L1ServerMapLocalStoreTransactionCompletionListener txnListener = (L1ServerMapLocalStoreTransactionCompletionListener) context;
    txnListener.postTransactionCallback();
  }
}
