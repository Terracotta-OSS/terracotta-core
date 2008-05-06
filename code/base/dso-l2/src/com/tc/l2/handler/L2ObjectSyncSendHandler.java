/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.ObjectSyncMessageFactory;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesImpl;
import com.tc.properties.TCPropertiesConsts;

public class L2ObjectSyncSendHandler extends AbstractEventHandler {

  private static final TCLogger      logger                               = TCLogging
                                                                              .getLogger(L2ObjectSyncSendHandler.class);

  private static final boolean       TXN_ACK_THROTTLING_ENABLED           = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getBoolean(
                                                                                          TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_ENABLED);
  private static final int           TOTAL_PENDING_TRANSACTIONS_THRESHOLD = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getInt(
                                                                                      TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_THRESHOLD);
  private static final int           MAX_SLEEP_SECS                       = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getInt(
                                                                                      TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_MAXSLEEPSECONDS);
  private static final long          TIME_TO_THROTTLE_ON_OBJECT_SEND      = TCPropertiesImpl
                                                                              .getProperties()
                                                                              .getLong(
                                                                                       TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_TIME);

  private final L2ObjectStateManager objectStateManager;
  private GroupManager               groupManager;

  private Sink                       syncRequestSink;

  private ServerTransactionManager   serverTxnMgr;

  public L2ObjectSyncSendHandler(L2ObjectStateManager objectStateManager) {
    this.objectStateManager = objectStateManager;
  }

  public void handleEvent(EventContext context) {
    if (context instanceof ManagedObjectSyncContext) {
      ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
      if (sendObjects(mosc)) {
        if (mosc.hasMore()) {
          throttleOnObjectSync();
          syncRequestSink.add(new SyncObjectsRequest(mosc.getNodeID()));
        }
      }
    } else if (context instanceof ServerTxnAckMessage) {
      ServerTxnAckMessage txnMsg = (ServerTxnAckMessage) context;
      sendAcks(txnMsg);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private synchronized void throttleOnObjectSync() {
    if (TIME_TO_THROTTLE_ON_OBJECT_SEND > 0) {
      try {
        this.wait(TIME_TO_THROTTLE_ON_OBJECT_SEND);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  private void sendAcks(ServerTxnAckMessage ackMsg) {
    if (TXN_ACK_THROTTLING_ENABLED) throttleOnTxnAck();
    try {
      this.groupManager.sendTo(ackMsg.getDestinationID(), ackMsg);
    } catch (GroupException e) {
      String error = "ERROR sending ACKS: Caught exception while sending message to ACTIVE";
      logger.error(error, e);
      // try Zapping the active server so that a split brain war is initiated, at least we won't hold the whole cluster
      // down.
      groupManager.zapNode(ackMsg.getDestinationID(), L2HAZapNodeRequestProcessor.COMMUNICATION_TO_ACTIVE_ERROR,
                           error + L2HAZapNodeRequestProcessor.getErrorString(e));
    }
  }

  // A Simple way to throttle Active from Passive when the number of pending txns reaches the threshold
  private synchronized void throttleOnTxnAck() {
    int totalPendingTxns = serverTxnMgr.getTotalPendingTransactionsCount();
    int factor = totalPendingTxns / TOTAL_PENDING_TRANSACTIONS_THRESHOLD;
    if (factor < 1) {
      // No Throttling
      return;
    } else if (factor >= 3) {
      // Halt
      haltUntilLessThan(TOTAL_PENDING_TRANSACTIONS_THRESHOLD * 3, totalPendingTxns);
    } else {
      // throttle
      int maxSecsToSleep = MAX_SLEEP_SECS * Math.min(factor, 3);
      logger.info("Throttling Transaction Acks for " + maxSecsToSleep + " secs maximum since totalPendingTxns reached "
                  + totalPendingTxns);
      while (maxSecsToSleep > 0 && totalPendingTxns > TOTAL_PENDING_TRANSACTIONS_THRESHOLD) {
        try {
          wait(1000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        totalPendingTxns = serverTxnMgr.getTotalPendingTransactionsCount();
        maxSecsToSleep--;
      }
    }
  }

  private void haltUntilLessThan(int maxLimit, int totalPendingTxns) {

    logger.info("Halting Transaction Acks as limit exceeded : limit = " + maxLimit + " total Pending txns = "
                + totalPendingTxns);
    int count = 0;
    do {
      if (count++ % 30 == 0) {
        logger.info("Still Waiting for pending txns to reach limit. limit " + maxLimit + " total Pending txns = "
                    + totalPendingTxns);
      }
      try {
        wait(1000);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      totalPendingTxns = serverTxnMgr.getTotalPendingTransactionsCount();
    } while (maxLimit < totalPendingTxns);
    logger.info("Starting Transaction Acks as limit reached : limit = " + maxLimit + " total Pending txns = "
                + totalPendingTxns);
  }

  private boolean sendObjects(ManagedObjectSyncContext mosc) {
    ObjectSyncMessage msg = ObjectSyncMessageFactory.createObjectSyncMessageFrom(mosc);
    try {
      this.groupManager.sendTo(mosc.getNodeID(), msg);
      logger.info("Sent " + mosc.getTotalObjectsSynced() + " objects out of " + mosc.getTotalObjectsToSync() + " to "
                  + mosc.getNodeID() + (mosc.getRootsMap().size() == 0 ? "" : " roots = " + mosc.getRootsMap().size()));
      objectStateManager.close(mosc);
      return true;
    } catch (GroupException e) {
      logger.error("Removing " + mosc.getNodeID() + " from group because of Exception :", e);
      groupManager.zapNode(mosc.getNodeID(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                           "Error sending objects." + L2HAZapNodeRequestProcessor.getErrorString(e));
      return false;
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverTxnMgr = oscc.getTransactionManager();
    L2Coordinator l2Coordinator = oscc.getL2Coordinator();
    this.groupManager = l2Coordinator.getGroupManager();
    this.syncRequestSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_REQUEST_STAGE).getSink();
  }

}
