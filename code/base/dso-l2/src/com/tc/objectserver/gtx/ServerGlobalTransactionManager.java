/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.Collection;

public interface ServerGlobalTransactionManager extends GlobalTransactionManager {

  /**
   * Returns true if the specified transaction hasn't been applied yet. This method is partially redundant with
   * startApply. It should be removed once the dispatching logic is factored out of ProcessTransactionHandler and
   * ApplyTransactionHandler
   */
  public boolean needsApply(ServerTransactionID stxID);

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

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID stxnID);

  public GlobalTransactionID createGlobalTransactionID(ServerTransactionID serverTransactionID);

}
