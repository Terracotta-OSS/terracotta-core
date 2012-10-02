/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.NodeID;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.util.concurrent.CopyOnWriteSequentialMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is supposed to reduce the processing when pending and blocked transactions reaches high thresholds, since
 * it accounts txns from each clients separately.
 * <p>
 * Today we assign GIDs when the server sees the transactions and these GIDs are broadcasts to passive too (in networked
 * A/P config). Now if we reorder txns across clients, since we maintain client level sequencer, we may end up applying
 * a txn with higher GID before a txn with lower GID which is a no-no because of client lookups and stuff.
 * <p>
 * We have to fix it so that GIDs are assigned just before applies and are somehow broadcasted to passive too and
 * passive has to apply the transactions in that order. Right now this class is purely experimental.
 */
public class MetaServerTransactionSequencerImpl implements ServerTransactionSequencer {

  private final CopyOnWriteSequentialMap<NodeID, ServerTransactionSequencer> txnSequencers = new CopyOnWriteSequentialMap<NodeID, ServerTransactionSequencer>(
                                                                                                                                                              new CopyOnWriteSequentialMap.TypedArrayFactory() {
                                                                                        @Override
                                                                                        public ServerTransactionSequencer[] createTypedArray(int size) {
                                                                                          return new ServerTransactionSequencer[size];
                                                                                        }
                                                                                      });

  private int                 index;
  private final ServerTransactionSequencerStats                              msStats       = new ServerTransactionSequencerStats() {
                                                                                             @Override
                                                                                             public int getBlockedObjectsCount() {
                                                                                               int blockedObjectCount = 0;
                                                                                               for (ServerTransactionSequencer element : txnSequencers
                                                                                                   .values()) {
                                                                                                 ServerTransactionSequencerStats stsi = element
                                                                                                     .getStats();
                                                                                                 blockedObjectCount += stsi
                                                                                                     .getBlockedObjectsCount();
                                                                                               }
                                                                                               return blockedObjectCount;
                                                                                             }

                                                                                             @Override
                                                                                             public int getBlockedTxnsCount() {
                                                                                               int blockedTxnCount = 0;
                                                                                               for (ServerTransactionSequencer element : txnSequencers
                                                                                                   .values()) {
                                                                                                 ServerTransactionSequencerStats stsi = element
                                                                                                     .getStats();
                                                                                                 blockedTxnCount += stsi
                                                                                                     .getBlockedTxnsCount();
                                                                                               }
                                                                                               return blockedTxnCount;
                                                                                             }

                                                                                             @Override
                                                                                             public int getPendingTxnsCount() {
                                                                                               int pendingTxnsCount = 0;
                                                                                               for (ServerTransactionSequencer element : txnSequencers
                                                                                                   .values()) {
                                                                                                 ServerTransactionSequencerStats stsi = element
                                                                                                     .getStats();
                                                                                                 pendingTxnsCount += stsi
                                                                                                     .getPendingTxnsCount();
                                                                                               }
                                                                                               return pendingTxnsCount;
                                                                                             }

                                                                                             @Override
                                                                                             public int getTxnsCount() {
                                                                                               int txnsCount = 0;
                                                                                               for (ServerTransactionSequencer element : txnSequencers
                                                                                                   .values()) {
                                                                                                 ServerTransactionSequencerStats stsi = element
                                                                                                     .getStats();
                                                                                                 txnsCount += stsi
                                                                                                     .getTxnsCount();
                                                                                               }
                                                                                               return txnsCount;
                                                                                             }
                                                                                           };

  @Override
  public void addTransactionLookupContexts(Collection<TransactionLookupContext> txnLookupContexts) {
    LinkedHashMap<NodeID, List<TransactionLookupContext>> segregated = segregateIncomingTransaction(txnLookupContexts);
    for (Map.Entry<NodeID, List<TransactionLookupContext>> e : segregated.entrySet()) {
      ServerTransactionSequencer sequencer = getOrCreateTransactionSequencer(e.getKey());
      sequencer.addTransactionLookupContexts(e.getValue());
    }
  }

  private ServerTransactionSequencer getOrCreateTransactionSequencer(NodeID key) {
    synchronized (txnSequencers) {
      ServerTransactionSequencer sq = txnSequencers.get(key);
      if (sq == null) {
        sq = new ServerTransactionSequencerImpl();
        txnSequencers.put(key, sq);
      }
      return sq;
    }
  }

  private LinkedHashMap<NodeID, List<TransactionLookupContext>> segregateIncomingTransaction(
                                                                                              Collection<TransactionLookupContext> txnLookupContexts) {
    LinkedHashMap<NodeID, List<TransactionLookupContext>> map = new LinkedHashMap<NodeID, List<TransactionLookupContext>>();
    for (TransactionLookupContext transactionLookupContext : txnLookupContexts) {
      NodeID n = transactionLookupContext.getSourceID();
      List<TransactionLookupContext> list = map.get(n);
      if (list == null) {
        list = new ArrayList<TransactionLookupContext>(txnLookupContexts.size());
        map.put(n, list);
      }
      list.add(transactionLookupContext);
    }
    return map;
  }

  @Override
  public TransactionLookupContext getNextTxnLookupContextToProcess() {
    ServerTransactionSequencer[] _txnSequencers = txnSequencers.valuesToArray();
    if (_txnSequencers.length == 0) return null;
    if (index >= _txnSequencers.length) index = 0;
    int end = index;
    do {
      TransactionLookupContext txnLookupContext = _txnSequencers[index].getNextTxnLookupContextToProcess();
      if (txnLookupContext != null) return txnLookupContext;
      index = ++index % _txnSequencers.length;
    } while (index != end);
    return null;
  }

  @Override
  public void makePending(ServerTransaction txn) {
    ServerTransactionSequencer sq = txnSequencers.get(txn.getSourceID());
    sq.makePending(txn);
  }

  @Override
  public void makeUnpending(ServerTransaction txn) {
    ServerTransactionSequencer sq = txnSequencers.get(txn.getSourceID());
    sq.makeUnpending(txn);
  }

  @Override
  public String toString() {
    return "MetaServerTransactionSequencerImpl { txnSequencers : " + txnSequencers.size() + " index = " + index + " } ";
  }

  // Used in tests
  int getTxnSequencersCount() {
    return txnSequencers.size();
  }

  @Override
  public ServerTransactionSequencerStats getStats() {
    return msStats;
  }
}