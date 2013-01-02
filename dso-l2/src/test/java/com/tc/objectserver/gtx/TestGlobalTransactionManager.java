/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.util.sequence.Sequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class TestGlobalTransactionManager implements ServerGlobalTransactionManager {

  private long                           idSequence   = 0;
  private final Set<ServerTransactionID> commitedSIDs = new HashSet<ServerTransactionID>();

  @Override
  public boolean initiateApply(ServerTransactionID stxID) {
    return !commitedSIDs.contains(stxID);
  }

  @Override
  public void commit(Transaction persistenceTransaction, ServerTransactionID stxID) {
    commitedSIDs.add(stxID);
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return GlobalTransactionID.NULL_ID;
  }

  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID sid) {
    return;
  }

  @Override
  public void shutdownNode(NodeID nodeID) {
    return;
  }

  @Override
  public void commitAll(Transaction persistenceTransaction, Collection stxIDs) {
    for (Iterator iter = stxIDs.iterator(); iter.hasNext();) {
      commit(persistenceTransaction, (ServerTransactionID) iter.next());
    }
  }

  public void clear() {
    commitedSIDs.clear();
  }

  @Override
  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    return new GlobalTransactionID(idSequence++);
  }

  @Override
  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    throw new ImplementMe();
  }

  @Override
  public void shutdownAllClientsExcept(Set cids) {
    return;
  }

  @Override
  public Sequence getGlobalTransactionIDSequence() {
    throw new ImplementMe();
  }

  @Override
  public GlobalTransactionIDSequenceProvider getGlobalTransactionIDSequenceProvider() {
    throw new ImplementMe();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID serverTransactionID) {
    throw new ImplementMe();
  }

  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    return;
  }

  @Override
  public void registerCallbackOnLowWaterMarkReached(Runnable callback) {
    //
  }
}