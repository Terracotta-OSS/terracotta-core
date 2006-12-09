/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.util.concurrent.ThreadUtil;

/**
 * @author steve
 */
public class ChannelLifeCycleHandler extends AbstractEventHandler {
  private final ServerTransactionManager transactionManager;
  private final TransactionBatchManager  transactionBatchManager;
  private TCLogger                       logger;
  private final CommunicationsManager    commsManager;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager, ServerTransactionManager transactionManager,
                                 TransactionBatchManager transactionBatchManager) {
    this.commsManager = commsManager;
    this.transactionManager = transactionManager;
    this.transactionBatchManager = transactionBatchManager;
  }

  public void handleEvent(EventContext context) {
    ChannelEvent event = (ChannelEvent) context;
    if (ChannelEventType.TRANSPORT_DISCONNECTED_EVENT.matches(event)) {
      ChannelID channelID = event.getChannelID();
      if (commsManager.isInShutdown()) {
        logger.info("Ignoring transport disconnect for " + channelID + " while shutting down.");
      } else {
        // Giving 0.5 sec for the server to catch up with any pending transactions. Not a fool prove mechanism.
        ThreadUtil.reallySleep(500);
        logger.info("Received transport disconnect.  Killing client " + channelID);
        transactionManager.shutdownClient(channelID);
        transactionBatchManager.shutdownClient(channelID);
      }
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
  }

}
