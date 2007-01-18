/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
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
    Event event = (Event) context;

    switch (event.type) {
      case Event.CREATE: {
        channelCreated(event.channel);
        break;
      }
      case Event.REMOVE: {
        channelRemoved(event.channel);
        break;
      }
      default: {
        throw new AssertionError("unknown event: " + event.type);
      }
    }
  }

  private void channelRemoved(MessageChannel channel) {
    ChannelID channelID = channel.getChannelID();
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

  private void channelCreated(MessageChannel channel) {
    //
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext scc = (ServerConfigurationContext) context;
    this.logger = scc.getLogger(ChannelLifeCycleHandler.class);
  }

  public static class Event implements EventContext {
    public static final int     CREATE = 0;
    public static final int     REMOVE = 1;

    private final int            type;
    private final MessageChannel channel;

    Event(int type, MessageChannel channel) {
      this.type = type;
      this.channel = channel;
      if ((type != CREATE) && (type != REMOVE)) { throw new IllegalArgumentException("invalid type: " + type); }
    }
  }

  public static class EventListener implements DSOChannelManagerEventListener {

    private final Sink sink;

    public EventListener(Sink sink) {
      this.sink = sink;
    }

    public void channelCreated(MessageChannel channel) {
      sink.add(new Event(Event.CREATE, channel));
    }

    public void channelRemoved(MessageChannel channel) {
      sink.add(new Event(Event.REMOVE, channel));
    }

  }

}
