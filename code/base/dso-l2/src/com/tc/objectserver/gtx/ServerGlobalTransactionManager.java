/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionIDGenerator;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.Collection;
import java.util.Set;

public interface ServerGlobalTransactionManager extends GlobalTransactionIDGenerator {

  /**
   * Changes state to APPLY_INITIATED and returns true if the specified transaction hasn't been initiated apply. If not
   * returns false.
   */
  public boolean initiateApply(ServerTransactionID stxID);

  /**
   * Commits the state of the transaciton.
   */
  public void commit(PersistenceTransaction persistenceTransaction, ServerTransactionID stxID);

  /**
   * Commits all the state of the all the transacitons.
   */
  public void commitAll(PersistenceTransaction persistenceTransaction, Collection gtxIDs);

  /**
   * Notifies the transaction manager that the ServerTransactionIDs in the given collection are no longer active (i.e.,
   * it will never be referenced again). The transaction manager is free to release resources dedicated those
   * transactions.
   */
  public void completeTransactions(PersistenceTransaction tx, Collection collection);

  public void shutdownClient(ChannelID channelID);

  public void shutdownAllClientsExcept(Set cids);

  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID);

}
