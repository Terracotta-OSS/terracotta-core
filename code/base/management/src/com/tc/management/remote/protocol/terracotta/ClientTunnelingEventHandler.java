/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.net.DSOChannelManagerEventListener;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.message.Message;

public class ClientTunnelingEventHandler extends AbstractEventHandler implements DSOChannelManagerEventListener {

  public static final class L1ConnectionMessage implements EventContext {

    private final MBeanServer    mbs;
    private final MessageChannel channel;
    private final Map            channelIdToJmxConnector;
    private final Map            channelIdToMsgConnection;
    private final boolean        isConnectingMsg;

    public L1ConnectionMessage(MBeanServer mbs, MessageChannel channel, Map channelIdToJmxConnector,
        Map channelIdToMsgConnection, boolean isConnectingMsg) {
      this.mbs = mbs;
      this.channel = channel;
      this.channelIdToJmxConnector = channelIdToJmxConnector;
      this.channelIdToMsgConnection = channelIdToMsgConnection;
      this.isConnectingMsg = isConnectingMsg;

      if (isConnectingMsg && mbs == null) {
        final AssertionError ae = new AssertionError("Attempting to create a L1-connecting-message without"
            + " a valid mBeanServer.");
        throw ae;
      }
    }

    public MBeanServer getMBeanServer() {
      return mbs;
    }

    public MessageChannel getChannel() {
      return channel;
    }

    public Map getChannelIdToJmxConnector() {
      return channelIdToJmxConnector;
    }

    public Map getChannelIdToMsgConnector() {
      return channelIdToMsgConnection;
    }

    public boolean isConnectingMsg() {
      return isConnectingMsg;
    }
  }

  private static final TCLogger logger = TCLogging.getLogger(ClientTunnelingEventHandler.class);

  private final Map             channelIdToJmxConnector;
  private final Map             channelIdToMsgConnection;
  private final MBeanServer     l2MBeanServer;
  private final Object          sinkLock;
  private Sink                  connectStageSink;

  public ClientTunnelingEventHandler() {
    l2MBeanServer = (MBeanServer) MBeanServerFactory.findMBeanServer(null).get(0);
    channelIdToJmxConnector = new HashMap();
    channelIdToMsgConnection = new HashMap();
    sinkLock = new Object();
  }

  public void handleEvent(final EventContext context) throws EventHandlerException {
    if (context instanceof L1JmxReady) {
      final L1JmxReady readyMessage = (L1JmxReady) context;
      connectToL1JmxServer(readyMessage.getChannel());
    } else {
      final JmxRemoteTunnelMessage messageEnvelope = (JmxRemoteTunnelMessage) context;
      if (messageEnvelope.getCloseConnection()) {
        channelRemoved(messageEnvelope.getChannel());
      } else if (messageEnvelope.getInitConnection()) {
        logger.warn("Received a JMX tunneled connection init from the remote"
            + " JMX server, only the JMX client should do this");
      } else {
        routeTunneledMessage(messageEnvelope);
      }
    }
  }

  private void connectToL1JmxServer(final MessageChannel channel) {
    logger.info("L1[" + channel.getChannelID() + "] notified us that their JMX server is now available");
    EventContext msg = new L1ConnectionMessage(l2MBeanServer, channel, channelIdToJmxConnector,
        channelIdToMsgConnection, true);
    synchronized (sinkLock) {
      if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
      connectStageSink.add(msg);
    }
  }

  private void routeTunneledMessage(final JmxRemoteTunnelMessage messageEnvelope) {
    final Message message = messageEnvelope.getTunneledMessage();
    final MessageChannel channel = messageEnvelope.getChannel();
    final ChannelID channelID = channel.getChannelID();
    final TunnelingMessageConnection tmc;
    synchronized (channelIdToMsgConnection) {
      tmc = (TunnelingMessageConnection) channelIdToMsgConnection.get(channelID);
    }
    if (tmc != null) {
      tmc.incomingNetworkMessage(message);
    } else {
      logger.warn("Received tunneled JMX message with no associated message connection,"
          + " sending close() to remote JMX server");
      final JmxRemoteTunnelMessage closeMessage = (JmxRemoteTunnelMessage) channel
          .createMessage(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE);
      closeMessage.setCloseConnection();
      closeMessage.send();
    }
  }

  public void channelCreated(final MessageChannel channel) {
    // DEV-16: Instead of immediately interrogating an L1's JMX server as soon as it connects, we wait for the L1 client
    // to send us a 'L1JmxReady' network message to avoid a startup race condition
  }

  public void channelRemoved(final MessageChannel channel) {
    EventContext msg = new L1ConnectionMessage(null, channel, channelIdToJmxConnector, channelIdToMsgConnection, false);
    synchronized (sinkLock) {
      if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
      connectStageSink.add(msg);
    }
  }

  public void setConnectStageSink(Sink sink) {
    synchronized (sinkLock) {
      if (connectStageSink != null) {
        logger.warn("Attempted to set ConnectStageSink more than once.");
        return;
      }
      connectStageSink = sink;
    }
  }
}
