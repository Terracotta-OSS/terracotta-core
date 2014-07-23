/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.Transaction;
import com.tc.objectserver.api.TransactionStore;
import com.tc.objectserver.context.LowWaterMarkCallbackContext;
import com.tc.objectserver.event.ServerEventBuffer;
import com.tc.objectserver.persistence.PersistenceTransactionProvider;
import com.tc.util.SequenceValidator;
import com.tc.util.sequence.Sequence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class ServerGlobalTransactionManagerImpl implements ServerGlobalTransactionManager {
  private final TransactionStore                    transactionStore;
  private final SequenceValidator                   sequenceValidator;
  private final GlobalTransactionIDSequenceProvider gidSequenceProvider;
  private final Sequence                            globalTransactionIDSequence;
  private final SortedMap<GlobalTransactionID, List<Runnable>> lwmCallbacks = new TreeMap<GlobalTransactionID, List<Runnable>>();
  private final Sink                                callbackSink;
  private final PersistenceTransactionProvider      persistenceTransactionProvider;
  private final ServerEventBuffer                              serverEventBuffer;

  public ServerGlobalTransactionManagerImpl(SequenceValidator sequenceValidator, TransactionStore transactionStore,
                                            GlobalTransactionIDSequenceProvider gidSequenceProvider,
                                            Sequence globalTransactionIDSequence, Sink callbackSink,
                                            PersistenceTransactionProvider persistenceTransactionProvider,
                                            ServerEventBuffer serverEventBuffer) {
    this.sequenceValidator = sequenceValidator;
    this.transactionStore = transactionStore;
    this.gidSequenceProvider = gidSequenceProvider;
    this.globalTransactionIDSequence = globalTransactionIDSequence;
    this.callbackSink = callbackSink;
    this.persistenceTransactionProvider = persistenceTransactionProvider;
    this.serverEventBuffer = serverEventBuffer;
  }

  @Override
  public void shutdownNode(NodeID nodeID) {
    this.sequenceValidator.remove(nodeID);
    transactionStore.shutdownNode(nodeID);
    processCallbacks();
  }

  @Override
  public void shutdownAllClientsExcept(Set cids) {
    transactionStore.shutdownAllClientsExcept(cids);
    processCallbacks();
  }

  @Override
  public boolean initiateApply(ServerTransactionID stxID) {
    GlobalTransactionDescriptor gtx = this.transactionStore.getTransactionDescriptor(stxID);
    return gtx.initiateApply();
  }

  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(ServerTransactionID sid) {
    Transaction tx = persistenceTransactionProvider.newTransaction();
    Collection<GlobalTransactionDescriptor> removedGDs = transactionStore.clearCommitedTransactionsBelowLowWaterMark(sid);
    tx.commit();
    processCallbacks();
    clearEventBuffer(removedGDs);
  }

  @Override
  public void clearCommittedTransaction(final ServerTransactionID serverTransactionID) {
    // This never hits the persistor (disk), so don't bother starting a transaction or it'll slow everything down
    GlobalTransactionDescriptor descriptor = transactionStore.clearCommittedTransaction(serverTransactionID);
    processCallbacks();
    clearEventBuffer(descriptor == null ? Collections.<GlobalTransactionDescriptor>emptySet() :
        Collections.singleton(descriptor));
  }

  private void clearEventBuffer(Collection<GlobalTransactionDescriptor> removedGDs) {
    for (GlobalTransactionDescriptor gd : removedGDs) {
      serverEventBuffer.removeEventsForTransaction(gd.getGlobalTransactionID());
    }
  }

  @Override
  public void clearCommitedTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    Transaction tx = persistenceTransactionProvider.newTransaction();
    transactionStore.clearCommitedTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
    tx.commit();
    clearEventBufferBelowLowWaterMark(lowGlobalTransactionIDWatermark);
  }

  private void clearEventBufferBelowLowWaterMark(GlobalTransactionID lowWatermark) {
    serverEventBuffer.clearEventBufferBelowLowWaterMark(lowWatermark);
  }

  @Override
  public void commit(ServerTransactionID stxID) {
    transactionStore.commitTransactionDescriptor(stxID);
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return transactionStore.getLeastGlobalTransactionID();
  }

  @Override
  public GlobalTransactionID getOrCreateGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor gdesc = transactionStore.getOrCreateTransactionDescriptor(serverTransactionID);
    return gdesc.getGlobalTransactionID();
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID(ServerTransactionID serverTransactionID) {
    GlobalTransactionDescriptor gdesc = transactionStore.getTransactionDescriptor(serverTransactionID);
    return (gdesc != null ? gdesc.getGlobalTransactionID() : GlobalTransactionID.NULL_ID);

  }

  @Override
  public void createGlobalTransactionDescIfNeeded(ServerTransactionID stxnID, GlobalTransactionID globalTransactionID) {
    transactionStore.createGlobalTransactionDescIfNeeded(stxnID, globalTransactionID);
  }

  @Override
  public GlobalTransactionIDSequenceProvider getGlobalTransactionIDSequenceProvider() {
    return gidSequenceProvider;
  }

  @Override
  public Sequence getGlobalTransactionIDSequence() {
    return globalTransactionIDSequence;
  }

  private void processCallbacks() {
    GlobalTransactionID gid = getLowGlobalTransactionIDWatermark();
    List<Runnable> callbacks = new ArrayList<Runnable>();
    synchronized (lwmCallbacks) {
      if (lwmCallbacks.isEmpty()) { return; }

      Iterator<List<Runnable>> i;
      if (gid.isNull()) {
        // No global transactions left, just clear out all the callbacks right away
        i = lwmCallbacks.values().iterator();
      } else {
        // clear out only the callbacks < gid
        i = lwmCallbacks.headMap(gid).values().iterator();
      }
      while (i.hasNext()) {
        callbacks.addAll(i.next());
        i.remove();
      }
    }
    // We can allow the callbacks to finish asynchronously here since from this point on they no longer have anything to
    // do with the current state of the global transaction system.
    for (Runnable r : callbacks) {
      callbackSink.add(new LowWaterMarkCallbackContext(r));
    }
  }

  @Override
  public void registerCallbackOnLowWaterMarkReached(final Runnable callback) {
    if (getLowGlobalTransactionIDWatermark().isNull()) {
      // Just execute the callback right away if there are no live transactions in the system.
      callbackSink.add(new LowWaterMarkCallbackContext(callback));
      return;
    }
    GlobalTransactionID gid = new GlobalTransactionID(getGlobalTransactionIDSequence().current());
    synchronized (lwmCallbacks) {
      if (!lwmCallbacks.containsKey(gid)) {
        lwmCallbacks.put(gid, new ArrayList<Runnable>());
      }
      lwmCallbacks.get(gid).add(callback);
    }
  }

  @Override
  public void recordApplyResults(ServerTransactionID serverTransactionID, Map<LogicalChangeID, LogicalChangeResult> results) {
    GlobalTransactionDescriptor gdesc = transactionStore.getTransactionDescriptor(serverTransactionID);
    gdesc.recordLogicalChangeResults(results);
  }

  @Override
  public Map<LogicalChangeID, LogicalChangeResult> getApplyResults(ServerTransactionID stxnID) {
    GlobalTransactionDescriptor gdesc = transactionStore.getTransactionDescriptor(stxnID);
    return gdesc.getApplyResults();
  }

}
