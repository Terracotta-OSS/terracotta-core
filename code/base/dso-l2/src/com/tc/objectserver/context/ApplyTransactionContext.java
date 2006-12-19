/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Map;

public class ApplyTransactionContext implements EventContext {

  private final ServerTransaction txn;
  private final Map objects;

  public ApplyTransactionContext(ServerTransaction txn, Map objects) {
    this.txn = txn;
    this.objects = objects;
  }

  public Map getObjects() {
    return objects;
  }

  public ServerTransaction getTxn() {
    return txn;
  }

}
