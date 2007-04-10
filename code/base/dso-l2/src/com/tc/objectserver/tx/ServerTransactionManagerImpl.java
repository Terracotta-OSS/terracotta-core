/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArrayList;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.net.ChannelStats;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.objectserver.api.ObjectInstanceMonitor;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.managedobject.BackReferences;
import com.tc.objectserver.persistence.api.PersistenceTransaction;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.stats.counter.Counter;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class ServerTransactionManagerImpl implements ServerTransactionManager, ServerTransactionManagerMBean {

  private static final TCLogger                logger              = TCLogging
                                                                       .getLogger(ServerTransactionManager.class);

  private final Map                            transactionAccounts = Collections.synchronizedMap(new HashMap());
  private final ClientStateManager             stateManager;
  private final ObjectManager                  objectManager;
  private final TransactionAcknowledgeAction   action;
  private final LockManager                    lockManager;
  private final List                           rootEventListeners  = new CopyOnWriteArrayList();
  private final List                           txnEventListeners   = new CopyOnWriteArrayList();

  private final Counter                        transactionRateCounter;

  private final ChannelStats                   channelStats;

  private final ServerGlobalTransactionManager gtxm;

  public ServerTransactionManagerImpl(ServerGlobalTransactionManager gtxm, TransactionStore transactionStore,
                                      LockManager lockManager, ClientStateManager stateManager,
                                      ObjectManager objectManager, TransactionAcknowledgeAction action,
                                      Counter transactionRateCounter, ChannelStats channelStats) {
    this.gtxm = gtxm;
    this.lockManager = lockManager;
    this.objectManager = objectManager;
    this.stateManager = stateManager;
    this.action = action;
    this.transactionRateCounter = transactionRateCounter;
    this.channelStats = channelStats;
  }

  public void dump() {
    StringBuffer buf = new StringBuffer("ServerTransactionManager");
    buf.append("transactionAccounts: " + transactionAccounts);
    buf.append("\n/ServerTransactionManager");
    System.err.println(buf.toString());
  }

  // TODO:: shutdown clients should not be cleared immediately. some time to apply all the transactions
  // on the wire should be given before removing if from accounting and releasing the lock.
  public void shutdownClient(ChannelID waitee) {
    transactionAccounts.remove(waitee);
    Map currentStates = new HashMap(transactionAccounts);
    for (Iterator i = currentStates.keySet().iterator(); i.hasNext();) {
      ChannelID key = (ChannelID) i.next();

      TransactionAccount client = getTransactionAccount(key);
      if (client != null) {
        for (Iterator it = client.requestersWaitingFor(waitee).iterator(); it.hasNext();) {
          TransactionID reqID = (TransactionID) it.next();
          acknowledgement(client.getClientID(), reqID, waitee);
        }
      }
    }

    stateManager.shutdownClient(waitee);
    lockManager.clearAllLocksFor(waitee);
    gtxm.shutdownClient(waitee);
  }

  public void addWaitingForAcknowledgement(ChannelID waiter, TransactionID txnID, ChannelID waitee) {
    TransactionAccount ci = getOrCreateTransactionAccount(waiter);
    ci.addWaitee(waitee, txnID);
  }

  // For testing
  public boolean isWaiting(ChannelID waiter, TransactionID txnID) {
    TransactionAccount c = getTransactionAccount(waiter);
    return c != null && c.hasWaitees(txnID);
  }

  private void acknowledge(ChannelID waiter, TransactionID txnID) {
    final ServerTransactionID serverTxnID = new ServerTransactionID(waiter, txnID);
    fireTransactionCompleteEvent(serverTxnID);
    action.acknowledgeTransaction(serverTxnID);
  }

  public void acknowledgement(ChannelID waiter, TransactionID txnID, ChannelID waitee) {

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
    final ChannelID channelID = txn.getChannelID();
    final TransactionID txnID = txn.getTransactionID();
    final List changes = txn.getChanges();

    GlobalTransactionID gtxID = txn.getGlobalTransactionID();

    TransactionAccount ci;
    if (txn.isPassive()) {
      ci = getOrCreateNullTransactionAccount(channelID);
      if (!stxnID.isServerGeneratedTransacation()) gtxm.createGlobalTransactionDesc(stxnID, gtxID);
    } else {
      // There could potentically be a small leak if the clients crash and then shutdownClient() called before
      // apply() is called. Will create a TransactionAccount which will never get removed.
      ci = getOrCreateTransactionAccount(channelID);
    }
    ci.applyStarted(txnID);

    for (Iterator i = changes.iterator(); i.hasNext();) {
      DNA orgDNA = (DNA) i.next();
      long version = orgDNA.getVersion();
      if (version == DNA.NULL_VERSION) {
        Assert.assertFalse(gtxID.isNull());
        version = gtxID.toLong();
      }
      DNA change = new VersionizedDNAWrapper(orgDNA, version, true);
      ManagedObject mo = (ManagedObject) objects.get(change.getObjectID());
      mo.apply(change, txnID, includeIDs, instanceMonitor);
      if (!change.isDelta() && !txn.isPassive()) {
        // Only New objects reference are added here
        stateManager.addReference(txn.getChannelID(), mo.getID());
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
    if (!stxnID.isServerGeneratedTransacation()) {
      gtxm.applyComplete(stxnID);
      channelStats.notifyTransaction(channelID);
    }
    transactionRateCounter.increment();

    fireTransactionAppliedEvent(stxnID);
  }

  public void skipApplyAndCommit(ServerTransaction txn) {
    final ChannelID channelID = txn.getChannelID();
    final TransactionID txnID = txn.getTransactionID();
    TransactionAccount ci = getOrCreateTransactionAccount(channelID);
    if (ci.skipApplyAndCommit(txnID)) {
      acknowledge(channelID, txnID);
    }
    fireTransactionAppliedEvent(txn.getServerTransactionID());
  }

  public void commit(PersistenceTransactionProvider ptxp, Collection objects, Map newRoots,
                     Collection appliedServerTransactionIDs, Set completedTransactionIDs) {
    PersistenceTransaction ptx = ptxp.newTransaction();
    release(ptx, objects, newRoots);
    gtxm.commitAll(ptx, appliedServerTransactionIDs);
    gtxm.completeTransactions(ptx, completedTransactionIDs);
    ptx.commit();
    committed(appliedServerTransactionIDs);
  }

  private void release(PersistenceTransaction ptx, Collection objects, Map newRoots) {
    // change done so now we can release the objects
    objectManager.releaseAll(ptx, objects);

    // NOTE: important to have released all objects in the TXN before
    // calling this event as the listeners tries to lookup for the object and blocks
    for (Iterator i = newRoots.entrySet().iterator(); i.hasNext();) {
      Map.Entry entry = (Entry) i.next();
      fireRootCreatedEvent((String) entry.getKey(), (ObjectID) entry.getValue());
    }
  }

  public void incomingTransactions(ChannelID cid, Set serverTxnIDs, boolean relayed) {
    TransactionAccount ci = getOrCreateTransactionAccount(cid);
    for (Iterator i = serverTxnIDs.iterator(); i.hasNext();) {
      final ServerTransactionID txnId = (ServerTransactionID) i.next();
      final TransactionID txnID = txnId.getClientTransactionID();
      if (!relayed) {
        ci.relayTransactionComplete(txnID);
      }
    }
    fireIncomingTransactionsEvent(cid, serverTxnIDs);
  }

  public void transactionsRelayed(ChannelID channelID, Set serverTxnIDs) {
    TransactionAccount ci = getOrCreateTransactionAccount(channelID);
    if (ci == null) {
      logger.warn("transactionsRelayed(): TransactionAccount not found for " + channelID);
      return;
    }
    for (Iterator i = serverTxnIDs.iterator(); i.hasNext();) {
      final ServerTransactionID txnId = (ServerTransactionID) i.next();
      final TransactionID txnID = txnId.getClientTransactionID();
      if (ci.relayTransactionComplete(txnID)) {
        acknowledge(channelID, txnID);
      }
    }
  }

  private void committed(Collection txnsIds) {
    for (Iterator i = txnsIds.iterator(); i.hasNext();) {
      final ServerTransactionID txnId = (ServerTransactionID) i.next();
      final ChannelID waiter = txnId.getChannelID();
      final TransactionID txnID = txnId.getClientTransactionID();

      TransactionAccount ci = getTransactionAccount(waiter);
      if (ci != null && ci.applyCommitted(txnID)) {
        acknowledge(waiter, txnID);
      }
    }
  }

  public void broadcasted(ChannelID waiter, TransactionID txnID) {
    TransactionAccount ci = getTransactionAccount(waiter);

    if (ci != null && ci.broadcastCompleted(txnID)) {
      acknowledge(waiter, txnID);
    }
  }

  private TransactionAccount getOrCreateTransactionAccount(ChannelID clientID) {
    synchronized (transactionAccounts) {
      TransactionAccount ta = (TransactionAccount) transactionAccounts.get(clientID);
      if ((ta == null) || (ta instanceof NullTransactionAccount)) {
        Object old = transactionAccounts.put(clientID, (ta = new TransactionAccountImpl(clientID)));
        if (old != null) {
          logger.info("Transaction Account changed from : " + old + " to " + ta);
        }
      }
      return ta;
    }
  }

  private TransactionAccount getOrCreateNullTransactionAccount(ChannelID clientID) {
    synchronized (transactionAccounts) {
      TransactionAccount ta = (TransactionAccount) transactionAccounts.get(clientID);
      if ((ta == null) || (ta instanceof TransactionAccountImpl)) {
        Object old = transactionAccounts.put(clientID, (ta = new NullTransactionAccount(clientID)));
        if (old != null) {
          logger.info("Transaction Account changed from : " + old + " to " + ta);
        }
      }
      return ta;

    }
  }

  private TransactionAccount getTransactionAccount(ChannelID clientID) {
    return (TransactionAccount) transactionAccounts.get(clientID);
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

  private void fireIncomingTransactionsEvent(ChannelID cid, Set serverTxnIDs) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.incomingTransactions(cid, serverTxnIDs);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e);
        } else {
          logger.warn("Exception in Txn complete event callback: " + e.getMessage());
        }
      }
    }
  }

  private void fireTransactionCompleteEvent(ServerTransactionID stxID) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.transactionCompleted(stxID);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e);
        } else {
          logger.warn("Exception in Txn complete event callback: " + e.getMessage());
        }
      }
    }
  }

  private void fireTransactionAppliedEvent(ServerTransactionID stxID) {
    for (Iterator iter = txnEventListeners.iterator(); iter.hasNext();) {
      try {
        ServerTransactionListener listener = (ServerTransactionListener) iter.next();
        listener.transactionApplied(stxID);
      } catch (Exception e) {
        if (logger.isDebugEnabled()) {
          logger.debug(e);
        } else {
          logger.warn("Exception in Txn Applied event callback: " + e.getMessage());
        }
      }
    }
  }

}
