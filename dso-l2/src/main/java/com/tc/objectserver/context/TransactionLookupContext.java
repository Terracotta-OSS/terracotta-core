/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.NodeID;
import com.tc.objectserver.tx.ServerTransaction;

public class TransactionLookupContext implements EventContext {

  private final ServerTransaction txn;
  private final boolean           initiateApply;

  public TransactionLookupContext(ServerTransaction txn, boolean initiateApply) {
    this.txn = txn;
    this.initiateApply = initiateApply;
  }

  public String toString() {
    return "TransactionLookupContext [ " + txn + " initiateApply = " + initiateApply + " ]";
  }

  public ServerTransaction getTransaction() {
    return txn;
  }

  public boolean initiateApply() {
    return initiateApply;
  }

  public NodeID getSourceID() {
    return txn.getSourceID();
  }

}
