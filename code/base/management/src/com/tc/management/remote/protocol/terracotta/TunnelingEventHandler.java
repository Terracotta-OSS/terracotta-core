/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import java.io.IOException;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;

public final class TunnelingEventHandler extends AbstractEventHandler implements ChannelEventListener {

  private static final TCLogger      logger = TCLogging.getLogger(TunnelingEventHandler.class);

  private final MessageChannel       channel;
  private TunnelingMessageConnection messageConnection;
  private boolean                    acceptOk;

  public TunnelingEventHandler(final MessageChannel channel) {
    this.channel = channel;
    this.channel.addListener(this);
    acceptOk = false;
  }

  public void handleEvent(final EventContext context) throws EventHandlerException {
    final JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) context;
    if (messageEnvelope.getCloseConnection()) {
      reset();
    } else {
      final Message message = messageEnvelope.getTunneledMessage();
      synchronized (this) {
        if (messageEnvelope.getInitConnection()) {
          if (messageConnection != null) {
            logger.warn("Received a client connection initialization, resetting existing connection");
            reset();
          }
          messageConnection = new TunnelingMessageConnection(channel, true);
          acceptOk = true;
          notifyAll();
        } else if (messageConnection == null) {
          logger.warn("Received unexpected data message, connection is not yet established");
        } else {
          if (message != null) {
            messageConnection.incomingNetworkMessage(message);
          } else {
            logger.warn("Received tunneled message with no data, resetting connection");
            reset();
          }
        }
      }
    }
  }

  synchronized MessageConnection accept() throws IOException {
    while (!acceptOk) {
      try {
        wait();
      } catch (InterruptedException ie) {
        logger.warn("Interrupted while waiting for a new connection", ie);
        throw new IOException("Interrupted while waiting for new connection: " + ie.getMessage());
      }
    }
    logger.info("Tunneled JMX connection to the Terracotta server was established");
    acceptOk = false;
    return messageConnection;
  }

  private synchronized void reset() {
    if (messageConnection != null) {
      logger.info("Closing tunneled JMX connection to the Terracotta server");
      try {
        messageConnection.close();
      } catch (IOException ioe) {
        logger.warn("Caught I/O exception while closing tunneled JMX connection");
      }
    }
    messageConnection = null;
    acceptOk = false;
    notifyAll();
  }

  public void notifyChannelEvent(final ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT
          || event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
        reset();
      }
    }
  }

}
