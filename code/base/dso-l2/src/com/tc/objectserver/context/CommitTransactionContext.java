/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class CommitTransactionContext implements EventContext {

  private final Collection txnIDs;
  private final Collection objects;
  private final Set        completedTxnIds;
  private final Map        newRoots;

  public CommitTransactionContext(Collection appliedTxnIDs, Collection appliedObjects, Map newRootsInAppliedTxns,
                                  Set completedTxnIds) {
    this.txnIDs = appliedTxnIDs;
    this.objects = appliedObjects;
    this.newRoots = newRootsInAppliedTxns;
    this.completedTxnIds = completedTxnIds;
  }

  public Set getCompletedTransactionIDs() {
    Assert.assertNotNull(completedTxnIds);
    return completedTxnIds;
  }

  public Collection getObjects() {
    return objects;
  }

  public Collection getAppliedServerTransactionIDs() {
    return txnIDs;
  }

  public Map getNewRoots() {
    return newRoots;
  }

}
