/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
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
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ReplicatedTransactionManagerImpl implements ReplicatedTransactionManager, GroupMessageListener {

  private static final TCLogger                        logger              = TCLogging
                                                                               .getLogger(ReplicatedTransactionManagerImpl.class);

  private final ServerTransactionManager               transactionManager;
  private final GroupManager                           groupManager;
  private final OrderedSink                            objectsSyncSink;

  private PassiveTransactionManager                    delegate;

  private final L2ObjectSyncAckManager                 objectSyncAckManager;
  private final PassiveUninitializedTransactionManager passiveUninitTxnMgr = new PassiveUninitializedTransactionManager();
  private final PassiveStandbyTransactionManager       passiveStdByTxnMgr  = new PassiveStandbyTransactionManager();
  private final NullPassiveTransactionManager          activeTxnMgr        = new NullPassiveTransactionManager();

  private final ServerGlobalTransactionManager         gtxm;

  private final MessageRecycler                        recycler;

  public ReplicatedTransactionManagerImpl(GroupManager groupManager, OrderedSink objectsSyncSink,
                                          ServerTransactionManager transactionManager,
                                          ServerGlobalTransactionManager gtxm, MessageRecycler recycler,
                                          L2ObjectSyncAckManager objectSyncAckManager) {
    this.groupManager = groupManager;
    this.objectsSyncSink = objectsSyncSink;
    this.transactionManager = transactionManager;
    this.gtxm = gtxm;
    this.recycler = recycler;
    groupManager.registerForMessages(ObjectSyncResetMessage.class, this);
    this.delegate = passiveUninitTxnMgr;
    this.objectSyncAckManager = objectSyncAckManager;
  }

  @Override
  public synchronized void init(Set knownObjectIDs) {
    if (delegate == passiveUninitTxnMgr) {
      passiveUninitTxnMgr.addKnownObjectIDs(knownObjectIDs);
    } else {
      logger.info("Not initing with known Ids since not in UNINITIALIZED state : " + knownObjectIDs.size());
    }
  }

  @Override
  public synchronized void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
    delegate.clearTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
  }

  @Override
  public synchronized void addCommittedTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns,
                                                    Recyclable message) {
    delegate.addCommittedTransactions(nodeID, txns, message);
  }

  @Override
  public synchronized void addObjectSyncTransaction(ServerTransaction txn, final Set<ObjectID> deletedObjects) {
    delegate.addObjectSyncTransaction(txn, deletedObjects);
  }

  @Override
  public void messageReceived(final NodeID fromNode, GroupMessage msg) {
    ObjectSyncResetMessage osr = (ObjectSyncResetMessage) msg;
    Assert.assertTrue(osr.getType() == ObjectSyncResetMessage.REQUEST_RESET);
    objectsSyncSink.setAddPredicate(new AddPredicate() {
      @Override
      public boolean accept(EventContext context) {
        GroupMessage gp = (GroupMessage) context;
        return fromNode.equals(gp.messageFrom());
      }
    });
    objectsSyncSink.clear();
    objectSyncAckManager.reset();
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

  @Override
  public void publishResetRequest(NodeID nodeID) throws GroupException {
    ObjectSyncResetMessage osr = (ObjectSyncResetMessage) groupManager
        .sendToAndWaitForResponse(nodeID, ObjectSyncResetMessageFactory.createObjectSyncResetRequestMessage());
    validateResponse(nodeID, osr);
  }

  @Override
  public synchronized void l2StateChanged(StateChangedEvent sce) {
    if (sce.getCurrentState().equals(StateManager.ACTIVE_COORDINATOR)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = activeTxnMgr;
    } else if (sce.getCurrentState().equals(StateManager.PASSIVE_STANDBY)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = passiveStdByTxnMgr;
    }
  }

  private void addIncomingTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns) {
    transactionManager.incomingTransactions(nodeID, txns);
  }

  @Override
  public int pendingTransactions() {
    return delegate.pendingTransactions();
  }

  private static final class NullPassiveTransactionManager implements PassiveTransactionManager {

    @Override
    public void addCommittedTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns,
                                         Recyclable message) {
      // There could still be some messages in the queue that arrives after the node becomes ACTIVE
      logger.warn("NullPassiveTransactionManager :: Ignoring commit Txn Messages from " + nodeID);
    }

    @Override
    public void addObjectSyncTransaction(ServerTransaction txn, final Set<ObjectID> deletedObjects) {
      throw new AssertionError("Recd. ObjectSyncTransaction while in ACTIVE state : " + txn);
    }

    @Override
    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      // There could still be some messages in the queue that arrives after the node becomes ACTIVE
      logger.warn("Ignoring LowWaterMark recd. while in ACTIVE state : " + lowGlobalTransactionIDWatermark);
    }

    @Override
    public int pendingTransactions() {
      return 0;
    }
  }

  private final class PassiveStandbyTransactionManager implements PassiveTransactionManager {

    @Override
    public void addCommittedTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns,
                                         Recyclable message) {
      recycler.addMessage(message, txns.keySet());
      addIncomingTransactions(nodeID, txns);
    }

    @Override
    public void addObjectSyncTransaction(ServerTransaction txn, final Set<ObjectID> deletedObjects) {
      // XXX::NOTE:: This is possible when there are 2 or more passive servers in standby and when the active crashes.
      // One of them will become passive and it is possible that the one became active has some objects that is missing
      // from the other guy. So the current active is going to think that the other guy is in passive uninitialized
      // state and send those objects. This can be ignored as long as all commit transactions are replayed.
      logger
          .warn("PassiveStandbyTransactionManager :: Ignoring ObjectSyncTxn Messages since already in PASSIVE-STANDBY "
                + txn);
      objectSyncAckManager.ackObjectSyncTxn(txn.getServerTransactionID());
    }

    @Override
    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      gtxm.clearCommitedTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
    }

    @Override
    public int pendingTransactions() {
      return transactionManager.getTotalPendingTransactionsCount();
    }
  }

  private final class PassiveUninitializedTransactionManager implements PassiveTransactionManager {

    ObjectIDSet           existingOIDs = new ObjectIDSet();
    PendingChangesAccount pca          = new PendingChangesAccount();

    // NOTE::XXX:: Messages are not Recylced in Passive Uninitialized state because of complicated pruning
    // code. Messages may have to live longer than Txn acks.
    @Override
    public void addCommittedTransactions(NodeID nodeID, Map<ServerTransactionID, ServerTransaction> txns,
                                         Recyclable message) {
      LinkedHashMap<ServerTransactionID, ServerTransaction> prunedTransactionsMap = pruneTransactions(txns.values());
      addIncomingTransactions(nodeID, prunedTransactionsMap);
    }

    @Override
    public void clearTransactionsBelowLowWaterMark(GlobalTransactionID lowGlobalTransactionIDWatermark) {
      // We don't want to do this anymore. The only way to get relayed metadata in current impl is to keep committed
      // txns in pending account
      // until they are needed - which shouldn't be a long time, i.e. until all the maps have been synced over with
      // object sync
      // pca.clearTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
      gtxm.clearCommitedTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
    }

    // TODO::Recycle msg after use. Messages may have to live longer than Txn acks.
    private LinkedHashMap<ServerTransactionID, ServerTransaction> pruneTransactions(Collection<ServerTransaction> txns) {
      LinkedHashMap<ServerTransactionID, ServerTransaction> m = new LinkedHashMap<ServerTransactionID, ServerTransaction>();

      for (ServerTransaction st : txns) {
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
        existingOIDs = new ObjectIDSet(knownObjectIDs); // This is optimized for ObjectIDSet2
        existingOIDs.addAll(old);
      } else {
        existingOIDs.addAll(knownObjectIDs);
      }
    }

    @Override
    public void addObjectSyncTransaction(ServerTransaction txn, final Set<ObjectID> deletedObjects) {
      clearPendingChangesFor(deletedObjects);
      ServerTransaction newTxn = createCompoundTransactionFrom(txn);
      if (newTxn != null) {
        addIncomingTransactions(txn.getSourceID(), Collections.singletonMap(newTxn.getServerTransactionID(), newTxn));
      } else {
        logger
            .warn("Not adding Txn " + txn.getServerTransactionID() + " to queue since all changes have been ignored.");
        objectSyncAckManager.ackObjectSyncTxn(txn.getServerTransactionID());
      }
    }

    private void clearPendingChangesFor(Set<ObjectID> oids) {
      for (ObjectID oid : oids) {
        pca.getAnyPendingChangesForAndClear(oid);
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
        // Now add if there are more changes pending
        List moreChanges = pca.getAnyPendingChangesForAndClear(oid);
        if (existingOIDs.add(oid)) {
          compoundChanges.add(dna);
          oids.add(dna.getObjectID());
          long lastVersion = Long.MIN_VALUE;
          for (Iterator j = moreChanges.iterator(); j.hasNext();) {
            PendingRecord pr = (PendingRecord) j.next();
            long version = pr.getGlobalTransactionID().toLong();
            // XXX:: This should be true since we maintain the order in the List.
            if (lastVersion >= version) {
              logger.error("Change versions received out of order. lastVersion=" + lastVersion + " version=" + version
                           + ". Changes " + moreChanges);
              Assert.assertTrue("lastVersion=" + lastVersion + " version=" + version, lastVersion < version);
            }
            compoundChanges.add(new VersionizedDNAWrapper(pr.getChange(), version));

            lastVersion = version;
            modified = true;
          }
        } else {
          // XXX::Note:: This is a possible condition in the 3'rd PASSIVE which is initializing when the ACTIVE crashes.
          // The new ACTIVE might resend the same Object, but we don't want to pass it thru since technically if we
          // didn't miss any client transaction, we will have the same state. Also if the object sync transaction from
          // the new passive arrives before the transaction containing a change to this object, then we might apply full
          // DNA on old object. Coming to think of it, it might happen even in 1 PASSIVE case, since the ACTIVE computes
          // the diff only after sending a few txns, which might contain some new objects.
          if (logger.isDebugEnabled()) {
            logger.debug("Ignoring ObjectSyncTransaction for " + oid + " dna = " + dna + " since its already present");
          }
          modified = true;
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

    @Override
    public int pendingTransactions() {
      return transactionManager.getTotalPendingTransactionsCount() + pca.size();
    }
  }

  private static final class PendingChangesAccount {

    private final ListMultimap<ObjectID, PendingRecord> oid2Changes = LinkedListMultimap.create();
    private final Multimap<GlobalTransactionID, PendingRecord> gid2Changes = HashMultimap.create();

    void addToPending(ServerTransaction st, DNA dna) {
      PendingRecord pr = new PendingRecord(st.getServerTransactionID(), dna, st.getGlobalTransactionID());
      ObjectID oid = dna.getObjectID();
      oid2Changes.put(oid, pr);
      gid2Changes.put(st.getGlobalTransactionID(), pr);
    }

    void clear() {
      oid2Changes.clear();
      gid2Changes.clear();
    }

    List<PendingRecord> getAnyPendingChangesForAndClear(ObjectID oid) {
      List<PendingRecord> pendingChangesForOid = oid2Changes.removeAll(oid);
      for (PendingRecord pr : pendingChangesForOid) {
        gid2Changes.remove(pr.getGlobalTransactionID(), pr);
      }
      return pendingChangesForOid;
    }

    int size() {
      return gid2Changes.keySet().size();
    }
  }

  private static final class PendingRecord {

    private final ServerTransactionID serverTransactionID;
    private final DNA                 dna;
    private final GlobalTransactionID gid;

    public PendingRecord(final ServerTransactionID serverTransactionID, DNA dna, GlobalTransactionID gid) {
      this.serverTransactionID = serverTransactionID;
      this.dna = dna;
      this.gid = gid;
    }

    public DNA getChange() {
      return this.dna;
    }

    public GlobalTransactionID getGlobalTransactionID() {
      return this.gid;
    }

    @Override
    public String toString() {
      return String.format("DNA: %s, GID: %s, SID: %s", dna, gid, serverTransactionID);
    }
  }

}
