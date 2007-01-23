/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;

import javax.management.remote.generic.MessageConnection;
import javax.management.remote.message.Message;

public final class TunnelingEventHandler extends AbstractEventHandler implements ChannelEventListener {

  private static final TCLogger      logger = TCLogging.getLogger(TunnelingEventHandler.class);

  private final MessageChannel       channel;
  private TunnelingMessageConnection messageConnection;
  private boolean                    acceptOk;

  private Object                     jmxReadyLock;
  private SetOnceFlag                localJmxServerReady;
  private boolean                    transportConnected;
  private boolean                    sentReadyMessage;

  public TunnelingEventHandler(final MessageChannel channel) {
    this.channel = channel;
    this.channel.addListener(this);
    acceptOk = false;
    jmxReadyLock = new Object();
    localJmxServerReady = new SetOnceFlag();
    transportConnected = false;
    sentReadyMessage = false;
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
    acceptOk = false;
    return messageConnection;
  }

  private synchronized void reset() {
    if (messageConnection != null) {
      try {
        messageConnection.close();
      } catch (IOException ioe) {
        logger.warn("Caught I/O exception while closing tunneled JMX connection");
      }
    }
    messageConnection = null;
    acceptOk = false;
    synchronized (jmxReadyLock) {
      sentReadyMessage = false;
    }
    notifyAll();
  }

  public void notifyChannelEvent(final ChannelEvent event) {
    if (event.getChannel() == channel) {
      if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
        synchronized (jmxReadyLock) {
          transportConnected = true;
        }
        sendJmxReadyMessageIfNecessary();
      } else if (event.getType() == ChannelEventType.CHANNEL_CLOSED_EVENT
          || event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
        reset();
        synchronized (jmxReadyLock) {
          transportConnected = false;
        }
      }
    }
  }

  public void jmxIsReady() {
    synchronized (jmxReadyLock) {
      localJmxServerReady.set();
      sendJmxReadyMessageIfNecessary();
    }
  }

  /**
   * Once the local JMX server has successfully started (this happens in a background thread as DSO is so early in the
   * startup process that the system JMX server in 1.5+ can't be created inline with other initialization) we send a
   * 'ready' message to the L2 each time we connect to it. This tells the L2 that they can connect to our local JMX
   * server and see the beans we have in the DSO client.
   */
  private void sendJmxReadyMessageIfNecessary() {
    synchronized (jmxReadyLock) {
      if (localJmxServerReady.isSet() && transportConnected && !sentReadyMessage) {
        logger.info("Client JMX server ready; sending notification to L2 server");
        TCMessage readyMessage = channel.createMessage(TCMessageType.CLIENT_JMX_READY_MESSAGE);
        readyMessage.send();
        sentReadyMessage = true;
      }
    }
  }

}
