/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.impl.OrderedSink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ObjectSyncResetMessage;
import com.tc.l2.msg.ObjectSyncResetMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.lang.Recyclable;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.MessageRecycler;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import gnu.trove.TLinkable;
import gnu.trove.TLinkedList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class ReplicatedTransactionManagerImpl implements ReplicatedTransactionManager, GroupMessageListener {

  private static final TCLogger                        logger              = TCLogging
                                                                               .getLogger(ReplicatedTransactionManagerImpl.class);

  private final ServerTransactionManager               transactionManager;
  private final GroupManager                           groupManager;
  private final OrderedSink                            objectsSyncSink;

  private PassiveTransactionManager                    delegate;

  private final PassiveUninitializedTransactionManager passiveUninitTxnMgr = new PassiveUninitializedTransactionManager();
  private final PassiveStandbyTransactionManager       passiveStdByTxnMgr  = new PassiveStandbyTransactionManager();
  private final NullPassiveTransactionManager          activeTxnMgr        = new NullPassiveTransactionManager();

  private final ServerGlobalTransactionManager         gtxm;

  private final MessageRecycler                        recycler;

  public ReplicatedTransactionManagerImpl(GroupManager groupManager, OrderedSink objectsSyncSink,
                                          ServerTransactionManager transactionManager,
                                          ServerGlobalTransactionManager gtxm, MessageRecycler recycler) {
    this.groupManager = groupManager;
    this.objectsSyncSink = objectsSyncSink;
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.recycler = recycler;
    groupManager.registerForMessages(ObjectSyncResetMessage.class, this);
    this.delegate = passiveUninitTxnMgr;
  }

  public synchronized void init(Set knownObjectIDs) {
    if (delegate == passiveUninitTxnMgr) {
      passiveUninitTxnMgr.addKnownObjectIDs(knownObjectIDs);
    } else {
      logger.info("Not initing with known Ids since not in UNINITIALIED state : " + knownObjectIDs.size());
    }
  }

  public synchronized void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    delegate.clearTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
  }

  public synchronized void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message) {
    delegate.addCommitedTransactions(nodeID, txnIDs, txns, message);
  }

  public synchronized void addObjectSyncTransaction(ServerTransaction txn) {
    delegate.addObjectSyncTransaction(txn);
  }

  public void messageReceived(final NodeID fromNode, GroupMessage msg) {
    ObjectSyncResetMessage osr = (ObjectSyncResetMessage) msg;
    Assert.assertTrue(osr.getType() == ObjectSyncResetMessage.REQUEST_RESET);
    objectsSyncSink.setAddPredicate(new AddPredicate() {
      public boolean accept(EventContext context) {
        GroupMessage gp = (GroupMessage) context;
        return fromNode.equals(gp.messageFrom());
      }
    });
    objectsSyncSink.clear();
    sendOKResponse(fromNode, osr);
  }

  private void validateResponse(NodeID nodeID, ObjectSyncResetMessage msg) {
    if (msg == null || msg.getType() != ObjectSyncResetMessage.OPERATION_SUCCESS) {
      String error = "Recd wrong response from : " + nodeID + " : msg = " + msg
                     + " while requesting reset: Killing the node";
      logger.error(error);
      groupManager.zapNode(nodeID, L2HAZapNodeRequestProcessor.PROGRAM_ERROR,
                           error + L2HAZapNodeRequestProcessor.getErrorString(new Throwable()));
    }
  }

  private void sendOKResponse(NodeID fromNode, ObjectSyncResetMessage msg) {
    try {
      groupManager.sendTo(fromNode, ObjectSyncResetMessageFactory.createOKResponse(msg));
    } catch (GroupException e) {
      logger.error("Error handling message : " + msg, e);
    }
  }

  public void publishResetRequest(NodeID nodeID) throws GroupException {
    ObjectSyncResetMessage osr = (ObjectSyncResetMessage) groupManager
        .sendToAndWaitForResponse(nodeID, ObjectSyncResetMessageFactory.createObjectSyncResetRequestMessage());
    validateResponse(nodeID, osr);
  }

  public synchronized void l2StateChanged(StateChangedEvent sce) {
    if (sce.getCurrentState().equals(StateManager.ACTIVE_COORDINATOR)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = activeTxnMgr;
    } else if (sce.getCurrentState().equals(StateManager.PASSIVE_STANDBY)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = passiveStdByTxnMgr;
    }
  }

  private void addIncommingTransactions(NodeID nodeID, Set txnIDs, Collection txns) {
    transactionManager.incomingTransactions(nodeID, txnIDs, txns, false);
  }

  private static final class NullPassiveTransactionManager implements PassiveTransactionManager {

    public void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message) {
      // There could still be some messages in the queue that arrives after the node becomes ACTIVE
      logger.warn("NullPassiveTransactionManager :: Ignoring commit Txn Messages from " + nodeID);
    }

    public void addObjectSyncTransaction(ServerTransaction txn) {
      throw new AssertionError("Recd. ObjectSyncTransaction while in ACTIVE state : " + txn);
    }

    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      // There could still be some messages in the queue that arrives after the node becomes ACTIVE
      logger.warn("Ignoring LowWaterMark recd. while in ACTIVE state : " + lowGlobalTransactionIDWatermark);
    }

  }

  private final class PassiveStandbyTransactionManager implements PassiveTransactionManager {

    public void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message) {
      recycler.addMessage(message, txnIDs);
      addIncommingTransactions(nodeID, txnIDs, txns);
    }

    public void addObjectSyncTransaction(ServerTransaction txn) {
      // XXX::NOTE:: This is possible when there are 2 or more passive servers in standby and when the active crashes.
      // One of them will become passive and it is possible that the one became active has some objects that is missing
      // from the other guy. So the current active is going to think that the other guy is in passive uninitialized
      // state and send those objects. This can be ignored as long as all commit transactions are replayed.
      logger
          .warn("PassiveStandbyTransactionManager :: Ignoring ObjectSyncTxn Messages since already in PASSIVE-STANDBY"
                + txn);
    }

    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      gtxm.clearCommitedTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
    }
  }

  private final class PassiveUninitializedTransactionManager implements PassiveTransactionManager {

    ObjectIDSet           existingOIDs = new ObjectIDSet();
    PendingChangesAccount pca          = new PendingChangesAccount();

    // NOTE::XXX:: MEssages are not REcylced in Passive Uninitialized state because of complicated pruning
    // code. Messages may have to live longer than Txn acks.
    public void addCommitedTransactions(NodeID nodeID, Set txnIDs, Collection txns, Recyclable message) {
      Assert.assertEquals(txnIDs.size(), txns.size());
      LinkedHashMap prunedTransactionsMap = pruneTransactions(txns);
      Collection prunedTxns = prunedTransactionsMap.values();
      addIncommingTransactions(nodeID, prunedTransactionsMap.keySet(), prunedTxns);
    }

    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      pca.clearTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
      gtxm.clearCommitedTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
    }

    // TODO::Recycle msg after use. Messgaes may have to live longer than Txn acks.
    private LinkedHashMap pruneTransactions(Collection txns) {
      LinkedHashMap m = new LinkedHashMap();

      for (Iterator i = txns.iterator(); i.hasNext();) {
        ServerTransaction st = (ServerTransaction) i.next();
        List changes = st.getChanges();
        List prunedChanges = new ArrayList(changes.size());
        ObjectIDSet oids = new ObjectIDSet();
        ObjectIDSet newOids = new ObjectIDSet();
        for (Iterator j = changes.iterator(); j.hasNext();) {
          DNA dna = (DNA) j.next();
          ObjectID id = dna.getObjectID();
          if (!dna.isDelta()) {
            // New Object
            if (existingOIDs.add(id)) {
              prunedChanges.add(dna);
              oids.add(id);
              newOids.add(id);
            } else {
              // XXX::Note:: We already know about this object, ACTIVE has sent it in Object Sync Transaction
              logger.warn("Ignoring New Object " + id + "in transaction " + st + " dna = " + dna
                          + " since its already present");
            }
          } else if (existingOIDs.contains(id)) {
            // Already present
            prunedChanges.add(dna);
            oids.add(id);
          } else {
            // Not present
            pca.addToPending(st, dna);
          }
        }
        if (prunedChanges.size() == changes.size()) {
          // The whole transaction could pass thru
          m.put(st.getServerTransactionID(), st);
        } else if (!prunedChanges.isEmpty()) {
          // We have pruned changes
          m.put(st.getServerTransactionID(), new PrunedServerTransaction(prunedChanges, st, oids, newOids));
        }
      }

      return m;
    }

    public void clear() {
      existingOIDs = new ObjectIDSet();
      pca.clear();
    }

    public void addKnownObjectIDs(Set knownObjectIDs) {
      if (existingOIDs.size() < knownObjectIDs.size()) {
        ObjectIDSet old = existingOIDs;
        existingOIDs = new ObjectIDSet(knownObjectIDs); // This is optimizeded for ObjectIDSet2
        existingOIDs.addAll(old);
      } else {
        existingOIDs.addAll(knownObjectIDs);
      }
    }

    public void addObjectSyncTransaction(ServerTransaction txn) {
      Map txns = new LinkedHashMap(1);
      ServerTransaction newTxn = createCompoundTransactionFrom(txn);
      if (newTxn != null) {
        txns.put(txn.getServerTransactionID(), newTxn);
        addIncommingTransactions(txn.getSourceID(), txns.keySet(), txns.values());
      } else {
        logger.warn("Not add Txn " + txn + " to queue since all changes are ignored");
      }
    }

    private ServerTransaction createCompoundTransactionFrom(ServerTransaction txn) {
      List changes = txn.getChanges();
      // XXX::NOTE:: Normally even though getChanges() returns a list, you will only find one change for each OID (Look
      // at ClientTransactionImpl) but here we break that. But hopefully no one is depending on THAT in the system.
      List compoundChanges = new ArrayList(changes.size() * 2);
      ObjectIDSet oids = new ObjectIDSet();
      boolean modified = false;
      for (Iterator i = changes.iterator(); i.hasNext();) {
        DNA dna = (DNA) i.next();
        ObjectID oid = dna.getObjectID();
        if (existingOIDs.add(oid)) {
          compoundChanges.add(dna);
          oids.add(dna.getObjectID());
          // Now add if there are more changes pending
          List moreChanges = pca.getAnyPendingChangesForAndClear(oid);
          long lastVersion = Long.MIN_VALUE;
          for (Iterator j = moreChanges.iterator(); j.hasNext();) {
            PendingRecord pr = (PendingRecord) j.next();
            long version = pr.getGlobalTransactionID().toLong();
            // XXX:: This should be true since we maintain the order in the List.
            Assert.assertTrue(lastVersion < version);
            compoundChanges.add(new VersionizedDNAWrapper(pr.getChange(), version));
            lastVersion = version;
            modified = true;
          }
        } else {
          // XXX::Note:: This is a possible condition in the 3'rd PASSIVE which is initializing when the ACTIVE crashes.
          // The new ACTIVE might resend the same Object, but we dont want to pass it thru since technically if we
          // didint miss any client transaction, we will have the same state. Also if the object sync transaction from
          // the new passive arrives before the transaction containing a change to this object, then we might apply full
          // DNA on old object. Coming to think of it, it might happen even in 1 PASSIVE case, since the ACTIVE computes
          // the diff only after sending a few txns, which migh contain some new objects.
          logger.warn("Ignoring ObjectSyncTransaction for " + oid + " dna = " + dna + " since its already present");
          modified = true;
          List moreChanges = pca.getAnyPendingChangesForAndClear(oid);
          Assert.assertTrue(moreChanges.isEmpty());
        }
      }
      if (modified) {
        // This name is little misleading
        return (compoundChanges.isEmpty() ? null : new PrunedServerTransaction(compoundChanges, txn, oids, oids));
      } else {
        return txn;
      }
    }
  }

  private static final class PendingChangesAccount {

    HashMap oid2Changes = new HashMap();
    TreeMap gid2Changes = new TreeMap();

    public void addToPending(ServerTransaction st, DNA dna) {
      PendingRecord pr = new PendingRecord(dna, st.getGlobalTransactionID());
      ObjectID oid = dna.getObjectID();
      TLinkedList pendingChangesForOid = getOrCreatePendingChangesListFor(oid);
      pendingChangesForOid.addLast(pr);
      IdentityHashMap pendingChangesForTxn = getOrCreatePendingChangesSetFor(st.getGlobalTransactionID());
      pendingChangesForTxn.put(pr, pr);
    }

    public void clear() {
      oid2Changes.clear();
      gid2Changes.clear();
    }

    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowWaterMark) {
      Map lowerThanLWM = gid2Changes.headMap(lowWaterMark);
      for (Iterator i = lowerThanLWM.values().iterator(); i.hasNext();) {
        IdentityHashMap pendingChangesForTxn = (IdentityHashMap) i.next();
        for (Iterator j = pendingChangesForTxn.keySet().iterator(); j.hasNext();) {
          PendingRecord pr = (PendingRecord) j.next();
          TLinkedList pendingChangesForOid = getPendingChangesListFor(pr.getChange().getObjectID());
          pendingChangesForOid.remove(pr);
        }
        i.remove();
      }
    }

    public List getAnyPendingChangesForAndClear(ObjectID oid) {
      List pendingChangesForOid = removePendingChangesFor(oid);
      if (pendingChangesForOid != null) {
        for (Iterator i = pendingChangesForOid.iterator(); i.hasNext();) {
          PendingRecord pr = (PendingRecord) i.next();
          IdentityHashMap pendingChangesForTxn = getPendingChangesSetFor(pr.getGlobalTransactionID());
          pendingChangesForTxn.remove(pr);
        }
        return pendingChangesForOid;
      } else {
        return Collections.EMPTY_LIST;
      }
    }

    private IdentityHashMap getPendingChangesSetFor(GlobalTransactionID gid) {
      return (IdentityHashMap) gid2Changes.get(gid);
    }

    private TLinkedList getPendingChangesListFor(ObjectID objectID) {
      return (TLinkedList) oid2Changes.get(objectID);
    }

    private TLinkedList removePendingChangesFor(ObjectID oid) {
      return (TLinkedList) oid2Changes.remove(oid);
    }

    private IdentityHashMap getOrCreatePendingChangesSetFor(GlobalTransactionID gid) {
      IdentityHashMap m = (IdentityHashMap) gid2Changes.get(gid);
      if (m == null) {
        m = new IdentityHashMap();
        gid2Changes.put(gid, m);
      }
      return m;
    }

    private TLinkedList getOrCreatePendingChangesListFor(ObjectID oid) {
      TLinkedList l = (TLinkedList) oid2Changes.get(oid);
      if (l == null) {
        l = new TLinkedList();
        oid2Changes.put(oid, l);
      }
      return l;
    }

  }

  private static final class PendingRecord implements TLinkable {

    private TLinkable                 prev;
    private TLinkable                 next;

    private final DNA                 dna;
    private final GlobalTransactionID gid;

    public PendingRecord(DNA dna, GlobalTransactionID gid) {
      this.dna = dna;
      this.gid = gid;
    }

    public DNA getChange() {
      return this.dna;
    }

    public GlobalTransactionID getGlobalTransactionID() {
      return this.gid;
    }

    public TLinkable getNext() {
      return next;
    }

    public TLinkable getPrevious() {
      return prev;
    }

    public void setNext(TLinkable n) {
      this.next = n;
    }

    public void setPrevious(TLinkable p) {
      this.prev = p;
    }

  }

}
