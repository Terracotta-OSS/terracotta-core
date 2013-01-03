/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.tx.ServerTransaction;

import java.util.Map;

public class ApplyTransactionContext implements MultiThreadedEventContext {

  private final ServerTransaction txn;
  private final Map<ObjectID, ManagedObject> objects;
  private final boolean needsApply;

  public ApplyTransactionContext(ServerTransaction txn, Map<ObjectID, ManagedObject> objects, boolean needsApply) {
    this.txn = txn;
    this.objects = objects;
    this.needsApply = needsApply;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    return objects;
  }

  public ServerTransaction getTxn() {
    return txn;
  }

  public boolean needsApply() {
    return needsApply;
  }

  @Override
  public Object getKey() {
    return txn.getTransactionID();
  }
}
