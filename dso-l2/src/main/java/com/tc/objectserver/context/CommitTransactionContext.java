/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

import java.util.Collection;
import java.util.Map;

public class CommitTransactionContext implements EventContext {

  private Collection txnIDs;
  private Collection objects;
  private Map        newRoots;
  private boolean    isInitialized = false;

  public CommitTransactionContext() {
    // Empty constructor
  }

  public void initialize(Collection appliedTxnIDs, Collection appliedObjects, Map newRootsInAppliedTxns) {
    this.txnIDs = appliedTxnIDs;
    this.objects = appliedObjects;
    this.newRoots = newRootsInAppliedTxns;
    isInitialized = true;
  }

  public boolean isInitialized() {
    return isInitialized;
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
