/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Collections;
import java.util.Map;

public class ApplyTransactionContext implements EventContext {

  private final ServerTransaction txn;
  private final Map objects;
  private final boolean needsApply;

  public ApplyTransactionContext(ServerTransaction txn, Map objects) {
    this.txn = txn;
    this.objects = objects;
    this.needsApply = true;
  }
  
  public ApplyTransactionContext(ServerTransaction txn) {
    this.txn = txn;
    this.objects = Collections.EMPTY_MAP;
    this.needsApply = false;
  }

  public Map getObjects() {
    return objects;
  }

  public ServerTransaction getTxn() {
    return txn;
  }

  public boolean needsApply() {
    return needsApply;
  }

}
