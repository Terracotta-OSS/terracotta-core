/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ObjectSyncCompleteAckMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessageFactory;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerRelayedTxnAckMessage;
import com.tc.l2.msg.ServerSyncTxnAckMessage;
import com.tc.l2.msg.ServerTxnAckMessageFactory;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private static final TCLogger          logger = TCLogging.getLogger(L2ObjectSyncHandler.class);
  private TransactionBatchReaderFactory  batchReaderFactory;

  private Sink                           sendSink;
  private ReplicatedTransactionManager   rTxnManager;
  private StateSyncManager               stateSyncManager;
  private GroupManager                   groupManager;

  private final ServerTransactionFactory serverTransactionFactory;

  public L2ObjectSyncHandler(final ServerTransactionFactory factory) {
    this.serverTransactionFactory = factory;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ObjectSyncMessage) {
      final ObjectSyncMessage syncMsg = (ObjectSyncMessage) context;
      doSyncObjectsResponse(syncMsg);
    } else if (context instanceof RelayedCommitTransactionMessage) {
      final RelayedCommitTransactionMessage commitMessage = (RelayedCommitTransactionMessage) context;
      final Set serverTxnIDs = processCommitTransactionMessage(commitMessage);
      processTransactionLowWaterMark(commitMessage.getLowGlobalTransactionIDWatermark());
      ackRelayedTransactions(commitMessage, serverTxnIDs);
    } else if (context instanceof ObjectSyncCompleteMessage) {
      handleObjectSyncCompleteMessage((ObjectSyncCompleteMessage) context);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private void handleObjectSyncCompleteMessage(ObjectSyncCompleteMessage context) {
    final ObjectSyncCompleteMessage msg = context;
    logger.info("Received ObjectSyncComplete Msg from : " + msg.messageFrom() + " msg : " + msg);
    stateSyncManager.objectSyncComplete();
    ObjectSyncCompleteAckMessage ackMessage = ObjectSyncCompleteMessageFactory.createObjectSyncCompleteAckMessage(msg
        .messageFrom());
    sendObjectSyncCompleteAckMessage(ackMessage);
  }

  private void sendObjectSyncCompleteAckMessage(ObjectSyncCompleteAckMessage message) {
    try {
      this.groupManager.sendTo(message.getDestinationNodeID(), message);
    } catch (final GroupException e) {
      final String error = "Error sending ObjectSyncCompleteAckMessage to " + message.getDestinationNodeID()
                           + " Caught exception while sending message to ACTIVE";
      logger.error(error, e);
      this.groupManager.zapNode(message.getDestinationNodeID(),
                                L2HAZapNodeRequestProcessor.COMMUNICATION_TO_ACTIVE_ERROR,
                                error + L2HAZapNodeRequestProcessor.getErrorString(e));
    }

  }

  private void processTransactionLowWaterMark(final GlobalTransactionID lowGlobalTransactionIDWatermark) {
    // TODO:: This processing could be handled by another stage thread.
    this.rTxnManager.clearTransactionsBelowLowWaterMark(lowGlobalTransactionIDWatermark);
  }

  private void ackRelayedTransactions(final AbstractGroupMessage messageFrom, final Set serverTxnIDs) {
    final ServerRelayedTxnAckMessage msg = ServerTxnAckMessageFactory.createServerRelayedTxnAckMessage(messageFrom,
                                                                                                       serverTxnIDs);
    this.sendSink.add(msg);
  }

  private void ackSyncTransactions(final AbstractGroupMessage messageFrom, final Set serverTxnIDs) {
    final ServerSyncTxnAckMessage msg = ServerTxnAckMessageFactory.createServerSyncTxnAckMessage(messageFrom,
                                                                                                 serverTxnIDs);
    this.sendSink.add(msg);
  }

  private Set processCommitTransactionMessage(final RelayedCommitTransactionMessage commitMessage) {
    try {
      final TransactionBatchReader reader = this.batchReaderFactory.newTransactionBatchReader(commitMessage);
      ServerTransaction txn;
      // XXX:: Order has to be maintained.
      final Map txns = new LinkedHashMap(reader.getNumberForTxns());
      while ((txn = reader.getNextTransaction()) != null) {
        txn.setGlobalTransactionID(commitMessage.getGlobalTransactionIDFor(txn.getServerTransactionID()));
        txns.put(txn.getServerTransactionID(), txn);
      }
      this.rTxnManager.addCommitedTransactions(reader.getNodeID(), txns.keySet(), txns.values(), commitMessage);
      return txns.keySet();
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
  }

  private void doSyncObjectsResponse(final ObjectSyncMessage syncMsg) {
    final ServerTransaction txn = this.serverTransactionFactory.createTxnFrom(syncMsg);
    this.rTxnManager.addObjectSyncTransaction(txn);
    final HashSet serverTxnIDs = new HashSet(2);
    serverTxnIDs.add(txn.getServerTransactionID());
    ackSyncTransactions(syncMsg, serverTxnIDs);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.batchReaderFactory = oscc.getTransactionBatchReaderFactory();
    this.rTxnManager = oscc.getL2Coordinator().getReplicatedTransactionManager();
    this.stateSyncManager = oscc.getL2Coordinator().getStateSyncManager();
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_SEND_STAGE).getSink();
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
  }

}
