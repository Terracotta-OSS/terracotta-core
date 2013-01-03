/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.handler.CallbackDumpAdapter;
import com.tc.handler.CallbackDumpHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
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
    ObjectIDSet oids = txn.getObjectIDs();
//    log("lookupObjectsForApplyAndAddToSink(): START : " + txn.getServerTransactionID() + " : " + oids);
    ObjectIDSet newRequests = new ObjectIDSet();
    boolean makePending = false;
    for (ObjectID oid : oids) {
      if (pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else if (!checkedOutObjects.containsKey(oid)) {
        newRequests.add(oid);
      }
    }
    LookupContext lookupContext = null;
    if (!newRequests.isEmpty()) {
      lookupContext = new LookupContext(newRequests, txn);
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
//      log("lookupObjectsForApplyAndAddToSink(): Make Pending : " + txn.getServerTransactionID());
      makePending(transactionLookupContext);
      if (lookupContext != null) {
        lookupContext.makePending();
      }
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      TxnObjectGrouping newGrouping = new TxnObjectGrouping(txnID, txn.getNewRoots(), getRequiredObjectsMap(oids, checkedOutObjects));
      applyPendingTxns.put(txnID, newGrouping);
      txnStageCoordinator.addToApplyStage(new ApplyTransactionContext(txn, newGrouping.getObjects(), needsApply));
      makeUnpending(txn);
//      log("lookupObjectsForApplyAndAddToSink(): Success: " + txn.getServerTransactionID());
    }
  }

  public String shortDescription() {
    return "TxnObjectManager : checked Out count = " + this.checkedOutObjects.size() + " apply pending txn = "
           + this.applyPendingTxns.size() + " pending txns = "
           + this.pendingTxnList.size() + " pending object requests = " + this.pendingObjectRequest.size();
  }

  private Map<ObjectID, ManagedObject> getRequiredObjectsMap(Collection<ObjectID> oids, Map<ObjectID, ManagedObject> objects) {
    Map<ObjectID, ManagedObject> map = new HashMap<ObjectID, ManagedObject>(oids.size());
    for (ObjectID oid : oids) {
      ManagedObject mo = objects.remove(oid);
      if (mo == null) {
        dumpToLogger();
        log("NULL !! " + oid + " not found ! " + oids);
        log("Map contains " + objects);
        throw new AssertionError("Object is NULL !! : " + oid);
      }
      map.put(oid, mo);
    }
    return map;
  }

  private void dumpToLogger() {
    CallbackDumpHandler dumpHandler = new CallbackDumpHandler();
    dumpHandler.registerForDump(new CallbackDumpAdapter(this));
    dumpHandler.dump();
  }

  private void log(String message) {
    logger.info(message);
  }

  private synchronized void addLookedupObjects(LookupContext context) {
    Map<ObjectID, ManagedObject> lookedUpObjects = context.getLookedUpObjects();
    if (lookedUpObjects == null || lookedUpObjects.isEmpty()) { throw new AssertionError("Lookedup object is null : "
                                                                                         + lookedUpObjects
                                                                                         + " context = " + context); }
    for (Entry<ObjectID, ManagedObject> e : lookedUpObjects.entrySet()) {
      this.pendingObjectRequest.remove(e.getKey());
      this.checkedOutObjects.put(e.getKey(), e.getValue());
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
    applyInfo.addObjectsToBeReleased(grouping.getObjects().values());
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
    private boolean                      pending    = false;
    private boolean                      resultsSet = false;
    private Map<ObjectID, ManagedObject> lookedUpObjects;

    public LookupContext(ObjectIDSet oids, ServerTransaction txn) {
      this.oids = oids;
      this.txn = txn;
    }

    public synchronized void makePending() {
      this.pending = true;
      if (this.resultsSet) {
        TransactionalObjectManagerImpl.this.addProcessedPending(this);
      }
    }

    @Override
    public synchronized void setResults(ObjectManagerLookupResults results) {
      this.lookedUpObjects = results.getObjects();
      assertNoMissingObjects(results.getMissingObjectIDs());
      this.resultsSet = true;
      if (this.pending) {
        TransactionalObjectManagerImpl.this.addProcessedPending(this);
      }
    }

    public synchronized Map<ObjectID, ManagedObject> getLookedUpObjects() {
      return this.lookedUpObjects;
    }

    @Override
    public String toString() {
      return "LookupContext [ txnID = " + this.txn.getServerTransactionID() + ", oids = " + this.oids + ", seqID = "
             + this.txn.getClientSequenceID() + ", clientTxnID = " + this.txn.getTransactionID() + ", numTxn = "
             + this.txn.getNumApplicationTxn() + "] = { pending = " + this.pending + ", lookedupObjects.size() = "
             + (this.lookedUpObjects == null ? "0" : Integer.toString(this.lookedUpObjects.size())) + "}";
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
      if (!missing.isEmpty()) { throw new AssertionError("Lookup for non-exisistent Objects : " + missing
                                                         + " lookup context is : " + this); }
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
