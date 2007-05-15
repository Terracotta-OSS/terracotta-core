/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.impl.OrderedSink;
import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.msg.ObjectSyncResetMessage;
import com.tc.l2.msg.ObjectSyncResetMessageFactory;
import com.tc.l2.state.StateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.Assert;
import com.tc.util.ObjectIDSet2;

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
  private final TransactionalObjectManager             txnObjectManager;
  private final GroupManager                           groupManager;
  private final OrderedSink                            objectsSyncSink;

  private volatile PassiveTransactionManager           delegate;

  private final PassiveUninitializedTransactionManager passiveUninitTxnMgr = new PassiveUninitializedTransactionManager();
  private final PassiveStandbyTransactionManager       passiveStdByTxnMgr  = new PassiveStandbyTransactionManager();
  private final NullPassiveTransactionManager          activeTxnMgr        = new NullPassiveTransactionManager();

  public ReplicatedTransactionManagerImpl(GroupManager groupManager, OrderedSink objectsSyncSink,
                                          ServerTransactionManager transactionManager,
                                          TransactionalObjectManager txnObjectManager) {
    this.groupManager = groupManager;
    this.objectsSyncSink = objectsSyncSink;
    this.transactionManager = transactionManager;
    this.txnObjectManager = txnObjectManager;
    groupManager.registerForMessages(ObjectSyncResetMessage.class, this);
    this.delegate = passiveUninitTxnMgr;
  }

  public void init(Set knownObjectIDs) {
    if (delegate == passiveUninitTxnMgr) {
      passiveUninitTxnMgr.addKnownObjectIDs(knownObjectIDs);
    } else {
      logger.info("Not initing with known Ids since not in UNINITIALIED state : " + knownObjectIDs.size());
    }
  }

  public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
    delegate.addCommitTransactionMessage(channelID, txnIDs, txns, completedTxnIDs);
  }

  public void addObjectSyncTransaction(ServerTransaction txn) {
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
      logger.error("Recd wrong response from : " + nodeID + " : msg = " + msg
                   + " while requesting reset: Killing the node");
      groupManager.zapNode(nodeID);
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

  public void l2StateChanged(StateChangedEvent sce) {
    if (sce.getCurrentState().equals(StateManager.ACTIVE_COORDINATOR)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = activeTxnMgr;
    } else if (sce.getCurrentState().equals(StateManager.PASSIVE_STANDBY)) {
      passiveUninitTxnMgr.clear(); // Release Memory
      this.delegate = passiveStdByTxnMgr;
    }
  }

  public void addIncommingTransactions(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
    transactionManager.incomingTransactions(channelID, txnIDs, txns, false);
    txnObjectManager.addTransactions(txns, completedTxnIDs);
  }

  private final class NullPassiveTransactionManager implements PassiveTransactionManager {

    public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
      // There could still be some messages in the queue that arrives after the node becomes ACTIVE
      logger.warn("NullPassiveTransactionManager :: Ignoring commit Txn Messages from " + channelID);
    }

    public void addObjectSyncTransaction(ServerTransaction txn) {
      throw new AssertionError("Recd. ObjectSyncTransaction while in ACTIVE state : " + txn);
    }
  }

  private final class PassiveStandbyTransactionManager implements PassiveTransactionManager {

    public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
      addIncommingTransactions(channelID, txnIDs, txns, completedTxnIDs);
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

  }

  private final class PassiveUninitializedTransactionManager implements PassiveTransactionManager {

    ObjectIDSet2 existingOIDs = new ObjectIDSet2();

    public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
      clearCompleted(completedTxnIDs);
      Assert.assertEquals(txnIDs.size(), txns.size());
      LinkedHashMap prunedTransactionsMap = pruneTransactions(txns);
      Collection prunedTxns = prunedTransactionsMap.values();
      addIncommingTransactions(channelID, prunedTransactionsMap.keySet(), prunedTxns, completedTxnIDs);
    }

    // TODO::Recycle msg after use. Messgaes may have to live longer than Txn acks.
    private LinkedHashMap pruneTransactions(Collection txns) {
      LinkedHashMap m = new LinkedHashMap();

      for (Iterator i = txns.iterator(); i.hasNext();) {
        ServerTransaction st = (ServerTransaction) i.next();
        List changes = st.getChanges();
        List prunedChanges = new ArrayList(changes.size());
        for (Iterator j = changes.iterator(); i.hasNext();) {
          DNA dna = (DNA) j.next();
          ObjectID id = dna.getObjectID();
          if (!dna.isDelta()) {
            // New Object
            existingOIDs.add(id);
            prunedChanges.add(dna);
          } else if (existingOIDs.contains(id)) {
            // Already present
            prunedChanges.add(dna);
          } else {
            // Not present
            addToPending(st, dna);
          }
        }
        if (prunedChanges.size() == changes.size()) {
          // The whole transaction could pass thru
          m.put(st.getServerTransactionID(), st);
        } else {
          // We have pruned changes
          m.put(st.getServerTransactionID(), new PrunedServerTransaction(prunedChanges, st));
        }
      }

      return m;
    }

    private void addToPending(ServerTransaction st, DNA dna) {
      // TODO::
    }

    private void clearCompleted(Collection completedTxnIDs) {
      // TODO
    }

    public void clear() {
      existingOIDs = new ObjectIDSet2();
    }

    public void addKnownObjectIDs(Set knownObjectIDs) {
      if (existingOIDs.size() < knownObjectIDs.size()) {
        ObjectIDSet2 old = existingOIDs;
        existingOIDs = new ObjectIDSet2(knownObjectIDs); // This is optimizeded for ObjectIDSet2
        existingOIDs.addAll(old);
      } else {
        existingOIDs.addAll(knownObjectIDs);
      }
    }

    public void addObjectSyncTransaction(ServerTransaction txn) {
      Map txns = new LinkedHashMap(1);
      txns.put(txn.getServerTransactionID(), createCompoundTransactionFrom(txn));
      addIncommingTransactions(ChannelID.L2_SERVER_ID, txns.keySet(), txns.values(), Collections.EMPTY_LIST);
    }

    private ServerTransaction createCompoundTransactionFrom(ServerTransaction txn) {
      // TODO::
      return txn;
    }

  }

}
