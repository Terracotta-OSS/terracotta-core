/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;
import com.tc.objectserver.storage.api.PersistenceTransaction;

import java.util.Collection;
import java.util.Set;

public interface TransactionStore {

  public void commitTransactionDescriptor(PersistenceTransaction transaction, ServerTransactionID stxID);

  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID);

  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID serverTransactionID);

  public GlobalTransactionID getLeastGlobalTransactionID();

  /**
   * This method clears the server transaction ids less than the low water mark, for that particular node.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction transaction,
                                                         ServerTransactionID lowWaterMark);

  /**
   * This is used by the passive to clear completed Transaction ids.
   */
  public void clearCommitedTransactionsBelowLowWaterMark(PersistenceTransaction tx,
                                                         GlobalTransactionID lowGlobalTransactionIDWatermark);

  public void shutdownNode(PersistenceTransaction transaction, NodeID nid);

  public void shutdownAllClientsExcept(PersistenceTransaction tx, Set cids);

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID);

  public void commitAllTransactionDescriptor(PersistenceTransaction persistenceTransaction, Collection stxIDs);

}
