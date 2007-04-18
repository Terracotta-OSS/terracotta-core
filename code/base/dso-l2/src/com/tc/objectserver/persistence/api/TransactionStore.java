/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.api;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.GlobalTransactionDescriptor;

import java.util.Collection;
import java.util.Set;

public interface TransactionStore {

  public void commitTransactionDescriptor(PersistenceTransaction transaction, GlobalTransactionDescriptor txID);
  
  public GlobalTransactionDescriptor getTransactionDescriptor(ServerTransactionID serverTransactionID);
  
  public GlobalTransactionDescriptor getOrCreateTransactionDescriptor(ServerTransactionID serverTransactionID);

  public GlobalTransactionID getLeastGlobalTransactionID();
  
  /**
   * Deletes all entries whose ServerTransactionIDs are in the collections
   */
  public void removeAllByServerTransactionID(PersistenceTransaction transaction, Collection collection);

  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID stxnID);
  
  public void shutdownClient(PersistenceTransaction transaction, ChannelID client);

  public void shutdownAllClientsExcept(PersistenceTransaction tx, Set cids);
  
  public void createGlobalTransactionDesc(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID);
  
}
