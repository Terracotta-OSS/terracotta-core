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

  private Collection txnIDs;
  private Collection objects;
  private Set        completedTxnIds;
  private Map        newRoots;
  private boolean    isInitialized = false;

  public CommitTransactionContext() {
    // Empty constructor
  }

  public void initialize(Collection appliedTxnIDs, Collection appliedObjects, Map newRootsInAppliedTxns,
                         Set completedTransactionIDs) {
    this.txnIDs = appliedTxnIDs;
    this.objects = appliedObjects;
    this.newRoots = newRootsInAppliedTxns;
    this.completedTxnIds = completedTransactionIDs;
    isInitialized = true;
  }

  public boolean isInitialized() {
    return isInitialized;
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
