/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.util.Assert;
import com.tc.util.SequenceValidator;

import java.util.Collection;
import java.util.Iterator;

public class ServerGlobalTransactionManagerImpl implements ServerGlobalTransactionManager {

  private final TransactionStore               transactionStore;
  private final PersistenceTransactionProvider persistenceTransactionProvider;
  private final SequenceValidator              sequenceValidator;

  public ServerGlobalTransactionManagerImpl(SequenceValidator sequenceValidator, TransactionStore transactionStore,
                                            PersistenceTransactionProvider ptxp) {
    super();
    this.sequenceValidator = sequenceValidator;
    this.transactionStore = transactionStore;
    this.persistenceTransactionProvider = ptxp;
  }

  public void shutdownClient(ChannelID channelID) {
    this.sequenceValidator.remove(channelID);
    PersistenceTransaction tx = this.persistenceTransactionProvider.newTransaction();
    transactionStore.shutdownClient(tx, channelID);
    tx.commit();
  }

  public boolean needsApply(ServerTransactionID stxID) {
    GlobalTransactionDescriptor gtx = this.transactionStore.getTransactionDescriptor(stxID);
    return (gtx == null);
  }

  public void completeTransactions(PersistenceTransaction tx, Collection collection) {
    if (collection.isEmpty()) return;
    transactionStore.removeAllByServerTransactionID(tx, collection);
  }

  public void commit(PersistenceTransaction persistenceTransaction, ServerTransactionID stxID) {
    GlobalTransactionDescriptor desc = transactionStore.getTransactionDescriptor(stxID);
    Assert.assertNotNull(desc);
    transactionStore.commitTransactionDescriptor(persistenceTransaction, desc);
  }

  public void commitAll(PersistenceTransaction persistenceTransaction, Collection stxIDs) {
    for (Iterator i = stxIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxID = (ServerTransactionID) i.next();
      commit(persistenceTransaction, stxID);
    }
  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return transactionStore.getLeastGlobalTransactionID();
  }

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID stxnID) {
    return transactionStore.getGlobalTransactionID(stxnID);
  }

  public GlobalTransactionID createGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor gdesc = transactionStore.createTransactionDescriptor(serverTransactionID);
    return gdesc.getGlobalTransactionID();
  }

}
