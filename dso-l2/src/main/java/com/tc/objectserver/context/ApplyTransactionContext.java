/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TxnObjectGrouping;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ApplyTransactionContext implements MultiThreadedEventContext {

  private final TxnObjectGrouping grouping;
  private final ServerTransaction txn;
  private final boolean needsApply;
  private final Collection<ObjectID> ignoredObjects;

  public ApplyTransactionContext(ServerTransaction txn, TxnObjectGrouping grouping, boolean needsApply,
                                 Collection<ObjectID> ignoredObjects) {
    this.txn = txn;
    this.needsApply = needsApply;
    this.grouping = grouping;
    this.ignoredObjects = ignoredObjects;
  }

  public Map<ObjectID, ManagedObject> getObjects() {
    Set<ObjectID> oids = new ObjectIDSet(txn.getObjectIDs());
    oids.removeAll(ignoredObjects);
    return grouping.getObjects(oids);
  }

  public Set<ObjectID> allCheckedOutObjects() {
    Set<ObjectID> objects = new HashSet<ObjectID>();
    for (ManagedObject managedObject : grouping.getObjects()) {
      objects.add(managedObject.getID());
    }
    return objects;
  }

  public ServerTransaction getTxn() {
    return txn;
  }

  public boolean needsApply() {
    return needsApply;
  }

  public Collection<ObjectID> getIgnoredObjects() {
    return ignoredObjects;
  }

  @Override
  public Object getKey() {
    return grouping;
  }

}
