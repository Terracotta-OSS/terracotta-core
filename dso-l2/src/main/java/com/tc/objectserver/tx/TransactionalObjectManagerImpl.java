/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.FlushApplyCommitContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.BitSetObjectIDSet;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

/**
 * This class keeps track of locally checked out objects for applies and maintain the objects to txnid mapping in the
 * server. It wraps calls going to object manager from lookup, apply, commit stages
 */
public class TransactionalObjectManagerImpl implements TransactionalObjectManager, PrettyPrintable {
  private static final TCLogger                                       logger                  = TCLogging
                                                                                                  .getLogger(TransactionalObjectManagerImpl.class);

  private final ObjectManager                                         objectManager;
  private final ServerTransactionSequencer                            sequencer               = new ServerTransactionSequencerImpl();
  private final ServerGlobalTransactionManager                        gtxm;

  private final Map<ObjectID, ManagedObject>                          checkedOutObjects       = new HashMap<ObjectID, ManagedObject>();
  private final ConcurrentMap<ObjectID, TxnObjectGrouping>            liveObjectGroupings     = new ConcurrentHashMap<ObjectID, TxnObjectGrouping>();
  private final ConcurrentMap<ServerTransactionID, TxnObjectGrouping> applyPendingTxns        = new ConcurrentHashMap<ServerTransactionID, TxnObjectGrouping>();

  private final Set<ObjectID>                                         pendingObjectRequest    = new HashSet<ObjectID>();
  private final PendingList                                           pendingTxnList          = new PendingList();
  private final Queue<LookupContext>                                  processedPendingLookups = new ConcurrentLinkedQueue<LookupContext>();

  private final TransactionalStageCoordinator                         txnStageCoordinator;

  public TransactionalObjectManagerImpl(ObjectManager objectManager,
                                        ServerGlobalTransactionManager gtxm,
                                        TransactionalStageCoordinator txnStageCoordinator) {
    this.objectManager = objectManager;
    this.gtxm = gtxm;
    this.txnStageCoordinator = txnStageCoordinator;
  }

  // ProcessTransactionHandler Method
  @Override
  public void addTransactions(Collection<ServerTransaction> txns) {
    try {
      Collection<TransactionLookupContext> txnLookupContexts = createObjectsFor(txns);
      this.sequencer.addTransactionLookupContexts(txnLookupContexts);
      this.txnStageCoordinator.initiateLookup();
    } catch (Throwable t) {
      logger.error(t);
      dumpOnError(txns);
      throw new AssertionError(t);
    }
  }

  private void dumpOnError(Collection<ServerTransaction> txns) {
    try {
      for (final ServerTransaction stx : txns) {
        ServerTransactionID stxn = stx.getServerTransactionID();
        logger.error("DumpOnError : Txn = " + stx);
        // NOTE:: Calling initiateApply() changes state, but we are crashing anyways
        logger.error("DumpOnError : GID for Txn " + stxn + " is " + this.gtxm.getGlobalTransactionID(stxn)
                     + " : initate apply : " + this.gtxm.initiateApply(stxn));
      }
      logger.error("DumpOnError : GID Low watermark : " + this.gtxm.getLowGlobalTransactionIDWatermark());
      logger.error("DumpOnError : GID Sequence current : " + this.gtxm.getGlobalTransactionIDSequence().current());
    } catch (Exception e) {
      logger.error("DumpOnError : Exception on dumpOnError", e);
    }
  }

  private Collection<TransactionLookupContext> createObjectsFor(Collection<ServerTransaction> txns) {
    List<TransactionLookupContext> lookupContexts = new ArrayList<TransactionLookupContext>(txns.size());
    Set<ObjectID> newOids = new HashSet<ObjectID>(txns.size() * 10);
    for (ServerTransaction txn : txns) {
      boolean initiateApply = this.gtxm.initiateApply(txn.getServerTransactionID());
      if (initiateApply) {
        newOids.addAll(txn.getNewObjectIDs());
      }
      lookupContexts.add(new TransactionLookupContext(txn, initiateApply));
    }
    objectManager.createNewObjects(newOids);
    return lookupContexts;
  }

  // LookupHandler Method
  @Override
  public void lookupObjectsForTransactions() {
    processPendingIfNecessary();
    while (true) {
      TransactionLookupContext lookupContext = this.sequencer.getNextTxnLookupContextToProcess();
      if (lookupContext == null) {
        break;
      }
      lookupObjectsForApplyAndAddToSink(lookupContext);
    }
  }

  private synchronized void processPendingIfNecessary() {
    if (addProcessedPendingLookups()) {
      processPendingTransactions();
    }
  }

  public synchronized void lookupObjectsForApplyAndAddToSink(TransactionLookupContext transactionLookupContext) {
    ServerTransaction txn = transactionLookupContext.getTransaction();
    boolean needsApply = transactionLookupContext.initiateApply();
    ObjectIDSet objectsToLookup = txn.getObjectIDs();
//    log("lookupObjectsForApplyAndAddToSink(): START : " + txn.getServerTransactionID() + " : " + objectsToLookup);

    TxnObjectGrouping grouping = null;

    // Check to see if a live object grouping has all this txn's objects, if not we'll need to need to do a fresh checkout
    ObjectIDSet oldObjectIDs = new BitSetObjectIDSet(objectsToLookup);
    oldObjectIDs.removeAll(txn.getNewObjectIDs());
    if (!oldObjectIDs.isEmpty()) {
      TxnObjectGrouping existingGrouping = liveObjectGroupings.get(oldObjectIDs.first());
      if (existingGrouping != null && existingGrouping.containsAll(oldObjectIDs) && existingGrouping.addServerTransactionID(txn.getServerTransactionID())) {
//        log("XXX allowing txn object grouping merge. Existing " + grouping + " oids " + oldObjectIDs);
        // All the existing objects are already part of the grouping, so we only need to look up new objects now.
        objectsToLookup = txn.getNewObjectIDs();
        grouping = existingGrouping;
      }
    }

    ObjectIDSet newRequests = new BitSetObjectIDSet();
    boolean makePending = false;
    for (ObjectID oid : objectsToLookup) {
      if (pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else if (!checkedOutObjects.containsKey(oid)) {
        newRequests.add(oid);
      }
    }
    LookupContext lookupContext = null;
    if (!newRequests.isEmpty()) {
      lookupContext = new LookupContext(newRequests, txn, transactionLookupContext.initiateApply());
      if (this.objectManager.lookupObjectsFor(txn.getSourceID(), lookupContext)) {
        addLookedupObjects(lookupContext);
      } else {
        // New request went pending in object manager
//        log("lookupObjectsForApplyAndAddToSink(): New Request went pending : " + newRequests);
        makePending = true;
        this.pendingObjectRequest.addAll(newRequests);
      }
    }
    if (makePending) {
      if (grouping != null) {
        // Would have merged here, but the new object lookup went pending. Most likely due to DGC blocking all lookups

        // Kill all object groupings associated with this txn.
        for (ObjectID oid : txn.getObjectIDs()) {
          liveObjectGroupings.remove(oid);
        }

        // Try to "finish" the grouping. If we so happen to be the last transaction (that would have triggered a commit)
        // send it through to the appropriate apply thread to perform the commit.
        if (grouping.transactionComplete(txn.getServerTransactionID())) {
          txnStageCoordinator.addToApplyStage(new FlushApplyCommitContext(grouping));
        }
      }

//      log("lookupObjectsForApplyAndAddToSink(): Make Pending : " + txn.getServerTransactionID());
      makePending(transactionLookupContext);
      if (lookupContext != null) {
        lookupContext.makePending();
      }
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      Collection<ObjectID> missingObjects;
      if (grouping == null) {
        grouping = new TxnObjectGrouping(txnID);
        missingObjects = addObjectsToGrouping(txn.getObjectIDs(), grouping, transactionLookupContext.initiateApply());
      } else {
        missingObjects = addObjectsToGrouping(txn.getNewObjectIDs(), grouping, transactionLookupContext.initiateApply());
      }
      applyPendingTxns.put(txnID, grouping);
      txnStageCoordinator.addToApplyStage(new ApplyTransactionContext(txn, grouping, needsApply, missingObjects));
      makeUnpending(txn);
//      log("lookupObjectsForApplyAndAddToSink(): Success: " + txn.getServerTransactionID());
    }
  }

  public String shortDescription() {
    return "TxnObjectManager : checked Out count = " + this.checkedOutObjects.size() + " pending txns = "
           + this.pendingTxnList.size() + " pending object requests = " + this.pendingObjectRequest.size()
           + " live object checkouts = " + liveObjectGroupings.size();
  }

  private Set<ObjectID> addObjectsToGrouping(Collection<ObjectID> oids, TxnObjectGrouping txnObjectGrouping, final boolean initiateApply) {
    Set<ObjectID> missingObjects = new HashSet<ObjectID>();
    for (ObjectID oid : oids) {
      ManagedObject mo = checkedOutObjects.remove(oid);
      if (!initiateApply && mo == null) {
        missingObjects.add(oid);
      } else if (mo == null) {
        log("NULL !! " + oid + " not found ! " + oids);
        throw new AssertionError("Object is NULL !! : " + oid);
      } else {
        txnObjectGrouping.addObject(oid, mo);
        liveObjectGroupings.put(oid, txnObjectGrouping);
      }
    }
    return missingObjects;
  }

  private void log(String message) {
    logger.info(message);
  }

  private synchronized void addLookedupObjects(LookupContext context) {
    Map<ObjectID, ManagedObject> lookedUpObjects = context.getLookedUpObjects();
    if (lookedUpObjects == null) { throw new AssertionError("Lookedup object is null : " + lookedUpObjects + " context = " + context); }
    for (Entry<ObjectID, ManagedObject> e : lookedUpObjects.entrySet()) {
      this.pendingObjectRequest.remove(e.getKey());
      this.checkedOutObjects.put(e.getKey(), e.getValue());
    }

    for (ObjectID missingObject : context.getMissingObjects()) {
      pendingObjectRequest.remove(missingObject);
    }
  }

  private void makePending(TransactionLookupContext lookupContext) {
    if (this.pendingTxnList.add(lookupContext)) {
      this.sequencer.makePending(lookupContext.getTransaction());
    }
  }

  private void makeUnpending(ServerTransaction txn) {
    if (this.pendingTxnList.remove(txn)) {
      this.sequencer.makeUnpending(txn);
    }
  }

  private boolean addProcessedPendingLookups() {
    LookupContext c;
    boolean processedPending = false;
    while ((c = this.processedPendingLookups.poll()) != null) {
      addLookedupObjects(c);
      processedPending = true;
    }
    return processedPending;
  }

  private void addProcessedPending(LookupContext context) {
    this.processedPendingLookups.add(context);
    this.txnStageCoordinator.initiateLookup();
  }

  private void processPendingTransactions() {
    List<TransactionLookupContext> copy = this.pendingTxnList.copy();
    for (final TransactionLookupContext lookupContext : copy) {
      lookupObjectsForApplyAndAddToSink(lookupContext);
    }
  }

  // ApplyTransaction stage method
  @Override
  public void applyTransactionComplete(final ApplyTransactionInfo applyInfo) {
    TxnObjectGrouping grouping = applyPendingTxns.remove(applyInfo.getServerTransactionID());
    Assert.assertNotNull(grouping);
    if (grouping.transactionComplete(applyInfo.getServerTransactionID())) {
      applyInfo.addObjectsToBeReleased(grouping.getObjects());
      for (ManagedObject mo : grouping.getObjects()) {
        liveObjectGroupings.remove(mo.getID());
      }
      applyInfo.setCommitNow(true);
    } else {
      applyInfo.setCommitNow(false);
    }
  }

  @Override
  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.print(this.getClass().getName()).flush();
    out.indent().print(shortDescription()).flush();
    return out;
  }

  private class LookupContext implements ObjectManagerResultsContext {

    private final ObjectIDSet            oids;
    private final ServerTransaction      txn;
    private final boolean                initiateApply;
    private boolean                      pending    = false;
    private boolean                      resultsSet = false;
    private Map<ObjectID, ManagedObject> lookedUpObjects;
    private Set<ObjectID> missingObjects;

    LookupContext(ObjectIDSet oids, ServerTransaction txn, final boolean initiateApply) {
      this.oids = oids;
      this.txn = txn;
      this.initiateApply = initiateApply;
    }

    synchronized void makePending() {
      this.pending = true;
      if (this.resultsSet) {
        TransactionalObjectManagerImpl.this.addProcessedPending(this);
      }
    }

    @Override
    public synchronized void setResults(ObjectManagerLookupResults results) {
      this.lookedUpObjects = results.getObjects();
      assertNoMissingObjects(results.getMissingObjectIDs());
      missingObjects = results.getMissingObjectIDs();
      this.resultsSet = true;
      if (this.pending) {
        TransactionalObjectManagerImpl.this.addProcessedPending(this);
      }
    }

    synchronized Map<ObjectID, ManagedObject> getLookedUpObjects() {
      return this.lookedUpObjects;
    }

    synchronized Set<ObjectID> getMissingObjects() {
      return missingObjects;
    }

    @Override
    public String toString() {
      return "LookupContext [ txnID = " + this.txn.getServerTransactionID() + ", oids = " + this.oids + ", seqID = "
             + this.txn.getClientSequenceID() + ", clientTxnID = " + this.txn.getTransactionID() + ", numTxn = "
             + this.txn.getNumApplicationTxn() + "] = { pending = " + this.pending + ", lookedupObjects.size() = "
             + (this.lookedUpObjects == null ? "0" : this.lookedUpObjects.size()) + "}";
    }

    @Override
    public ObjectIDSet getLookupIDs() {
      return this.oids;
    }

    @Override
    public ObjectIDSet getNewObjectIDs() {
      return this.txn.getNewObjectIDs();
    }

    private void assertNoMissingObjects(ObjectIDSet missing) {
      if (initiateApply && !missing.isEmpty()) {
        throw new AssertionError("Lookup for non-exisistent Objects : " + missing
                                                         + " lookup context is : " + this);
      }
    }

  }

  private static final class PendingList implements PrettyPrintable {
    private final LinkedHashMap<ServerTransactionID, TransactionLookupContext> pending = new LinkedHashMap<ServerTransactionID, TransactionLookupContext>();

    public boolean add(TransactionLookupContext lookupContext) {
      ServerTransactionID sTxID = lookupContext.getTransaction().getServerTransactionID();
      // Doing two lookups to avoid reordering
      if (this.pending.containsKey(sTxID)) {
        return false;
      } else {
        this.pending.put(sTxID, lookupContext);
        return true;
      }
    }

    public List<TransactionLookupContext> copy() {
      return new ArrayList<TransactionLookupContext>(this.pending.values());
    }

    public boolean remove(ServerTransaction txn) {
      return (this.pending.remove(txn.getServerTransactionID()) != null);
    }

    @Override
    public String toString() {
      return "PendingList : pending Txns = " + this.pending;
    }

    public int size() {
      return this.pending.size();
    }

    @Override
    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.print(getClass().getName()).print(" : ").print(this.pending.size());
      return out;
    }
  }
}
