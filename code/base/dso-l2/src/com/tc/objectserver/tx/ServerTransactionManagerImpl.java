/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;
import EDU.oswego.cs.dl.util.concurrent.Latch;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.gtx.GlobalTransactionManager;
import com.tc.object.net.ChannelStats;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.gtx.GlobalTransactionIDLowWaterMarkProvider;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.stats.counter.Counter;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Assert;
import com.tc.util.State;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

public class ServerTransactionManagerImpl implements ServerTransactionManager, ServerTransactionManagerMBean,
    GlobalTransactionManager {

  private static final TCLogger                         logger                   = TCLogging
                                                                                     .getLogger(ServerTransactionManager.class);

  private static final State                            PASSIVE_MODE             = new State("PASSIVE-MODE");
  private static final State                            ACTIVE_MODE              = new State("ACTIVE-MODE");

  // TODO::FIXME::Change this to concurrent HashMap
  private final Map                                     transactionAccounts      = Collections
                                                                                     .synchronizedMap(new HashMap());
  private final ClientStateManager                      stateManager;
  private final ObjectManager                           objectManager;
  private final ResentTransactionSequencer              resentTxnSequencer;
  private final TransactionAcknowledgeAction            action;
  private final LockManager                             lockManager;
  private final List                                    rootEventListeners       = new CopyOnWriteArrayList();
  private final List                                    txnEventListeners        = new CopyOnWriteArrayList();
  private final GlobalTransactionIDLowWaterMarkProvider lwmProvider;

  private final Counter                                 transactionRateCounter;

  private final ChannelStats                            channelStats;

  private final ServerGlobalTransactionManager          gtxm;

  private final ServerTransactionLogger                 txnLogger;

  private volatile State                                state                    = PASSIVE_MODE;
  private final AtomicInteger                           totalPendingTransactions = new AtomicInteger(0);
  private final AtomicInteger                           txnsCommitted            = new AtomicInteger(0);
  private final AtomicInteger                           objectsCommitted         = new AtomicInteger(0);
  private final AtomicInteger                           noOfCommits              = new AtomicInteger(0);
  private final boolean                                 commitLoggingEnabled;
  private volatile long                                 lastStatsTime            = 0;

  private Object                                        statsLock                = new Object();

  public ServerTransactionManagerImpl(ServerGlobalTransactionManager gtxm, TransactionStore transactionStore,
                                      LockManager lockManager, ClientStateManager stateManager,
                                      ObjectManager objectManager, TransactionalObjectManager txnObjectManager,
                                      TransactionAcknowledgeAction action, Counter transactionRateCounter,
                                      ChannelStats channelStats, ServerTransactionManagerConfig config) {
    this.gtxm = gtxm;
    this.lockManager = lockManager;
    this.objectManager = objectManager;
    this.stateManager = stateManager;
    this.resentTxnSequencer = new ResentTransactionSequencer(this, gtxm, txnObjectManager);
    this.action = action;
    this.transactionRateCounter = transactionRateCounter;
    this.channelStats = channelStats;
    this.lwmProvider = new GlobalTransactionIDLowWaterMarkProvider(this, gtxm);
    this.txnLogger = new ServerTransactionLogger(logger, config);
    if (config.isLoggingEnabled()) {
      enableTransactionLogger();
    }
    this.commitLoggingEnabled = config.isPrintCommitsEnabled();
  }

  public void enableTransactionLogger() {
    synchronized (txnLogger) {
      removeTransactionListener(txnLogger);
      addTransactionListener(txnLogger);
    }
  }

  public void disableTransactionLogger() {
    synchronized (txnLogger) {
      removeTransactionListener(txnLogger);
    }
  }

  public String dump() {
    StringWriter writer = new StringWriter();
    PrintWriter pw = new PrintWriter(writer);
    new PrettyPrinterImpl(pw).visit(this);
    writer.flush();
    return writer.toString();
  }

  public void dump(Writer writer) {
    PrintWriter pw = new PrintWriter(writer);
    pw.write(dump());
    pw.flush();
  }

  public void dumpToLogger() {
    logger.info(dump());
  }

  public synchronized PrettyPrinter prettyPrint(PrettyPrinter out) {
    out.println(getClass().getName());
    out.indent().print("transactionAccounts: ").visit(transactionAccounts).println();
    return out;
  }

  /**
   * Shutdown clients are not cleared immediately. Only on completing of all txns this is processed.
   */
  public void shutdownNode(final NodeID deadNodeID) {
    boolean callBackAdded = false;
    synchronized (transactionAccounts) {
      TransactionAccount deadClientTA = (TransactionAccount) transactionAccounts.get(deadNodeID);
      if (deadClientTA != null) {
        deadClientTA.nodeDead(new TransactionAccount.CallBackOnComplete() {
          public void onComplete(NodeID dead) {
            synchronized (ServerTransactionManagerImpl.this.transactionAccounts) {
              transactionAccounts.remove(deadNodeID);
            }
            stateManager.shutdownNode(deadNodeID);
            lockManager.clearAllLocksFor(deadNodeID);
            gtxm.shutdownNode(deadNodeID);
            fireClientDisconnectedEvent(deadNodeID);
          }
        });
        callBackAdded = true;
      }

      TransactionAccount tas[] = (TransactionAccount[]) transactionAccounts.values()
          .toArray(new TransactionAccount[transactionAccounts.size()]);
      for (int i = 0; i < tas.length; i++) {
        TransactionAccount client = tas[i];
        if (client == deadClientTA) continue;
        for (Iterator it = client.requestersWaitingFor(deadNodeID).iterator(); it.hasNext();) {
          TransactionID reqID = (TransactionID) it.next();
          acknowledgement(client.getNodeID(), reqID, deadNodeID);
        }
      }
    }

    if (!callBackAdded) {
      stateManager.shutdownNode(deadNodeID);
      lockManager.clearAllLocksFor(deadNodeID);
      gtxm.shutdownNode(deadNodeID);
      fireClientDisconnectedEvent(deadNodeID);
    }
  }

  public void nodeConnected(NodeID nodeID) {
    lockManager.enableLockStatsForNodeIfNeeded(nodeID);
  }

  public void start(Set cids) {
    synchronized (transactionAccounts) {
      int sizeB4 = transactionAccounts.size();
      transactionAccounts.keySet().retainAll(cids);
      int sizeAfter = transactionAccounts.size();
      if (sizeB4 != sizeAfter) {
        logger.warn("Cleaned up Transaction Accounts for : " + (sizeB4 - sizeAfter) + " clients");
      }
    }
    // XXX:: The server could have crashed right after a client crash/disconnect before it had a chance to remove
    // transactions from the DB. If we dont do this, then these will stick around for ever and cause low-water mark to
    // remain the same for ever and ever.
    // For Network enabled Active/Passive, when a passive becomes active, this will be called and the passive (now
    // active) will correct itself.
    gtxm.shutdownAllClientsExcept(cids);
    fireTransactionManagerStartedEvent(cids);
  }

  public void goToActiveMode() {
    waitForTxnsToComplete();
    state = ACTIVE_MODE;
    resentTxnSequencer.goToActiveMode();
    lwmProvider.goToActiveMode();
  }

  private void waitForTxnsToComplete() {
    final Latch latch = new Latch();
    logger.info("Waiting for txns to complete");
    callBackOnTxnsInSystemCompletion(new TxnsInSystemCompletionLister() {
      public void onCompletion() {
        logger.info("No more txns in the system.");
        latch.release();
      }
    });
    try {
      latch.acquire();
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

  }

  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    return lwmProvider.getLowGlobalTransactionIDWatermark();
  }

  public void addWaitingForAcknowledgement(NodeID waiter, TransactionID txnID, NodeID waitee) {
    TransactionAccount ci = getTransactionAccount(waiter);
    if (ci != null) {
      ci.addWaitee(waitee, txnID);
    } else {
      logger.warn("Not adding to Waiting for Ack since Waiter not found in the states map: " + waiter);
    }
    if (isActive() && waitee.getType() == NodeID.L1_NODE_TYPE) {
      channelStats.notifyTransactionBroadcastedTo(waitee);
    }
  }

  // This method is called when objects are sent to sync, this is done to maintain correct booking since things like DGC
  // relies on this to decide when to send the results
  public void objectsSynched(NodeID to, ServerTransactionID stid) {
    TransactionAccount ci = getOrCreateObjectSyncTransactionAccount(stid.getSourceID()); // Local Node ID
    totalPendingTransactions.incrementAndGet();
    ci.addWaitee(to, stid.getClientTransactionID());
  }

  // For testing
  public boolean isWaiting(NodeID waiter, TransactionID txnID) {
    TransactionAccount c = getTransactionAccount(waiter);
    return c != null && c.hasWaitees(txnID);
  }

  private void acknowledge(NodeID waiter, TransactionID txnID) {
    final ServerTransactionID serverTxnID = new ServerTransactionID(waiter, txnID);
    totalPendingTransactions.decrementAndGet();
    fireTransactionCompleteEvent(serverTxnID);
    if (isActive() && waiter.getType() == NodeID.L1_NODE_TYPE) {
      action.acknowledgeTransaction(serverTxnID);
    }
  }

  public void acknowledgement(NodeID waiter, TransactionID txnID, NodeID waitee) {

    // NOTE ::Sometime you can get double notification for the same txn in server restart cases. In those cases the
    // accounting could be messed up. The counter is set to have a minimum bound of zero to avoid ugly negative values.
    // @see ChannelStatsImpl
    if (isActive() && waitee.getType() == NodeID.L1_NODE_TYPE) {
      channelStats.notifyTransactionAckedFrom(waitee);
    }

    TransactionAccount transactionAccount = getTransactionAccount(waiter);
    if (transactionAccount == null) {
      // This can happen if an ack makes it into the system and the server crashed
      // leading to a removed state;
      logger.warn("Waiter not found in the states map: " + waiter);
      return;
    }
    if (transactionAccount.removeWaitee(waitee, txnID)) {
      acknowledge(waiter, txnID);
    }
  }

  public void apply(ServerTransaction txn, Map objects, BackReferences includeIDs, ObjectInstanceMonitor instanceMonitor) {

    final ServerTransactionID stxnID = txn.getServerTransactionID();
    final NodeID sourceID = txn.getSourceID();
    final TransactionID txnID = txn.getTransactionID();
    final List changes = txn.getChanges();

    GlobalTransactionID gtxID = txn.getGlobalTransactionID();

    boolean active = isActive();

    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNA orgDNA = (DNA) i.next();
      long version = orgDNA.getVersion();
      if (version == DNA.NULL_VERSION) {
        Assert.assertFalse(gtxID.isNull());
        version = gtxID.toLong();
      }
      DNA change = new VersionizedDNAWrapper(orgDNA, version, true);
      ManagedObject mo = (ManagedObject) objects.get(change.getObjectID());
      mo.apply(change, txnID, includeIDs, instanceMonitor, !active);
      if (active && !change.isDelta()) {
        // Only New objects reference are added here
        stateManager.addReference(txn.getSourceID(), mo.getID());
      }
    }

    Map newRoots = txn.getNewRoots();

    if (newRoots.size() > 0) {
      for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
        Entry entry = (Entry) i.next();
        String rootName = (String) entry.getKey();
        ObjectID newID = (ObjectID) entry.getValue();
        objectManager.createRoot(rootName, newID);
      }
    }
    if (active) {
      channelStats.notifyTransaction(sourceID);
    }
    transactionRateCounter.increment(txn.getNumApplicationTxn());

    fireTransactionAppliedEvent(stxnID);
  }

  public void skipApplyAndCommit(ServerTransaction txn) {
    final NodeID nodeID = txn.getSourceID();
    final TransactionID txnID = txn.getTransactionID();
    TransactionAccount ci = getTransactionAccount(nodeID);
    fireTransactionAppliedEvent(txn.getServerTransactionID());
    if (ci.skipApplyAndCommit(txnID)) {
      acknowledge(nodeID, txnID);
    }
  }

  /**
   * Earlier this method used to call releaseAll and then explicitly do a commit on the txn. But that implies that the
   * objects are released for other lookups before it it committed to disk. This is good for performance reason but
   * imposes a problem. The clients could read an object that has changes but it not committed to the disk yet and If
   * the server crashes then transactions are resent and may be re-applied in the clients when it should not have
   * re-applied. To avoid this we now commit inline before releasing the objects.
   * 
   * @see ObjectManagerImpl.releaseAll() for more details.
   */
  public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                     Collection appliedServerTransactionIDs) {
    PersistenceTransaction ptx = ptxp.newTransaction();
    gtxm.commitAll(ptx, appliedServerTransactionIDs);
    // This call commits the transaction too.
    objectManager.releaseAll(ptx, objects);
    fireRootCreatedEvents(newRoots);
    committed(appliedServerTransactionIDs);
    if (commitLoggingEnabled) updateCommittedStats(appliedServerTransactionIDs.size(), objects.size());
  }

  private void updateCommittedStats(int noOfTxns, int noOfObjects) {
    txnsCommitted.addAndGet(noOfTxns);
    objectsCommitted.addAndGet(noOfObjects);
    noOfCommits.incrementAndGet();
    long now = System.currentTimeMillis();
    if (now - lastStatsTime > 5000) {
      synchronized (statsLock) {
        if (now - lastStatsTime > 5000) {
          lastStatsTime = now;
          logger.info("Last 5 secs : No Of Txns committed : " + txnsCommitted.getAndSet(0)
                      + " No of Objects Commited : " + objectsCommitted.getAndSet(0) + " No of commits : "
                      + noOfCommits.getAndSet(0));
        }
      }
    }
  }

  /**
   * NOTE: Its important to have released all objects in the TXN before calling this event as the listeners tries to
   * lookup for the object and blocks
   */
  private void fireRootCreatedEvents(Map newRoots) {
    for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      fireRootCreatedEvent((String) entry.getKey(), (ObjectID) entry.getValue());
    }
  }

  public void incomingTransactions(NodeID source, Set txnIDs, Collection txns, boolean relayed) {
    final boolean active = isActive();
    TransactionAccount ci = getOrCreateTransactionAccount(source);
    ci.incommingTransactions(txnIDs);
    totalPendingTransactions.addAndGet(txnIDs.size());
    for (Iterator i = txns.iterator(); i.hasNext();) {
      final ServerTransaction txn = (ServerTransaction) i.next();
      final ServerTransactionID stxnID = txn.getServerTransactionID();
      final TransactionID txnID = stxnID.getClientTransactionID();
      if (active && !relayed) {
        ci.relayTransactionComplete(txnID);
      } else if (!active) {
        gtxm.createGlobalTransactionDescIfNeeded(stxnID, txn.getGlobalTransactionID());
      }
    }
    fireIncomingTransactionsEvent(source, txnIDs);
    resentTxnSequencer.addTransactions(txns);
  }

  public int getTotalPendingTransactionsCount() {
    return totalPendingTransactions.get();
  }

  private boolean isActive() {
    return (state == ACTIVE_MODE);
  }

  public void transactionsRelayed(NodeID node, Set serverTxnIDs) {
    TransactionAccount ci = getTransactionAccount(node);
    if (ci == null) {
      logger.warn("transactionsRelayed(): TransactionAccount not found for " + node);
      return;
    }
    for (Iterator i = serverTxnIDs.iterator(); i.hasNext();) {
      final ServerTransactionID txnId = (ServerTransactionID) i.next();
      final TransactionID txnID = txnId.getClientTransactionID();
      if (ci.relayTransactionComplete(txnID)) {
        acknowledge(node, txnID);
      }
    }
  }

  private void committed(Collection txnsIds) {
    for (Iterator i = txnsIds.iterator(); i.hasNext();) {
      final ServerTransactionID txnId = (ServerTransactionID) i.next();
      final NodeID waiter = txnId.getSourceID();
      final TransactionID txnID = txnId.getClientTransactionID();

      TransactionAccount ci = getTransactionAccount(waiter);
      if (ci != null && ci.applyCommitted(txnID)) {
        acknowledge(waiter, txnID);
      }
    }
  }

  public void broadcasted(NodeID waiter, TransactionID txnID) {
    TransactionAccount ci = getTransactionAccount(waiter);

    if (ci != null && ci.broadcastCompleted(txnID)) {
      acknowledge(waiter, txnID);
    }
  }

  private TransactionAccount getOrCreateObjectSyncTransactionAccount(NodeID localID) {
    synchronized (transactionAccounts) {
      assert state == ACTIVE_MODE;
      TransactionAccount ta = (TransactionAccount) transactionAccounts.get(localID);
      if (ta == null) {
        transactionAccounts.put(localID, (ta = new ObjectSynchTransactionAccount(localID)));
      }
      return ta;
    }
  }

  private TransactionAccount getOrCreateTransactionAccount(NodeID source) {
    synchronized (transactionAccounts) {
      TransactionAccount ta = (TransactionAccount) transactionAccounts.get(source);
      if (ta != null && ta instanceof ObjectSynchTransactionAccount) { throw new AssertionError(
                                                                                                "Transaction Account is of type ObjectSyncTransactionAccount : "
                                                                                                    + ta
                                                                                                    + " source Id  : "
                                                                                                    + source); }
      if (state == ACTIVE_MODE) {
        if ((ta == null) || (ta instanceof PassiveTransactionAccount)) {
          Object old = transactionAccounts.put(source, (ta = new TransactionAccountImpl(source)));
          if (old != null) {
            logger.info("Transaction Account changed from : " + old + " to " + ta);
          }
        }
      } else {
        if ((ta == null) || (ta instanceof TransactionAccountImpl)) {
          Object old = transactionAccounts.put(source, (ta = new PassiveTransactionAccount(source)));
          if (old != null) {
            logger.info("Transaction Account changed from : " + old + " to " + ta);
          }
        }
      }
      return ta;
    }
  }

  private TransactionAccount getTransactionAccount(NodeID node) {
    return (TransactionAccount) transactionAccounts.get(node);
  }

  public void addRootListener(ServerTransactionManagerEventListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener cannot be null"); }
    this.rootEventListeners.add(listener);
  }

  private void fireRootCreatedEvent(String rootName, ObjectID id) {
    for (Iterator iter = rootEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionManagerEventListener listener = (ServerTransactionManagerEventListener) iter.next();
        listener.rootCreated(rootName, id);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e);
        } else {
          logger.warn("Exception in rootCreated event callback: " + e.getMessage());
        }
      }
    }
  }

  public void addTransactionListener(ServerTransactionListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener cannot be null"); }
    this.txnEventListeners.add(listener);
  }

  public void removeTransactionListener(ServerTransactionListener listener) {
    if (listener == null) { throw new IllegalArgumentException("listener cannot be null"); }
    this.txnEventListeners.remove(listener);
  }

  public void callBackOnTxnsInSystemCompletion(TxnsInSystemCompletionLister l) {
    TxnsInSystemCompletionListenerCallback callBack = new TxnsInSystemCompletionListenerCallback(l);
    Set txnsInSystem = callBack.getTxnsInSystem();
    synchronized (transactionAccounts) {
      // DEV-1874, MNK-683 :: Register before adding pending server transaction ids to avoid race.
      addTransactionListener(callBack);
      for (Iterator i = transactionAccounts.entrySet().iterator(); i.hasNext();) {
        Entry entry = (Entry) i.next();
        TransactionAccount client = (TransactionAccount) entry.getValue();
        client.addAllPendingServerTransactionIDsTo(txnsInSystem);
      }
    }
    callBack.initializationComplete();
  }

  private void fireIncomingTransactionsEvent(NodeID nodeID, Set serverTxnIDs) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.incomingTransactions(nodeID, serverTxnIDs);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  private void fireTransactionCompleteEvent(ServerTransactionID stxID) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.transactionCompleted(stxID);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  private void fireTransactionAppliedEvent(ServerTransactionID stxID) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.transactionApplied(stxID);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  private void fireTransactionManagerStartedEvent(Set cids) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.transactionManagerStarted(cids);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  public void setResentTransactionIDs(NodeID source, Collection transactionIDs) {
    if (transactionIDs.isEmpty()) return;
    Collection stxIDs = new ArrayList();
    for (Iterator iter = transactionIDs.iterator(); iter.hasNext();) {
      TransactionID txn = (TransactionID) iter.next();
      stxIDs.add(new ServerTransactionID(source, txn));
    }
    fireAddResentTransactionIDsEvent(stxIDs);
  }

  private void fireAddResentTransactionIDsEvent(Collection stxIDs) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.addResentServerTransactionIDs(stxIDs);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  private void fireClientDisconnectedEvent(NodeID deadNodeID) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.clearAllTransactionsFor(deadNodeID);
      } catch (Exception e) {
        logger.error("Exception in Txn listener event callback: ", e);
        throw new AssertionError(e);
      }
    }
  }

  private final class TxnsInSystemCompletionListenerCallback implements ServerTransactionListener {

    private final TxnsInSystemCompletionLister callback;
    private final Set                          txnsInSystem;
    private boolean                            initialized = false;
    private int                                count       = 0;
    private int                                lastSize    = -1;

    public TxnsInSystemCompletionListenerCallback(TxnsInSystemCompletionLister callback) {
      this.callback = callback;
      this.txnsInSystem = Collections.synchronizedSet(new HashSet());
    }

    public void initializationComplete() {
      synchronized (txnsInSystem) {
        initialized = true;
        lastSize = txnsInSystem.size();
        callBackIfEmpty();
      }
    }

    public Set getTxnsInSystem() {
      return txnsInSystem;
    }

    public void addResentServerTransactionIDs(Collection stxIDs) {
      // NOP
    }

    public void clearAllTransactionsFor(NodeID deadNode) {
      // NOP
    }

    public void incomingTransactions(NodeID source, Set serverTxnIDs) {
      // NOP
    }

    public void transactionApplied(ServerTransactionID stxID) {
      // NOP
    }

    public void transactionCompleted(ServerTransactionID stxID) {
      synchronized (txnsInSystem) {
        if (txnsInSystem.remove(stxID) && initialized) {
          callBackIfEmpty();
        }
        if (++count % 1000 == 0) {
          if (lastSize == txnsInSystem.size()) {
            logger.warn("TxnsInSystemCompletionLister :: Still waiting for completion of " + txnsInSystem.size()
                        + " txns to call callback " + callback + " count = " + count + " lastSize = " + lastSize
                        + " No change in size. Txns = " + shortDesc(txnsInSystem));
          } else {
            lastSize = txnsInSystem.size();
          }
        }
      }
    }

    private void callBackIfEmpty() {
      if (txnsInSystem.isEmpty()) {
        ServerTransactionManagerImpl.this.removeTransactionListener(this);
        callback.onCompletion();
      }
    }

    private String shortDesc(Set txns) {
      if (txns == null) {
        return "null";
      } else if (txns.size() < 11) {
        return txns.toString();
      } else {
        StringBuilder sb = new StringBuilder("{");
        int j = 0;
        for (Iterator i = txns.iterator(); j < 11 && i.hasNext(); j++) {
          sb.append(i.next()).append(",");
        }
        sb.append("....<more> }");
        return sb.toString();
      }
    }

    public void transactionManagerStarted(Set cids) {
      // NOP
    }
  }
}
