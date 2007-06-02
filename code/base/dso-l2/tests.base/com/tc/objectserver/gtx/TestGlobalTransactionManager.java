/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.exception.ImplementMe;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.sequence.Sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class TestGlobalTransactionManager implements ServerGlobalTransactionManager {

  public final NoExceptionLinkedQueue completeTransactionsContexts = new NoExceptionLinkedQueue();
  private long                        idSequence                   = 0;
  private Set                         commitedSIDs                 = new HashSet();

  public boolean initiateApply(ServerTransactionID stxID) {
    return !commitedSIDs.contains(stxID);
  }

  public void commit(PersistenceTransaction persistenceTransaction, ServerTransactionID stxID) {
    commitedSIDs.add(stxID);
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return null;
  }

  public void completeTransactions(PersistenceTransaction persistenceTransaction, Collection collection) {
    completeTransactionsContexts.put(collection);
  }

  public void shutdownClient(ChannelID channelID) {
    return;
  }

  public void commitAll(PersistenceTransaction persistenceTransaction, Collection stxIDs) {
    for (Iterator iter = stxIDs.iterator(); iter.hasNext();) {
      commit(persistenceTransaction, (ServerTransactionID) iter.next());
    }
  }

  public void clear() {
    commitedSIDs.clear();
    while (!completeTransactionsContexts.isEmpty()) {
      completeTransactionsContexts.take();
    }
  }

  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    return new GlobalTransactionID(idSequence++);
  }

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    throw new ImplementMe();
  }

  public void shutdownAllClientsExcept(Set cids) {
    return;
  }

  public Sequence getGlobalTransactionIDSequence() {
    throw new ImplementMe();
  }

  public GlobalTransactionIDSequenceProvider getGlobalTransactionIDSequenceProvider() {
    throw new ImplementMe();
  }
}