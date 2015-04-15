/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ObjectSyncCompleteAckMessage;
import com.tc.l2.msg.ObjectSyncCompleteMessage;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.RelayedCommitTransactionMessage;
import com.tc.l2.msg.ServerRelayedTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectSyncAckManager;
import com.tc.l2.objectserver.ReplicatedTransactionManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransaction;
import com.tc.objectserver.tx.TransactionBatchReader;
import com.tc.objectserver.tx.TransactionBatchReaderFactory;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.concurrent.Timer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class L2ObjectSyncHandler extends AbstractEventHandler {

  private static final TCLogger          logger = TCLogging.getLogger(L2ObjectSyncHandler.class);
  private static final int               LWM_UPDATE_INTERVAL = 10000;

  private TransactionBatchReaderFactory  batchReaderFactory;

  private Sink                           sendSink;
  private ReplicatedTransactionManager   rTxnManager;
  private StateSyncManager               stateSyncManager;
  private GroupManager                   groupManager;

  private volatile GlobalTransactionID   currentLWM = GlobalTransactionID.NULL_ID;

  private final ServerTransactionFactory serverTransactionFactory;
  private final L2ObjectSyncAckManager   objectSyncAckManager;

  private final TaskRunner               taskRunner;
  private Timer                          lwmUpdateTimer;

  public L2ObjectSyncHandler(final ServerTransactionFactory factory,
                             final L2ObjectSyncAckManager objectSyncAckManager,
                             final TaskRunner taskRunner) {
    this.serverTransactionFactory = factory;
    this.objectSyncAckManager = objectSyncAckManager;
    this.taskRunner = taskRunner;
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
    objectSyncAckManager.objectSyncComplete();
    ObjectSyncCompleteAckMessage ackMessage = new ObjectSyncCompleteAckMessage(msg.messageFrom());
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

  private void startLWMUpdaterIfNecessary() {
    if (lwmUpdateTimer == null) {
      lwmUpdateTimer = taskRunner.newTimer("LWM updater");
      lwmUpdateTimer.scheduleAtFixedRate(new Runnable() {
        // thread-confined variable - does not require synchronization
        private GlobalTransactionID lastUpdate = GlobalTransactionID.NULL_ID;
        @Override
        public void run() {
          if (!lastUpdate.equals(currentLWM)) {
            lastUpdate = currentLWM;
            rTxnManager.clearTransactionsBelowLowWaterMark(currentLWM);
          }
        }
      }, LWM_UPDATE_INTERVAL, LWM_UPDATE_INTERVAL, TimeUnit.MILLISECONDS);
    }
  }

  private void processTransactionLowWaterMark(final GlobalTransactionID lowGlobalTransactionIDWatermark) {
    currentLWM = lowGlobalTransactionIDWatermark;
    startLWMUpdaterIfNecessary();
  }

  private void ackRelayedTransactions(final RelayedCommitTransactionMessage messageFrom, final Set serverTxnIDs) {
    final ServerRelayedTxnAckMessage msg = new ServerRelayedTxnAckMessage(messageFrom, serverTxnIDs);
    this.sendSink.add(msg);
  }

  private Set processCommitTransactionMessage(final RelayedCommitTransactionMessage commitMessage) {
    try {
      final TransactionBatchReader reader = this.batchReaderFactory.newTransactionBatchReader(commitMessage);
      ServerTransaction txn;
      // XXX:: Order has to be maintained.
      final Map<ServerTransactionID, ServerTransaction> txns = new LinkedHashMap<ServerTransactionID, ServerTransaction>(
                                                                                                                         reader
                                                                                                                             .getNumberForTxns());
      while ((txn = reader.getNextTransaction()) != null) {
        txn.setGlobalTransactionID(commitMessage.getGlobalTransactionIDFor(txn.getServerTransactionID()));
        txns.put(txn.getServerTransactionID(), txn);
      }
      this.rTxnManager.addCommittedTransactions(reader.getNodeID(), txns, commitMessage);
      return txns.keySet();
    } catch (final Exception e) {
      throw new AssertionError(e);
    }
  }

  private void doSyncObjectsResponse(final ObjectSyncMessage syncMsg) {
    // The object sync message must be registered with the objectSyncAckManager prior to adding it to rTxnManager to
    // avoid a race with completing the transaction
    this.objectSyncAckManager.addObjectSyncMessageToAck(syncMsg.getServerTransactionID(), syncMsg.getMessageID());
    final ServerTransaction txn = this.serverTransactionFactory.createTxnFrom(syncMsg);
    this.rTxnManager.addObjectSyncTransaction(txn, syncMsg.getDeletedOids());
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
