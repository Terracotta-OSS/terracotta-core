/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.InBandMoveToNextSink;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.objectserver.context.ChannelStateEventContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.TransactionBatchManager;

public class ChannelLifeCycleHandler extends AbstractEventHandler implements DSOChannelManagerEventListener {
  private final ServerTransactionManager transactionManager;
  private final TransactionBatchManager  transactionBatchManager;
  private TCLogger                       logger;
  private final CommunicationsManager    commsManager;
  private final DSOChannelManager        channelMgr;
  private Sink                           channelSink;
  private Sink hydrateSink;
  private Sink processTransactionSink;

  public ChannelLifeCycleHandler(CommunicationsManager commsManager, ServerTransactionManager transactionManager,
                                 TransactionBatchManager transactionBatchManager, DSOChannelManager channelManager) {
    this.commsManager = commsManager;
    this.transactionManager = transactionManager;
    this.transactionBatchManager = transactionBatchManager;
    this.channelMgr = channelManager;
  }

  public void handleEvent(EventContext context) {
    ChannelStateEventContext event = (ChannelStateEventContext) context;

    switch (event.getType()) {
      case ChannelStateEventContext.CREATE: {
        channelCreated(event.getChannelID());
        break;
      }
      case ChannelStateEventContext.REMOVE: {
        channelRemoved(event.getChannelID());
        break;
      }
      default: {
        throw new AssertionError("unknown event: " + event.getType());
      }
    }
  }

  private void channelRemoved(ChannelID channelID) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_DISCONNECTED, channelID);
    if (commsManager.isInShutdown()) {
      logger.info("Ignoring transport disconnect for " + channelID + " while shutting down.");
    } else {
      logger.info("Received transport disconnect.  Shutting down client " + channelID);
      transactionManager.shutdownClient(channelID);
      transactionBatchManager.shutdownClient(channelID);
    }
  }

  private void channelCreated(ChannelID channelID) {
    broadcastClusterMembershipMessage(ClusterMembershipMessage.EventType.NODE_CONNECTED, channelID);
  }

  private void broadcastClusterMembershipMessage(int eventType, ChannelID channelID) {
    MessageChannel[] channels = channelMgr.getActiveChannels();
    for (int i = 0; i < channels.length; i++) {
      MessageChannel channel = channels[i];

      if (!channel.getChannelID().equals(channelID)) {
        ClusterMembershipMessage cmm = (ClusterMembershipMessage) channel
            .createMessage(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE);
        cmm.initialize(eventType, channelID, channels);
        cmm.send();
      }
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
    channelSink = scc.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink();
    hydrateSink =  scc.getStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK).getSink();
    processTransactionSink =  scc.getStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE).getSink();
  }

  public void channelCreated(MessageChannel channel) {
    channelSink.add(new ChannelStateEventContext(ChannelStateEventContext.CREATE, channel.getChannelID()));
  }

  
  public void channelRemoved(MessageChannel channel) {
    // We want all the messages in the system from this client to reach its destinations before processing this request.
    // esp. hydrate stage and process transaction stage. This goo is for that.
    final ChannelStateEventContext disconnectEvent = new ChannelStateEventContext(ChannelStateEventContext.REMOVE, channel.getChannelID());
    InBandMoveToNextSink context1 = new InBandMoveToNextSink(disconnectEvent, channelSink);
    InBandMoveToNextSink context2 = new InBandMoveToNextSink(context1, processTransactionSink);
    hydrateSink.add(context2);
  }

}
