/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class keeps track of locally checked out objects for applys and maintain the objects to txnid mapping in the
 * server. It wraps calls going to object manager from lookup, apply, commit stages
 */
public class TransactionObjectManagerImpl implements TransactionObjectManager, PrettyPrintable {

  // TODO:: Move to TCProperties
  private static final int                     MAX_OBJECTS_TO_COMMIT = 500;

  private final ObjectManager                  objectManager;
  private final TransactionSequencer           sequencer;
  private final ServerGlobalTransactionManager gtxm;
  private final Sink                           lookupSink;

  private final Object                         completedTxnIdsLock   = new Object();
  private Set                                  completedTxnIDs       = new HashSet();

  /*
   * This map contains ObjectIDs to TxnObjectGrouping that contains these objects
   */
  private final Map                            checkedOutObjects     = new HashMap();
  private final Map                            applyPendingTxns      = new HashMap();
  private final LinkedHashMap                  commitPendingTxns     = new LinkedHashMap();

  private final Set                            pendingObjectRequest  = new HashSet();
  private final PendingList                    pendingTxnList        = new PendingList();

  public TransactionObjectManagerImpl(ObjectManager objectManager, TransactionSequencer sequencer,
                                      ServerGlobalTransactionManager gtxm, Sink lookupSink) {
    this.objectManager = objectManager;
    this.sequencer = sequencer;
    this.gtxm = gtxm;
    this.lookupSink = lookupSink;
    if (false) {
      System.err.println("Starting the dumper");
      Thread t = new Thread("TransactionObjectManager Dumper") {
        public void run() {
          ThreadUtil.reallySleep(60000);
          dump();
        }
      };
      t.start();
    }
  }

  // ProcessTransactionHandler Method
  public void addTransactions(ChannelID channelID, List txns, Collection completedTxnIds) {
    sequencer.addTransactions(txns);
    addCompletedTxnIds(completedTxnIds);
  }

  private void addCompletedTxnIds(Collection txnIds) {
    synchronized (completedTxnIdsLock) {
      completedTxnIDs.addAll(txnIds);
    }
  }

  private Set getCompletedTxnIds() {
    synchronized (completedTxnIdsLock) {
      Set toRet = completedTxnIDs;
      completedTxnIDs = new HashSet();
      return toRet;
    }
  }

  // LookupHandler Method
  public void lookupObjectsForTransactions(Sink applyChangesSink) {
    processPendingIfNecessary();
    ServerTransaction txn;
    while ((txn = sequencer.getNextTxnToProcess()) != null) {
      ServerTransactionID stxID = txn.getServerTransactionID();
      if (gtxm.needsApply(stxID)) {
        lookupObjectsForApplyAndAddToSink(new TxnLookupContext(txn, applyChangesSink));
      } else {
        // These txns are already applied, hence just sending it to the next stage.
        applyChangesSink.add(new ApplyTransactionContext(txn, Collections.EMPTY_MAP));
      }
    }
  }

  private void processPendingIfNecessary() {
    if (pendingTxnList.processPending()) {
      processPendingTransactions();
    }
  }

  public synchronized void lookupObjectsForApplyAndAddToSink(TxnLookupContext lookupContext) {
    ServerTransaction txn = lookupContext.getTransaction();
    Collection oids = txn.getObjectIDs();
    // log("lookupObjectsForApplyAndAddToSink(): START : " + txn.getServerTransactionID() + " : " + oids);
    Set newRequests = new HashSet();
    boolean makePending = false;
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      if (checkedOutObjects.containsKey(oid)) {
        // Object is already checked out
      } else if (pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else {
        newRequests.add(oid);
      }
    }
    // TODO:: make cache and stats right
    if (!newRequests.isEmpty()) {
      if (objectManager.lookupObjectsForCreateIfNecessary(txn.getChannelID(), newRequests, lookupContext)) {
        addLookedupObjects(lookupContext.getLookedUpObjectsAndClear());
      } else {
        // New request went pending in object manager
        // log("lookupObjectsForApplyAndAddToSink(): New Request went pending : " + newRequests);
        makePending = true;
        pendingObjectRequest.addAll(newRequests);
      }
    }
    if (makePending) {
      // log("lookupObjectsForApplyAndAddToSink(): Make Pending : " + txn.getServerTransactionID());
      makePending(lookupContext);
      pendingTxnList.add(lookupContext);
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      TxnObjectGrouping newGrouping = new TxnObjectGrouping(txnID, txn.getNewRoots());
      mergeTransactionGroupings(oids, newGrouping);
      applyPendingTxns.put(txnID, newGrouping);
      lookupContext.getSink().add(new ApplyTransactionContext(txn, newGrouping.getObjects()));
      makeUnpending(lookupContext);
      // log("lookupObjectsForApplyAndAddToSink(): Sucess: " + txn.getServerTransactionID());
    }
  }

  // private void log(String message) {
  // if (false) System.err.println(Thread.currentThread() + " :: " + message);
  // }

  private void mergeTransactionGroupings(Collection oids, TxnObjectGrouping newGrouping) {
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      TxnObjectGrouping oldGrouping = (TxnObjectGrouping) checkedOutObjects.get(oid);
      if (oldGrouping == null) {
        throw new AssertionError("Transaction Grouping for lookedup objects is Null !! " + oid);
      } else if (oldGrouping != newGrouping) {
        newGrouping.merge(oldGrouping);
        for (Iterator j = oldGrouping.getObjects().keySet().iterator(); j.hasNext();) {
          Object old = checkedOutObjects.put(j.next(), newGrouping);
          Assert.assertTrue(old == oldGrouping);
        }
        ServerTransactionID oldTxnId = oldGrouping.getServerTransactionID();
        if (commitPendingTxns.remove(oldTxnId) == null) {
          // This grouping is not in commitPending so it could be in apply pending
          for (Iterator j = oldGrouping.getApplyPendingTxns().iterator(); j.hasNext();) {
            oldTxnId = (ServerTransactionID) j.next();
            if (applyPendingTxns.containsKey(oldTxnId)) {
              applyPendingTxns.put(oldTxnId, newGrouping);
            }
          }
        }
      }
    }
  }

  private synchronized void addLookedupObjects(Map lookedupObjects) {
    if (lookedupObjects.isEmpty()) return;
    TxnObjectGrouping tg = new TxnObjectGrouping(lookedupObjects);
    for (Iterator i = lookedupObjects.keySet().iterator(); i.hasNext();) {
      Object oid = i.next();
      pendingObjectRequest.remove(oid);
      checkedOutObjects.put(oid, tg);
    }
  }

  private void makePending(TxnLookupContext context) {
    if (context.isNewRequest()) {
      ServerTransaction txn = context.getTransaction();
      context.makePending(txn.getChannelID(), txn.getObjectIDs());
      sequencer.makePending(txn);
    }
  }

  private void makeUnpending(TxnLookupContext context) {
    if (context.isPendingRequest()) {
      sequencer.processedPendingTxn(context.getTransaction());
      initiateLookup();
    }
  }

  private void initiateLookup() {
    lookupSink.add(new LookupEventContext()); // TODO:: Optimize unnecessary adds to the sink
  }

  private void initiateProcessPending() {
    pendingTxnList.setProcessPending(true);
    initiateLookup();
  }

  private synchronized void processPendingTransactions() {
    pendingTxnList.setProcessPending(false);
    List copy = pendingTxnList.copyAndDestroy();
    for (Iterator i = copy.iterator(); i.hasNext();) {
      TxnLookupContext tlc = (TxnLookupContext) i.next();
      addLookedupObjects(tlc.getLookedUpObjectsAndClear());
    }
    for (Iterator i = copy.iterator(); i.hasNext();) {
      TxnLookupContext tlc = (TxnLookupContext) i.next();
      lookupObjectsForApplyAndAddToSink(tlc);
    }
  }

  // ApplyTransaction stage method
  public synchronized boolean applyTransactionComplete(ServerTransactionID stxnID) {
    TxnObjectGrouping grouping = (TxnObjectGrouping) applyPendingTxns.remove(stxnID);
    Assert.assertNotNull(grouping);
    if (grouping.applyComplete(stxnID)) {
      // Since verifying against all txns is costly, only the prime one (the one that created this grouping) is verfied
      // against
      ServerTransactionID pTxnID = grouping.getServerTransactionID();
      Assert.assertNull(applyPendingTxns.get(pTxnID));
      Object old = commitPendingTxns.put(pTxnID, grouping);
      Assert.assertNull(old);
      return true;
    }
    return false;
  }

  // Commit Transaction stage method
  public synchronized void addTransactionsToCommit(CommitTransactionContext ctc) {
    int count = 0;
    for (Iterator i = commitPendingTxns.values().iterator(); i.hasNext();) {
      TxnObjectGrouping grouping = (TxnObjectGrouping) i.next();
      i.remove();
      Assert.assertTrue(grouping.getApplyPendingTxns().isEmpty());
      Map objects = grouping.getObjects();
      count += objects.size();
      ctc.addObjectsAndAppliedTxns(grouping.getTxnIDs(), objects.values(), grouping.getNewRoots());
      for (Iterator j = objects.keySet().iterator(); j.hasNext();) {
        Object old = checkedOutObjects.remove(j.next());
        Assert.assertTrue(old == grouping);
      }
      if (count >= MAX_OBJECTS_TO_COMMIT) {
        break;
      }
    }
    if (count > 0) {
      ctc.setCompletedTransactionIds(getCompletedTxnIds());
    }
  }

  public void dump() {
    PrintWriter pw = new PrintWriter(System.err);
    new PrettyPrinter(pw).visit(this);
    pw.flush();
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().print("checkedOutObjects: ").println(checkedOutObjects);
    out.indent().print("applyPendingTxns: ").visit(applyPendingTxns).println();
    out.indent().print("commitPendingTxns: ").visit(commitPendingTxns).println();
    out.indent().println("pendingTxnList: " + pendingTxnList);
    out.indent().print("pendingObjectRequest: ").visit(pendingObjectRequest).println();
    return out;
  }

  private class TxnLookupContext implements ObjectManagerResultsContext {

    private final ServerTransaction txn;
    private boolean                 pending = false;
    private boolean                 isNew   = true;
    private final Sink              applyChangesSink;
    private Map                     lookedUpObject;

    public TxnLookupContext(ServerTransaction txn, Sink applyChangesSink) {
      this.txn = txn;
      this.applyChangesSink = applyChangesSink;
    }

    public Sink getSink() {
      return applyChangesSink;
    }

    public ServerTransaction getTransaction() {
      return txn;
    }

    // TODO:: Remove this
    public Set getCheckedOutObjectIDs() {
      return Collections.EMPTY_SET;
    }

    public synchronized boolean isPendingRequest() {
      return pending;
    }

    public synchronized boolean isNewRequest() {
      return isNew;
    }

    // Make pending could be called more than once !
    public synchronized void makePending(ChannelID channelID, Collection ids) {
      isNew = false;
      pending = true;
    }

    public synchronized void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
      if (lookedUpObject == null) {
        lookedUpObject = results.getObjects();
      } else {
        lookedUpObject.putAll(results.getObjects());
      }
      if (pending) {
        pending = false;
        TransactionObjectManagerImpl.this.initiateProcessPending();
      }
    }

    public synchronized Map getLookedUpObjectsAndClear() {
      if (lookedUpObject == null) {
        return Collections.EMPTY_MAP;
      } else {
        Map map = lookedUpObject;
        lookedUpObject = null;
        return map;
      }
    }

    public String toString() {
      return "TxnLookupContext [ " + txn + ", pending = " + pending + ", lookedupObjects = " + lookedUpObject.keySet()
             + "]";
    }
  }

  private static final class PendingList {
    LinkedList      pending = new LinkedList();
    private boolean processPending;

    public void add(TxnLookupContext lookupContext) {
      pending.add(lookupContext);
    }

    public List copyAndDestroy() {
      LinkedList copy = pending;
      pending = new LinkedList();
      return copy;
    }

    public Iterator iterator() {
      return pending.iterator();
    }

    public synchronized void setProcessPending(boolean b) {
      this.processPending = b;
    }

    public synchronized boolean processPending() {
      return this.processPending;
    }

    public String toString() {
      return "PendingList : processPending = " + processPending + " pending = " + pending;
    }

  }
}
