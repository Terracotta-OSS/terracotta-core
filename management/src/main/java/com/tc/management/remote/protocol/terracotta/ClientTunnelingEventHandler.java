/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventHandlerException;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.net.DSOChannelManagerEventListener;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;

public class ClientTunnelingEventHandler implements DSOChannelManagerEventListener {

  public static final String    STATE_ATTACHMENT = ClientTunnelingEventHandler.class.getName() + ".STATE_ATTACHMENT";

  private static final TCLogger logger           = TCLogging.getLogger(ClientTunnelingEventHandler.class);

  private final MBeanServer     l2MBeanServer;
  private final Object          sinkLock;
  private Sink<L1ConnectionMessage.Connecting> connectStageSink;
  private Sink<L1ConnectionMessage.Disconnecting> disconnectStageSink;
  private Sink<TunneledDomainsChanged> tunnelDomainsChangedStageSink;

  public ClientTunnelingEventHandler() {
    l2MBeanServer = MBeanServerFactory.findMBeanServer(null).get(0);
    sinkLock = new Object();
  }

  
  private final AbstractEventHandler<L1JmxReady> readyHandler = new AbstractEventHandler<L1JmxReady>(){
    @Override
    public void handleEvent(L1JmxReady readyMessage) throws EventHandlerException {
      setupJMXStateMachine(readyMessage.getChannel());
      connectToL1JmxServer(readyMessage);
    }
  };
  public AbstractEventHandler<L1JmxReady> getReadyHandler() {
    return this.readyHandler;
  }
  
  private final AbstractEventHandler<TunneledDomainsChanged> tunnelHandler = new AbstractEventHandler<TunneledDomainsChanged>(){
    @Override
    public void handleEvent(TunneledDomainsChanged message) throws EventHandlerException {
      setupJMXStateMachine(message.getChannel());
      synchronized (sinkLock) {
        if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
        tunnelDomainsChangedStageSink.addSingleThreaded(message);
      }
    }
  };
  public AbstractEventHandler<TunneledDomainsChanged> getTunnelHandler() {
    return this.tunnelHandler;
  }
  
  private final AbstractEventHandler<JmxRemoteTunnelMessage> remoteHandler = new AbstractEventHandler<JmxRemoteTunnelMessage>(){
    @Override
    public void handleEvent(JmxRemoteTunnelMessage messageEnvelope) throws EventHandlerException {
      setupJMXStateMachine(messageEnvelope.getChannel());
      if (messageEnvelope.getCloseConnection()) {
        channelRemoved(messageEnvelope.getChannel());
      } else if (messageEnvelope.getInitConnection()) {
        logger.warn("Received a JMX tunneled connection init from the remote"
                    + " JMX server, only the JMX client should do this");
      } else {
        routeTunneledMessage(messageEnvelope);
      }
    }
  };
  public AbstractEventHandler<JmxRemoteTunnelMessage> getRemoteHandler() {
    return this.remoteHandler;
  }

  private void connectToL1JmxServer(L1JmxReady readyMessage) {
    logger.info("L1[" + readyMessage.getChannelID() + "] notified us that their JMX server is now available");

    L1ConnectionMessage.Connecting msg = new L1ConnectionMessage.Connecting(l2MBeanServer, readyMessage.getChannel(),
                                                          readyMessage.getUUID(), readyMessage.getTunneledDomains());
    synchronized (sinkLock) {
      if (connectStageSink == null) { throw new AssertionError("ConnectStageSink was not set."); }
      connectStageSink.addSingleThreaded(msg);
    }
  }

  private static void setupJMXStateMachine(MessageChannel channel) {
    if (channel.getAttachment(STATE_ATTACHMENT) == null) {
      channel.addAttachment(STATE_ATTACHMENT, new JMXConnectStateMachine(), false);
    }
  }

  private void routeTunneledMessage(JmxRemoteTunnelMessage messageEnvelope) {
    JMXConnectStateMachine state = (JMXConnectStateMachine) messageEnvelope.getChannel()
        .getAttachment(STATE_ATTACHMENT);

    if (state != null) {
      state.incomingNetworkMessage(messageEnvelope);
    } else {
      logger.warn("No jmx connect state present for channel " + messageEnvelope.getChannelID());
    }
  }

  @Override
  public void channelCreated(MessageChannel channel) {
    // DEV-16: Instead of immediately interrogating an L1's JMX server as soon as it connects, we wait for the L1 client
    // to send us a 'L1JmxReady' network message to avoid a startup race condition
  }

  @Override
  public void channelRemoved(MessageChannel channel) {
    L1ConnectionMessage.Disconnecting msg = new L1ConnectionMessage.Disconnecting(channel);

    synchronized (sinkLock) {
      if (disconnectStageSink == null) { throw new AssertionError("DisconnectStageSink was not set."); }
      disconnectStageSink.addSingleThreaded(msg);
    }
  }

  public void setStages(Sink<L1ConnectionMessage.Connecting> connectSink, Sink<L1ConnectionMessage.Disconnecting> disconnectSink, Sink<TunneledDomainsChanged> tunnelDomainsChangedSink) {
    synchronized (sinkLock) {
      if ((connectStageSink != null) || (disconnectStageSink != null) || (tunnelDomainsChangedStageSink != null)) {
        //
        throw new AssertionError("attempt to set stages more than once");
      }
      connectStageSink = connectSink;
      disconnectStageSink = disconnectSink;
      tunnelDomainsChangedStageSink = tunnelDomainsChangedSink;
    }
  }

}
