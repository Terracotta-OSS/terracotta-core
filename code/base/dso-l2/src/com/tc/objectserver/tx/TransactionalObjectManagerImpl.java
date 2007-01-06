/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;

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
import java.util.ArrayList;
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
public class TransactionalObjectManagerImpl implements TransactionalObjectManager, PrettyPrintable {

  private final ObjectManager                  objectManager;
  private final TransactionSequencer           sequencer;
  private final ServerGlobalTransactionManager gtxm;
  private final Sink                           lookupSink;
  private final Sink                           applySink;
  private final Sink                           commitSink;

  private final Object                         completedTxnIdsLock     = new Object();
  private Set                                  completedTxnIDs         = new HashSet();

  private final List                           processingCommits       = new LinkedList();

  /*
   * This map contains ObjectIDs to TxnObjectGrouping that contains these objects
   */
  private final Map                            checkedOutObjects       = new HashMap();
  private final Map                            applyPendingTxns        = new HashMap();
  private final LinkedHashMap                  commitPendingTxns       = new LinkedHashMap();

  private final Set                            pendingObjectRequest    = new HashSet();
  private final PendingList                    pendingTxnList          = new PendingList();
  private final LinkedQueue                    processedPendingLookups = new LinkedQueue();
  private final LinkedQueue                    processedApplys         = new LinkedQueue();

  private final int                            commitThreadsCount;

  public TransactionalObjectManagerImpl(ObjectManager objectManager, TransactionSequencer sequencer,
                                        ServerGlobalTransactionManager gtxm, Sink lookupSink, Sink applySink,
                                        Sink commitSink, int commitThreadsCount) {
    this.objectManager = objectManager;
    this.sequencer = sequencer;
    this.gtxm = gtxm;
    this.lookupSink = lookupSink;
    this.applySink = applySink;
    this.commitSink = commitSink;
    this.commitThreadsCount = commitThreadsCount;
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
  public void lookupObjectsForTransactions() {
    processPendingIfNecessary();
    while (true) {
      processApplyComplete();
      initiateCommitTransactions();
      ServerTransaction txn = sequencer.getNextTxnToProcess();
      if (txn == null) break;
      ServerTransactionID stxID = txn.getServerTransactionID();
      if (gtxm.needsApply(stxID)) {
        lookupObjectsForApplyAndAddToSink(txn);
      } else {
        // These txns are already applied, hence just sending it to the next stage.
        applySink.add(new ApplyTransactionContext(txn, Collections.EMPTY_MAP));
      }
    }
    // log(shortDescription());
  }

  private void processPendingIfNecessary() {
    if (addProcessedPendingLookups()) {
      processPendingTransactions();
    }
  }

  public synchronized void lookupObjectsForApplyAndAddToSink(ServerTransaction txn) {
    Collection oids = txn.getObjectIDs();
    // log("lookupObjectsForApplyAndAddToSink(): START : " + txn.getServerTransactionID() + " : " + oids);
    Set newRequests = new HashSet();
    boolean makePending = false;
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      TxnObjectGrouping tog;
      if (pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else if ((tog = (TxnObjectGrouping) checkedOutObjects.get(oid)) == null) {
        // 1) Object is not already checked out or
        newRequests.add(oid);
      } else if (tog.limitReached()) {
        // 2) the object is available, but we dont use it to prevent huge commits, large txn acks etc
        newRequests.add(oid);
        // log(shortDescription());
        // log("Limit Reached. " + oid + " - " + tog.shortDescription());
      }
    }
    // TODO:: make cache and stats right
    if (!newRequests.isEmpty()) {
      LookupContext lookupContext = new LookupContext(newRequests);
      if (objectManager.lookupObjectsForCreateIfNecessary(txn.getChannelID(), newRequests, lookupContext)) {
        addLookedupObjects(lookupContext);
      } else {
        // New request went pending in object manager
        // log("lookupObjectsForApplyAndAddToSink(): New Request went pending : " + newRequests);
        makePending = true;
        pendingObjectRequest.addAll(newRequests);
      }
    }
    if (makePending) {
      // log("lookupObjectsForApplyAndAddToSink(): Make Pending : " + txn.getServerTransactionID());
      makePending(txn);
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      TxnObjectGrouping newGrouping = new TxnObjectGrouping(txnID, txn.getNewRoots());
      mergeTransactionGroupings(oids, newGrouping);
      applyPendingTxns.put(txnID, newGrouping);
      applySink.add(new ApplyTransactionContext(txn, getRequiredObjectsMap(oids, newGrouping.getObjects())));
      makeUnpending(txn);
      // log("lookupObjectsForApplyAndAddToSink(): Success: " + txn.getServerTransactionID());
    }
  }

  private String shortDescription() {
    return "TxnObjectManager : checked Out count = " + checkedOutObjects.size() + " apply pending txn = "
           + applyPendingTxns.size() + " commit pending = " + commitPendingTxns.size() + " pending txns = "
           + pendingTxnList.size() + " pending object requests = " + pendingObjectRequest.size();
  }

  private Map getRequiredObjectsMap(Collection oids, Map objects) {
    HashMap map = new HashMap(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      Object oid = i.next();
      Object mo = objects.get(oid);
      if (mo == null) {
        dump();
        log("NULL !! " + oid + " not found ! " + oids);
        log("Map contains " + objects);
        throw new AssertionError("Object is NULL !! : " + oid);
      }
      map.put(oid, mo);
    }
    return map;
  }

  private void log(String message) {
    System.err.println(Thread.currentThread() + " :: " + message);
  }

  // This method written to be optimized to perform large merges fast. Hence the code flow might not
  // look natural.
  private void mergeTransactionGroupings(Collection oids, TxnObjectGrouping newGrouping) {
    long start = System.currentTimeMillis();
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      TxnObjectGrouping oldGrouping = (TxnObjectGrouping) checkedOutObjects.get(oid);
      if (oldGrouping == null) {
        throw new AssertionError("Transaction Grouping for lookedup objects is Null !! " + oid);
      } else if (oldGrouping != newGrouping && oldGrouping.isActive()) {
        ServerTransactionID oldTxnId = oldGrouping.getServerTransactionID();
        // This merge has a sideeffect of setting all reference contained in oldGrouping to null.
        newGrouping.merge(oldGrouping);
        commitPendingTxns.remove(oldTxnId);
      }
    }
    for (Iterator j = newGrouping.getObjects().keySet().iterator(); j.hasNext();) {
      checkedOutObjects.put(j.next(), newGrouping);
    }
    for (Iterator j = newGrouping.getApplyPendingTxnsIterator(); j.hasNext();) {
      ServerTransactionID oldTxnId = (ServerTransactionID) j.next();
      if (applyPendingTxns.containsKey(oldTxnId)) {
        applyPendingTxns.put(oldTxnId, newGrouping);
      }
    }
    long timeTaken = System.currentTimeMillis() - start;
    if (timeTaken > 500) {
      log("Merged " + oids.size() + " object into " + newGrouping.shortDescription() + " in " + timeTaken + " ms");
    }
  }

  private synchronized void addLookedupObjects(LookupContext context) {
    Map lookedUpObjects = context.getLookedUpObjects();
    Assert.assertTrue(lookedUpObjects != null && lookedUpObjects.size() > 0);
    TxnObjectGrouping tg = new TxnObjectGrouping(lookedUpObjects);
    for (Iterator i = lookedUpObjects.keySet().iterator(); i.hasNext();) {
      Object oid = i.next();
      pendingObjectRequest.remove(oid);
      checkedOutObjects.put(oid, tg);
    }
  }

  private void makePending(ServerTransaction txn) {
    if (pendingTxnList.add(txn)) {
      sequencer.makePending(txn);
    }
  }

  private void makeUnpending(ServerTransaction txn) {
    if (pendingTxnList.remove(txn)) {
      sequencer.processedPendingTxn(txn);
    }
  }

  private boolean addProcessedPendingLookups() {
    LookupContext c;
    boolean processedPending = false;
    try {
      while ((c = (LookupContext) processedPendingLookups.poll(0)) != null) {
        addLookedupObjects(c);
        processedPending = true;
      }
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    return processedPending;
  }

  private void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

  private void addProcessedPending(LookupContext context) {
    try {
      processedPendingLookups.put(context);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    initiateLookup();
  }

  private void processPendingTransactions() {
    List copy = pendingTxnList.copy();
    for (Iterator i = copy.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      lookupObjectsForApplyAndAddToSink(txn);
    }
  }

  private void processApplyComplete() {
    try {
      ServerTransactionID txnID;
      ArrayList txnIDs = new ArrayList();
      while ((txnID = (ServerTransactionID) processedApplys.poll(0)) != null) {
        txnIDs.add(txnID);
      }
      if (txnIDs.size() > 0) {
        processApplyTxnComplete(txnIDs);
      }
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  }

  // ApplyTransaction stage method
  public boolean applyTransactionComplete(ServerTransactionID stxnID) {
    try {
      processedApplys.put(stxnID);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    initiateLookup();
    return true;
  }

  private synchronized void processApplyTxnComplete(ArrayList txnIDs) {
    for (Iterator i = txnIDs.iterator(); i.hasNext();) {
      ServerTransactionID stxnID = (ServerTransactionID) i.next();
      processApplyTxnComplete(stxnID);
    }
  }

  private void processApplyTxnComplete(ServerTransactionID stxnID) {
    TxnObjectGrouping grouping = (TxnObjectGrouping) applyPendingTxns.remove(stxnID);
    Assert.assertNotNull(grouping);
    if (grouping.applyComplete(stxnID)) {
      // Since verifying against all txns is costly, only the prime one (the one that created this grouping) is verfied
      // against
      ServerTransactionID pTxnID = grouping.getServerTransactionID();
      Assert.assertNull(applyPendingTxns.get(pTxnID));
      Object old = commitPendingTxns.put(pTxnID, grouping);
      Assert.assertNull(old);
    }
  }

  private synchronized void initiateCommitTransactions() {
    synchronized (processingCommits) {
      while (processingCommits.size() < commitThreadsCount && commitPendingTxns.size() > 0) {
        Map newRoots = new HashMap();
        Map objects = new HashMap();
        Collection txnIDs = new ArrayList();
        for (Iterator i = commitPendingTxns.values().iterator(); i.hasNext();) {
          TxnObjectGrouping tog = (TxnObjectGrouping) i.next();
          newRoots.putAll(tog.getNewRoots());
          txnIDs.addAll(tog.getTxnIDs());
          objects.putAll(tog.getObjects());
          i.remove();
          if (objects.size() > 5000) {
            break;
          }
        }
        CommitTransactionContext ctc = new CommitTransactionContext(txnIDs, objects.values(), newRoots,
                                                                    getCompletedTxnIds());
        for (Iterator j = objects.keySet().iterator(); j.hasNext();) {
          Object old = checkedOutObjects.remove(j.next());
          Assert.assertNotNull(old);
        }
        commitSink.add(ctc);
        processingCommits.add(ctc);
      }
    }
  }

  // This should be called from a thread already holding the monitor for this object
  // private boolean addToCommitStage(TxnObjectGrouping grouping) {
  // synchronized (processingCommits) {
  // if (processingCommits.size() < commitThreadsCount) {
  // Map objects = grouping.getObjects();
  // CommitTransactionContext ctc = new CommitTransactionContext(grouping.getTxnIDs(), objects.values(), grouping
  // .getNewRoots(), getCompletedTxnIds());
  // for (Iterator j = objects.keySet().iterator(); j.hasNext();) {
  // Object old = checkedOutObjects.remove(j.next());
  // Assert.assertTrue(old == grouping);
  // }
  // commitSink.add(ctc);
  // processingCommits.add(ctc);
  // return true;
  // }
  // return false;
  // }
  // }

  // Commit Transaction stage method
  public void commitTransactionsComplete(Collection txns) {
    synchronized (processingCommits) {
      processingCommits.remove(0);
    }
    initiateLookup();
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

  private class LookupContext implements ObjectManagerResultsContext {

    private boolean   pending = false;
    private Map       lookedUpObjects;
    private final Set oids;

    public LookupContext(Set oids) {
      this.oids = oids;
    }

    public synchronized boolean isPendingRequest() {
      return pending;
    }

    public synchronized void makePending(ChannelID channelID, Collection ids) {
      pending = true;
    }

    public synchronized void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
      lookedUpObjects = results.getObjects();
      if (pending) {
        TransactionalObjectManagerImpl.this.addProcessedPending(this);
      }
    }

    public synchronized Map getLookedUpObjects() {
      return lookedUpObjects;
    }

    public String toString() {
      return "LookupContext [ " + oids + "] = { pending = " + pending + ", lookedupObjects = "
             + lookedUpObjects.keySet() + "}";
    }
  }

  private static final class PendingList {
    LinkedHashMap pending = new LinkedHashMap();

    public boolean add(ServerTransaction txn) {
      ServerTransactionID sTxID = txn.getServerTransactionID();
      // Doing two lookups to avoid reordering
      if (pending.containsKey(sTxID)) {
        return false;
      } else {
        pending.put(sTxID, txn);
        return true;
      }
    }

    public List copy() {
      return new ArrayList(pending.values());
    }

    public boolean remove(ServerTransaction txn) {
      return (pending.remove(txn.getServerTransactionID()) != null);
    }

    public String toString() {
      return "PendingList : pending Txns = " + pending;
    }

    public int size() {
      return pending.size();
    }
  }
}
