/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.msg.ServerTxnAckMessageFactory;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private TransactionBatchReaderFactory batchReaderFactory;

  private Sink                          sendSink;
  private ReplicatedTransactionManager  rTxnManager;

  public void handleEvent(EventContext context) {
    if (context instanceof ObjectSyncMessage) {
      ObjectSyncMessage syncMsg = (ObjectSyncMessage) context;
      doSyncObjectsResponse(syncMsg);
    } else if (context instanceof RelayedCommitTransactionMessage) {
      RelayedCommitTransactionMessage commitMessage = (RelayedCommitTransactionMessage) context;
      Set serverTxnIDs = processCommitTransactionMessage(commitMessage);
      ackTransactions(commitMessage, serverTxnIDs);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  // TODO:: Implement throttling between active/passive
  private void ackTransactions(RelayedCommitTransactionMessage commitMessage, Set serverTxnIDs) {
    ServerTxnAckMessage msg = ServerTxnAckMessageFactory.createServerTxnAckMessage(commitMessage, serverTxnIDs);
    sendSink.add(msg);
  }

  // TODO::recycle msg after use
  private Set processCommitTransactionMessage(RelayedCommitTransactionMessage commitMessage) {
    try {
      final TransactionBatchReader reader = batchReaderFactory.newTransactionBatchReader(commitMessage);
      ServerTransaction txn;
      // XXX:: Order has to be maintained.
      Map txns = new LinkedHashMap(reader.getNumTxns());
      while ((txn = reader.getNextTransaction()) != null) {
        txns.put(txn.getServerTransactionID(), txn);
      }
      rTxnManager.addCommitTransactionMessage(reader.getChannelID(), txns.keySet(), txns.values(), reader
          .addAcknowledgedTransactionIDsTo(new HashSet()));
      return txns.keySet();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  private void doSyncObjectsResponse(ObjectSyncMessage syncMsg) {
    ServerTransaction txn = ServerTransactionFactory.createTxnFrom(syncMsg);
    rTxnManager.addObjectSyncTransaction(txn);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    this.rTxnManager = oscc.getL2Coordinator().getReplicatedTransactionManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
  }

}
