/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.groups.NodeID;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.util.concurrent.CopyOnWriteArrayMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MetaServerTransactionSequencerImpl implements ServerTransactionSequencer {

  private CopyOnWriteArrayMap txnSequencers = new CopyOnWriteArrayMap(new CopyOnWriteArrayMap.TypedArrayFactory() {
                                                 public Object[] createTypedArray(int size) {
                                                   return new ServerTransactionSequencer[size];
                                                 }
                                               });

  private int                 index;
  
  public MetaServerTransactionSequencerImpl() {
    System.err.println("USING  META SERVER");
  }

  public void addTransactionLookupContexts(Collection<TransactionLookupContext> txnLookupContexts) {
    LinkedHashMap<NodeID, List<TransactionLookupContext>> segregated = segregateIncommingTransaction(txnLookupContexts);
    for (Iterator i = segregated.entrySet().iterator(); i.hasNext();) {
      Map.Entry<NodeID, List<TransactionLookupContext>> e = (Entry<NodeID, List<TransactionLookupContext>>) i.next();
      ServerTransactionSequencer sequencer = getOrCreateTransactionSequencer(e.getKey());
      sequencer.addTransactionLookupContexts(e.getValue());
    }
  }

  private ServerTransactionSequencer getOrCreateTransactionSequencer(NodeID key) {
    synchronized (txnSequencers) {
      ServerTransactionSequencer sq = (ServerTransactionSequencer) txnSequencers.get(key);
      if (sq == null) {
        sq = new ServerTransactionSequencerImpl();
        txnSequencers.put(key, sq);
      }
      return sq;
    }
  }

  private LinkedHashMap<NodeID, List<TransactionLookupContext>> segregateIncommingTransaction(
                                                                                              Collection<TransactionLookupContext> txnLookupContexts) {
    LinkedHashMap<NodeID, List<TransactionLookupContext>> map = new LinkedHashMap<NodeID, List<TransactionLookupContext>>();
    for (Iterator i = txnLookupContexts.iterator(); i.hasNext();) {
      TransactionLookupContext transactionLookupContext = (TransactionLookupContext) i.next();
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

  public TransactionLookupContext getNextTxnLookupContextToProcess() {
    ServerTransactionSequencer[] _txnSequencers = (ServerTransactionSequencer[]) txnSequencers.valuesToArray();
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

  public void makePending(ServerTransaction txn) {
    ServerTransactionSequencer sq = (ServerTransactionSequencer) txnSequencers.get(txn.getSourceID());
    sq.makePending(txn);
  }

  public void makeUnpending(ServerTransaction txn) {
    ServerTransactionSequencer sq = (ServerTransactionSequencer) txnSequencers.get(txn.getSourceID());
    sq.makeUnpending(txn);
  }

  public String toString() {
    return "MetaServerTransactionSequencerImpl { txnSequencers : " + txnSequencers.size() + " index = " + index
           + " } ";
  }

  // Used in tests
  int getTxnSequencersCount() {
    return txnSequencers.size();
  }
}
