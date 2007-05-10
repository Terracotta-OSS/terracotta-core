/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.objectserver;

import com.tc.async.api.AddPredicate;
import com.tc.async.api.EventContext;
import com.tc.async.impl.OrderedSink;
import com.tc.l2.msg.ObjectSyncResetMessage;
import com.tc.l2.msg.ObjectSyncResetMessageFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;
import com.tc.net.groups.GroupResponse;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionalObjectManager;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ReplicatedTransactionManagerImpl implements ReplicatedTransactionManager, GroupMessageListener {

  private static final TCLogger            logger = TCLogging.getLogger(ReplicatedTransactionManagerImpl.class);

  private final ServerTransactionManager   transactionManager;
  private final TransactionalObjectManager txnObjectManager;
  private final GroupManager               groupManager;
  private final OrderedSink                objectsSyncSink;

  public ReplicatedTransactionManagerImpl(GroupManager groupManager, OrderedSink objectsSyncSink,
                                          ServerTransactionManager transactionManager,
                                          TransactionalObjectManager txnObjectManager) {
    this.groupManager = groupManager;
    this.objectsSyncSink = objectsSyncSink;
    this.transactionManager = transactionManager;
    this.txnObjectManager = txnObjectManager;
    groupManager.registerForMessages(ObjectSyncResetMessage.class, this);
  }

  public void addCommitTransactionMessage(ChannelID channelID, Set txnIDs, Collection txns, Collection completedTxnIDs) {
    transactionManager.incomingTransactions(channelID, txnIDs, txns, false);
    txnObjectManager.addTransactions(txns, completedTxnIDs);
  }

  public void addObjectSyncTransaction(ServerTransaction txn) {
    Map txns = new LinkedHashMap(1);
    txns.put(txn.getServerTransactionID(), txn);
    transactionManager.incomingTransactions(ChannelID.L2_SERVER_ID, txns.keySet(), txns.values(), false);
    txnObjectManager.addTransactions(txns.values(), Collections.EMPTY_LIST);
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

  public void goActive() {
    try {
      GroupResponse gr = groupManager.sendAllAndWaitForResponse(ObjectSyncResetMessageFactory
          .createObjectSyncResetRequestMessage());
      for (Iterator i = gr.getResponses().iterator(); i.hasNext();) {
        ObjectSyncResetMessage msg = (ObjectSyncResetMessage) i.next();
        validateResponse(msg.messageFrom(), msg);
      }
    } catch (GroupException e) {
      logger.error("Error sending reset request : ", e);
    }
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
}
