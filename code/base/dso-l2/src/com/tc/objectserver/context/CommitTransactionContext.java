/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CommitTransactionContext implements EventContext {

  private Set txnIDs = new HashSet();
  private Collection objects = new ArrayList();
  private Set completedTxnIds;
  private Map newRoots = new HashMap();

  public void addObjectsAndAppliedTxns(Collection appliedTxnIDs, Collection appliedObject, Map newRootsInAppliedTxns) {
    this.txnIDs.addAll(appliedTxnIDs);
    this.objects.addAll(appliedObject);
    this.newRoots.putAll(newRootsInAppliedTxns);
  }

  public void setCompletedTransactionIds(Set completedTxnIds) {
    this.completedTxnIds = completedTxnIds;
  }

  public Set getCompletedTransactionIDs() {
    Assert.assertNotNull(completedTxnIds);
    return completedTxnIds;
  }

  public Collection getObjects() {
    return objects;
  }

  public Set getAppliedServerTransactionIDs() {
    return txnIDs;
  }
  
  public Map getNewRoots() {
    return newRoots;
  }

}
