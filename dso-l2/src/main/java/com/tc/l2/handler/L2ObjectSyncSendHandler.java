/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.api.L2Coordinator;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.ObjectSyncMessage;
import com.tc.l2.msg.ObjectSyncMessageFactory;
import com.tc.l2.msg.ServerTxnAckMessage;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.l2.objectserver.ServerTransactionFactory;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ObjectSyncSendHandler extends AbstractEventHandler {

  private static final TCLogger          logger                               = TCLogging
                                                                                  .getLogger(L2ObjectSyncSendHandler.class);

  private static final boolean           TXN_ACK_THROTTLING_ENABLED           = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_ENABLED);
  private static final int               TOTAL_PENDING_TRANSACTIONS_THRESHOLD = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getInt(TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_THRESHOLD);
  private static final int               MAX_SLEEP_SECS                       = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getInt(TCPropertiesConsts.L2_TRANSACTIONMANAGER_PASSIVE_THROTTLE_MAXSLEEPSECONDS);
  private static final long              TIME_TO_THROTTLE_ON_OBJECT_SEND      = TCPropertiesImpl
                                                                                  .getProperties()
                                                                                  .getLong(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_THROTTLE_TIME);

  private final SyncLogger               syncLogger                           = new SyncLogger();

  private final ServerTransactionFactory serverTransactionFactory;
  private final L2ObjectStateManager     objectStateManager;

  private GroupManager                   groupManager;
  private ServerTransactionManager       serverTxnMgr;

  public L2ObjectSyncSendHandler(final L2ObjectStateManager objectStateManager, final ServerTransactionFactory factory) {
    this.objectStateManager = objectStateManager;
    this.serverTransactionFactory = factory;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof ManagedObjectSyncContext) {
      final ManagedObjectSyncContext mosc = (ManagedObjectSyncContext) context;
      if (sendObjects(mosc)) {
        if (mosc.hasMore()) {
          throttleOnObjectSync();
          this.objectStateManager.syncMore(mosc.getNodeID());
        }
      }
    } else if (context instanceof ServerTxnAckMessage) {
      final ServerTxnAckMessage txnMsg = (ServerTxnAckMessage) context;
      sendAcks(txnMsg);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private synchronized void throttleOnObjectSync() {
    if (TIME_TO_THROTTLE_ON_OBJECT_SEND > 0) {
      try {
        this.wait(TIME_TO_THROTTLE_ON_OBJECT_SEND);
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  private void sendAcks(final ServerTxnAckMessage ackMsg) {
    if (TXN_ACK_THROTTLING_ENABLED) {
      throttleOnTxnAck();
    }
    try {
      this.groupManager.sendTo(ackMsg.getDestinationID(), ackMsg);
    } catch (final GroupException e) {
      final String error = "ERROR sending ACKS: Caught exception while sending message to ACTIVE";
      logger.error(error, e);
      // try Zapping the active server so that a split brain war is initiated, at least we won't hold the whole cluster
      // down.
      this.groupManager.zapNode(ackMsg.getDestinationID(), L2HAZapNodeRequestProcessor.COMMUNICATION_TO_ACTIVE_ERROR,
                                error + L2HAZapNodeRequestProcessor.getErrorString(e));
    }
  }

  // A Simple way to throttle Active from Passive when the number of pending txns reaches the threshold
  private synchronized void throttleOnTxnAck() {
    int totalPendingTxns = this.serverTxnMgr.getTotalPendingTransactionsCount();
    final int factor = totalPendingTxns / TOTAL_PENDING_TRANSACTIONS_THRESHOLD;
    if (factor < 1) {
      // No Throttling
      return;
    } else if (factor >= 3) {
      // More than 3 times, Halt until less than 2.5 times of TOTAL_PENDING_TRANSACTIONS_THRESHOLD
      haltUntilLessThan((int) (TOTAL_PENDING_TRANSACTIONS_THRESHOLD * 2.5), totalPendingTxns);
    } else {
      // throttle
      int maxSecsToSleep = MAX_SLEEP_SECS * Math.min(factor, 3);
      logger.info("Throttling Transaction Acks for " + maxSecsToSleep + " secs maximum since totalPendingTxns reached "
                  + totalPendingTxns);
      // Wait until max time to sleep or until it falls below 66 % of TOTAL_PENDING_TRANSACTIONS_THRESHOLD
      while (maxSecsToSleep > 0 && totalPendingTxns > ((int) (TOTAL_PENDING_TRANSACTIONS_THRESHOLD * .66))) {
        try {
          wait(1000);
        } catch (final InterruptedException e) {
          throw new AssertionError(e);
        }
        totalPendingTxns = this.serverTxnMgr.getTotalPendingTransactionsCount();
        maxSecsToSleep--;
      }
    }
  }

  private void haltUntilLessThan(final int maxLimit, int totalPendingTxns) {

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
      } catch (final InterruptedException e) {
        throw new AssertionError(e);
      }
      totalPendingTxns = this.serverTxnMgr.getTotalPendingTransactionsCount();
    } while (maxLimit < totalPendingTxns);
    logger.info("Starting Transaction Acks as limit reached : limit = " + maxLimit + " total Pending txns = "
                + totalPendingTxns);
  }

  private boolean sendObjects(final ManagedObjectSyncContext mosc) {

    ServerTransactionID sid = ServerTransactionID.NULL_ID;
    try {
      sid = this.serverTransactionFactory.getNextServerTransactionID(this.groupManager.getLocalNodeID());
      final ObjectSyncMessage msg = ObjectSyncMessageFactory.createObjectSyncMessageFrom(mosc, sid);
      this.serverTxnMgr.objectsSynched(mosc.getNodeID(), sid);
      this.groupManager.sendTo(mosc.getNodeID(), msg);
      this.syncLogger.logSynced(mosc);
      this.objectStateManager.close(mosc);
      return true;
    } catch (final GroupException e) {
      this.serverTxnMgr.acknowledgement(sid.getSourceID(), sid.getClientTransactionID(), mosc.getNodeID());
      logger.error("Removing " + mosc.getNodeID() + " from group because of Exception :", e);
      this.groupManager.zapNode(mosc.getNodeID(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending objects." + L2HAZapNodeRequestProcessor.getErrorString(e));
      return false;
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.serverTxnMgr = oscc.getTransactionManager();
    final L2Coordinator l2Coordinator = oscc.getL2Coordinator();
    this.groupManager = l2Coordinator.getGroupManager();
  }

  private static class SyncLogger {

    public void logSynced(final ManagedObjectSyncContext mosc) {
      final int last = mosc.getTotalObjectsSynced();
      final int current = last + mosc.getSynchedOids().size();
      final int totalObjectsToSync = mosc.getTotalObjectsToSync();
      final int lastPercent = (int) ((last * 100L) / totalObjectsToSync);
      final int currentPercent = (int) ((current * 100L) / totalObjectsToSync);

      if (currentPercent > lastPercent) {
        logger.info("Sent " + current + " (" + currentPercent + "%) objects out of " + mosc.getTotalObjectsToSync()
                    + " to " + mosc.getNodeID()
                    + (mosc.getRootsMap().size() == 0 ? "" : " roots = " + mosc.getRootsMap().size()));
      }
    }
  }
}
