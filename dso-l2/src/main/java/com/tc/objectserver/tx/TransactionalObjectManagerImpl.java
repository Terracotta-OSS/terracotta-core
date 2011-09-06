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
import com.tc.objectserver.context.CommitTransactionContext;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.objectserver.context.RecallObjectsContext;
import com.tc.objectserver.context.TransactionLookupContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.managedobject.ApplyTransactionInfo;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrintable;
import com.tc.text.PrettyPrinter;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This class keeps track of locally checked out objects for applies and maintain the objects to txnid mapping in the
 * server. It wraps calls going to object manager from lookup, apply, commit stages
 */
public class TransactionalObjectManagerImpl implements TransactionalObjectManager, PrettyPrintable {

  private static final TCLogger                                       logger                  = TCLogging
                                                                                                  .getLogger(TransactionalObjectManagerImpl.class);
  private static final int                                            MAX_COMMIT_SIZE         = TCPropertiesImpl
                                                                                                  .getProperties()
                                                                                                  .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_MAXOBJECTS_TO_COMMIT);
  private final ObjectManager                                         objectManager;
  private final ServerTransactionSequencer                            sequencer;
  private final ServerGlobalTransactionManager                        gtxm;

  /*
   * This map contains ObjectIDs to TxnObjectGrouping that contains these objects
   */
  private final Map<ObjectID, TxnObjectGrouping>                      checkedOutObjects       = new HashMap<ObjectID, TxnObjectGrouping>();
  private final Map<ServerTransactionID, TxnObjectGrouping>           applyPendingTxns        = new HashMap<ServerTransactionID, TxnObjectGrouping>();
  private final LinkedHashMap<ServerTransactionID, TxnObjectGrouping> commitPendingTxns       = new LinkedHashMap<ServerTransactionID, TxnObjectGrouping>();

  private final Set                                                   pendingObjectRequest    = new HashSet();
  private final PendingList                                           pendingTxnList          = new PendingList();
  private final Queue<LookupContext>                                  processedPendingLookups = new ConcurrentLinkedQueue<LookupContext>();
  private final Queue<ApplyTransactionInfo>                           processedApplys         = new ConcurrentLinkedQueue<ApplyTransactionInfo>();

  private final TransactionalStageCoordinator                         txnStageCoordinator;

  public TransactionalObjectManagerImpl(ObjectManager objectManager, ServerTransactionSequencer sequencer,
                                        ServerGlobalTransactionManager gtxm,
                                        TransactionalStageCoordinator txnStageCoordinator) {
    this.objectManager = objectManager;
    this.sequencer = sequencer;
    this.gtxm = gtxm;
    this.txnStageCoordinator = txnStageCoordinator;
  }

  // ProcessTransactionHandler Method
  public void addTransactions(Collection txns) {
    try {
      Collection txnLookupContexts = createAndPreFetchObjectsFor(txns);
      this.sequencer.addTransactionLookupContexts(txnLookupContexts);
      this.txnStageCoordinator.initiateLookup();
    } catch (Throwable t) {
      logger.error(t);
      dumpOnError(txns);
      throw new AssertionError(t);
    }
  }

  private void dumpOnError(Collection txns) {
    try {
      for (Iterator i = txns.iterator(); i.hasNext();) {
        ServerTransaction stx = (ServerTransaction) i.next();
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

  private Collection createAndPreFetchObjectsFor(Collection txns) {
    List lookupContexts = new ArrayList(txns.size());
    Set oids = new HashSet(txns.size() * 10);
    Set newOids = new HashSet(txns.size() * 10);
    for (Iterator i = txns.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      boolean initiateApply = this.gtxm.initiateApply(txn.getServerTransactionID());
      if (initiateApply) {
        newOids.addAll(txn.getNewObjectIDs());
        for (Iterator j = txn.getObjectIDs().iterator(); j.hasNext();) {
          ObjectID oid = (ObjectID) j.next();
          if (!newOids.contains(oid)) {
            oids.add(oid);
          }
        }
      }
      lookupContexts.add(new TransactionLookupContext(txn, initiateApply));
    }
    this.objectManager.preFetchObjectsAndCreate(oids, newOids);
    return lookupContexts;
  }

  // LookupHandler Method
  public void lookupObjectsForTransactions() {
    processPendingIfNecessary();
    while (true) {
      TransactionLookupContext lookupContext = this.sequencer.getNextTxnLookupContextToProcess();
      if (lookupContext == null) {
        break;
      }
      ServerTransaction txn = lookupContext.getTransaction();
      if (lookupContext.initiateApply()) {
        lookupObjectsForApplyAndAddToSink(txn);
      } else {
        // These txns are already applied, hence just sending it to the next stage.
        this.txnStageCoordinator.addToApplyStage(new ApplyTransactionContext(txn));
      }
    }
  }

  private synchronized void processPendingIfNecessary() {
    if (addProcessedPendingLookups()) {
      processPendingTransactions();
    }
  }

  public synchronized void lookupObjectsForApplyAndAddToSink(ServerTransaction txn) {
    Collection oids = txn.getObjectIDs();
    // log("lookupObjectsForApplyAndAddToSink(): START : " + txn.getServerTransactionID() + " : " + oids);
    ObjectIDSet newRequests = new ObjectIDSet();
    boolean makePending = false;
    for (Iterator i = oids.iterator(); i.hasNext();) {
      ObjectID oid = (ObjectID) i.next();
      TxnObjectGrouping tog;
      if (this.pendingObjectRequest.contains(oid)) {
        makePending = true;
      } else if ((tog = this.checkedOutObjects.get(oid)) == null) {
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
    LookupContext lookupContext = null;
    if (!newRequests.isEmpty()) {
      lookupContext = new LookupContext(newRequests, txn);
      if (this.objectManager.lookupObjectsFor(txn.getSourceID(), lookupContext)) {
        addLookedupObjects(lookupContext);
      } else {
        // New request went pending in object manager
        // log("lookupObjectsForApplyAndAddToSink(): New Request went pending : " + newRequests);
        makePending = true;
        this.pendingObjectRequest.addAll(newRequests);
      }

    }
    if (makePending) {
      // log("lookupObjectsForApplyAndAddToSink(): Make Pending : " + txn.getServerTransactionID());
      makePending(txn);
      if (lookupContext != null) {
        lookupContext.makePending();
      }
    } else {
      ServerTransactionID txnID = txn.getServerTransactionID();
      TxnObjectGrouping newGrouping = new TxnObjectGrouping(txnID, txn.getNewRoots());
      mergeTransactionGroupings(oids, newGrouping);
      this.applyPendingTxns.put(txnID, newGrouping);
      this.txnStageCoordinator.addToApplyStage(new ApplyTransactionContext(txn, getRequiredObjectsMap(oids, newGrouping
          .getObjects())));
      makeUnpending(txn);
      // log("lookupObjectsForApplyAndAddToSink(): Success: " + txn.getServerTransactionID());
    }
  }

  public String shortDescription() {
    return "TxnObjectManager : checked Out count = " + this.checkedOutObjects.size() + " apply pending txn = "
           + this.applyPendingTxns.size() + " commit pending = " + this.commitPendingTxns.size() + " pending txns = "
           + this.pendingTxnList.size() + " pending object requests = " + this.pendingObjectRequest.size();
  }

  private Map getRequiredObjectsMap(Collection oids, Map objects) {
    HashMap map = new HashMap(oids.size());
    for (Iterator i = oids.iterator(); i.hasNext();) {
      Object oid = i.next();
      Object mo = objects.get(oid);
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

  // This method written to be optimized to perform large merges fast. Hence the code flow might not
  // look natural.
  private void mergeTransactionGroupings(Collection<ObjectID> oids, TxnObjectGrouping newGrouping) {
    long start = System.currentTimeMillis();
    for (ObjectID oid : oids) {
      TxnObjectGrouping oldGrouping = this.checkedOutObjects.get(oid);
      if (oldGrouping == null) {
        throw new AssertionError("Transaction Grouping for lookedup objects is Null !! " + oid);
      } else if (oldGrouping != newGrouping && oldGrouping.isActive()) {
        ServerTransactionID oldTxnId = oldGrouping.getServerTransactionID();
        // This merge has a sideeffect of setting all reference contained in oldGrouping to null.
        newGrouping.merge(oldGrouping);
        this.commitPendingTxns.remove(oldTxnId);
      }
    }
    for (ObjectID oid : newGrouping.getObjects().keySet()) {
      this.checkedOutObjects.put(oid, newGrouping);
    }
    for (Iterator<ServerTransactionID> j = newGrouping.getApplyPendingTxnsIterator(); j.hasNext();) {
      ServerTransactionID oldTxnId = j.next();
      if (this.applyPendingTxns.containsKey(oldTxnId)) {
        this.applyPendingTxns.put(oldTxnId, newGrouping);
      }
    }
    long timeTaken = System.currentTimeMillis() - start;
    if (timeTaken > 500) {
      log("Merged " + oids.size() + " object into " + newGrouping.shortDescription() + " in " + timeTaken + " ms");
    }
  }

  private synchronized void addLookedupObjects(LookupContext context) {
    Map<ObjectID, ManagedObject> lookedUpObjects = context.getLookedUpObjects();
    if (lookedUpObjects == null || lookedUpObjects.isEmpty()) { throw new AssertionError("Lookedup object is null : "
                                                                                         + lookedUpObjects
                                                                                         + " context = " + context); }
    TxnObjectGrouping tg = new TxnObjectGrouping(lookedUpObjects);
    for (ObjectID oid : lookedUpObjects.keySet()) {
      this.pendingObjectRequest.remove(oid);
      this.checkedOutObjects.put(oid, tg);
    }
  }

  private void makePending(ServerTransaction txn) {
    if (this.pendingTxnList.add(txn)) {
      this.sequencer.makePending(txn);
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
    List copy = this.pendingTxnList.copy();
    for (Iterator i = copy.iterator(); i.hasNext();) {
      ServerTransaction txn = (ServerTransaction) i.next();
      lookupObjectsForApplyAndAddToSink(txn);
    }
  }

  // ApplyTransaction stage method
  public boolean applyTransactionComplete(final ApplyTransactionInfo applyInfo) {
    this.processedApplys.add(applyInfo);
    this.txnStageCoordinator.initiateApplyComplete();
    return true;
  }

  // Apply Complete stage method
  public void processApplyComplete() {
    ApplyTransactionInfo applyInfo;
    List<ApplyTransactionInfo> completedApplyTxnInfos = new ArrayList<ApplyTransactionInfo>();
    while ((applyInfo = this.processedApplys.poll()) != null) {
      completedApplyTxnInfos.add(applyInfo);
    }
    if (completedApplyTxnInfos.size() > 0) {
      processApplyTxnComplete(completedApplyTxnInfos);
    }
  }

  private synchronized void processApplyTxnComplete(List<ApplyTransactionInfo> completedApplyTxnInfos) {
    for (ApplyTransactionInfo applyTxnInfo : completedApplyTxnInfos) {
      processApplyTxnComplete(applyTxnInfo);
    }
  }

  private void processApplyTxnComplete(ApplyTransactionInfo applyTxnInfo) {
    TxnObjectGrouping grouping = this.applyPendingTxns.remove(applyTxnInfo.getServerTransactionID());
    Assert.assertNotNull(grouping);
    if (grouping.applyComplete(applyTxnInfo)) {
      // Since verifying against all txns is costly, only the prime one (the one that created this grouping) is verfied
      // against
      ServerTransactionID pTxnID = grouping.getServerTransactionID();
      Assert.assertNull(this.applyPendingTxns.get(pTxnID));
      TxnObjectGrouping old = this.commitPendingTxns.put(pTxnID, grouping);
      Assert.assertNull(old);
      this.txnStageCoordinator.initiateCommit();
    }
  }

  // Commit Transaction stage method
  public synchronized void commitTransactionsComplete(CommitTransactionContext ctc) {

    if (this.commitPendingTxns.isEmpty()) { return; }

    Map<String, ObjectID> newRoots = new HashMap<String, ObjectID>();
    Map<ObjectID, ManagedObject> objects = new HashMap<ObjectID, ManagedObject>();
    Collection<ServerTransactionID> txnIDs = new ArrayList<ServerTransactionID>();
    SortedSet<ObjectID> deletedObjects = new ObjectIDSet();
    for (Iterator<TxnObjectGrouping> i = this.commitPendingTxns.values().iterator(); i.hasNext();) {
      TxnObjectGrouping tog = i.next();
      newRoots.putAll(tog.getNewRoots());
      txnIDs.addAll(tog.getTxnIDs());
      objects.putAll(tog.getObjects());
      deletedObjects.addAll(tog.getDeletedObjects());
      i.remove();
      if (objects.size() > MAX_COMMIT_SIZE) {
        break;
      }
    }
    ctc.initialize(txnIDs, objects.values(), newRoots, deletedObjects);

    for (ObjectID oid : objects.keySet()) {
      TxnObjectGrouping old = this.checkedOutObjects.remove(oid);
      Assert.assertNotNull(old);
    }

    if (!this.commitPendingTxns.isEmpty()) {
      // More commits needed
      this.txnStageCoordinator.initiateCommit();
    }
  }

  // recall from ObjectManager on DGC start
  public void recallAllCheckedoutObject() {
    this.txnStageCoordinator.initiateRecallAll();
  }

  // Recall Stage method
  public synchronized void recallCheckedoutObject(RecallObjectsContext roc) {
    processPendingIfNecessary();
    if (roc.recallAll()) {
      IdentityHashMap recalled = new IdentityHashMap();
      HashMap recalledObjects = new HashMap();
      for (Iterator i = this.checkedOutObjects.entrySet().iterator(); i.hasNext();) {
        Entry e = (Entry) i.next();
        TxnObjectGrouping tog = (TxnObjectGrouping) e.getValue();
        if (tog.getServerTransactionID().isNull()) {
          i.remove();
          if (!recalled.containsKey(tog)) {
            recalled.put(tog, null);
            recalledObjects.putAll(tog.getObjects());
          }
        }
      }
      if (!recalledObjects.isEmpty()) {
        logger.info("Recalling " + recalledObjects.size() + " Objects to ObjectManager");
        this.objectManager.releaseAllReadOnly(recalledObjects.values());
      }
    }
  }

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

    public ObjectIDSet getLookupIDs() {
      return this.oids;
    }

    public ObjectIDSet getNewObjectIDs() {
      return this.txn.getNewObjectIDs();
    }

    private void assertNoMissingObjects(ObjectIDSet missing) {
      if (!missing.isEmpty()) { throw new AssertionError("Lookup for non-exisistent Objects : " + missing
                                                         + " lookup context is : " + this); }
    }

    public boolean updateStats() {
      // These lookups are already preFetched. So don't update stats.
      return false;
    }

  }

  private static final class PendingList implements PrettyPrintable {
    private final LinkedHashMap<ServerTransactionID, ServerTransaction> pending = new LinkedHashMap<ServerTransactionID, ServerTransaction>();

    public boolean add(ServerTransaction txn) {
      ServerTransactionID sTxID = txn.getServerTransactionID();
      // Doing two lookups to avoid reordering
      if (this.pending.containsKey(sTxID)) {
        return false;
      } else {
        this.pending.put(sTxID, txn);
        return true;
      }
    }

    public List<ServerTransaction> copy() {
      return new ArrayList(this.pending.values());
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

    public PrettyPrinter prettyPrint(PrettyPrinter out) {
      out.print(getClass().getName()).print(" : ").print(this.pending.size());
      return out;
    }
  }
}
