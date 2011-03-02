/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.l2.ha.L2HAZapNodeRequestProcessor;
import com.tc.l2.msg.IndexSyncCompleteAckMessage;
import com.tc.l2.msg.IndexSyncCompleteMessage;
import com.tc.l2.msg.IndexSyncMessage;
import com.tc.l2.msg.IndexSyncMessageFactory;
import com.tc.l2.msg.IndexSyncStartMessage;
import com.tc.l2.state.StateSyncManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.groups.GroupException;
import com.tc.net.groups.GroupManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.search.IndexHACoordinator;

public class L2IndexSyncHandler extends AbstractEventHandler {

  private static final TCLogger    logger = TCLogging.getLogger(L2IndexSyncHandler.class);
  private final IndexHACoordinator indexHACoordinator;

  private StateSyncManager         stateSyncManager;
  private GroupManager             groupManager;

  public L2IndexSyncHandler(final IndexHACoordinator indexHACoordinator) {
    this.indexHACoordinator = indexHACoordinator;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof IndexSyncStartMessage) {
      doSyncPrepare();
    } else if (context instanceof IndexSyncMessage) {
      final IndexSyncMessage syncMsg = (IndexSyncMessage) context;
      doSyncIndex(syncMsg);
    } else if (context instanceof IndexSyncCompleteMessage) {
      handleIndexSyncCompleteMessage((IndexSyncCompleteMessage) context);
    } else {
      throw new AssertionError("Unknown context type : " + context.getClass().getName() + " : " + context);
    }
  }

  private void handleIndexSyncCompleteMessage(IndexSyncCompleteMessage context) {
    final IndexSyncCompleteMessage msg = context;
    logger.info("Received IndexSyncCompleteMessage Msg from : " + msg.messageFrom());
    stateSyncManager.indexSyncComplete();
    IndexSyncCompleteAckMessage ackMessage = IndexSyncMessageFactory.createIndexSyncCompleteAckMessage(msg
        .messageFrom());
    sendIndexSyncCompleteAckMessage(ackMessage);
  }

  private void sendIndexSyncCompleteAckMessage(IndexSyncCompleteAckMessage message) {
    try {
      this.groupManager.sendTo(message.getDestinatioinNodeID(), message);
    } catch (final GroupException e) {
      final String error = "Error sending IndexSyncCompleteAckMessage to " + message.getDestinatioinNodeID()
                           + ": Caught exception while sending message to ACTIVE";
      logger.error(error, e);
      this.groupManager.zapNode(message.getDestinatioinNodeID(), L2HAZapNodeRequestProcessor.COMMUNICATION_ERROR,
                                "Error sending objects." + L2HAZapNodeRequestProcessor.getErrorString(e));
    }
  }

  private void doSyncPrepare() {
    this.indexHACoordinator.doSyncPrepare();
  }

  private void doSyncIndex(final IndexSyncMessage syncMsg) {
    this.indexHACoordinator.applyIndexSync(syncMsg.getCacheName(), syncMsg.getFileName(), syncMsg.getData());
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.stateSyncManager = oscc.getL2Coordinator().getStateSyncManager();
    this.groupManager = oscc.getL2Coordinator().getGroupManager();
  }

}