/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.NodeID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.net.UnknownHostException;

/**
 * @author orion
 */

public class ClientMessageChannelImpl extends AbstractMessageChannel implements ClientMessageChannel {
  private static final TCLogger       logger           = TCLogging.getLogger(ClientMessageChannel.class);
  private int                         connectAttemptCount;
  private int                         connectCount;
  private ChannelID                   channelID;
  private final ChannelIDProviderImpl cidProvider;
  private final SessionProvider       sessionProvider;
  private volatile SessionID          channelSessionID = SessionID.NULL_ID;

  protected ClientMessageChannelImpl(final TCMessageFactory msgFactory, final TCMessageRouter router,
                                     final SessionProvider sessionProvider, final NodeID remoteNodeID) {
    super(router, logger, msgFactory, remoteNodeID);
    this.cidProvider = new ChannelIDProviderImpl();
    this.sessionProvider = sessionProvider;
    this.sessionProvider.initProvider(remoteNodeID);
  }

  @Override
  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final ChannelStatus status = getStatus();

    synchronized (status) {
      if (status.isOpen()) { throw new IllegalStateException("Channel already open"); }
      ((MessageTransport) this.sendLayer).initConnectionID(new ConnectionID((((ClientID) getLocalNodeID()).toLong())));
      final NetworkStackID id = this.sendLayer.open();
      channelOpened();
      this.channelID = new ChannelID(id.toLong());
      setLocalNodeID(new ClientID(id.toLong()));
      this.cidProvider.setChannelID(this.channelID);
      this.channelSessionID = this.sessionProvider.getSessionID(getRemoteNodeID());
      return id;
    }
  }

  public ChannelID getChannelID() {
    final ChannelStatus status = getStatus();
    synchronized (status) {
      if (!status.isOpen()) {
        throw new IllegalStateException("Attempt to get the channel ID of an unopened channel.");
      } else {
        return this.channelID;
      }
    }
  }

  public int getConnectCount() {
    return this.connectCount;
  }

  public int getConnectAttemptCount() {
    return this.connectAttemptCount;
  }

  /*
   * Session message filter. To drop old session msgs when session changed.
   */
  @Override
  public void send(final TCNetworkMessage message) {
    if (this.channelSessionID == ((DSOMessageBase) message).getLocalSessionID()) {
      super.send(message);
    } else {
      logger.info("Drop old message: " + ((DSOMessageBase) message).getMessageType() + " Expected "
                  + this.channelSessionID + " but got " + ((DSOMessageBase) message).getLocalSessionID());
    }
  }

  @Override
  public void notifyTransportConnected(final MessageTransport transport) {
    super.notifyTransportConnected(transport);
    this.connectCount++;
  }

  @Override
  public void notifyTransportDisconnected(final MessageTransport transport, final boolean forcedDisconnect) {
    // Move channel to new session
    this.channelSessionID = this.sessionProvider.nextSessionID(getRemoteNodeID());
    logger.info("ClientMessageChannel moves to " + this.channelSessionID);
    fireTransportDisconnectedEvent();
  }

  @Override
  public void notifyTransportConnectAttempt(final MessageTransport transport) {
    super.notifyTransportConnectAttempt(transport);
    this.connectAttemptCount++;
  }

  @Override
  public void notifyTransportClosed(final MessageTransport transport) {
    //
  }

  public ChannelIDProvider getChannelIDProvider() {
    return this.cidProvider;
  }

  private static class ChannelIDProviderImpl implements ChannelIDProvider {

    private ChannelID channelID = ChannelID.NULL_ID;

    private synchronized void setChannelID(final ChannelID channelID) {
      this.channelID = channelID;
    }

    public synchronized ChannelID getChannelID() {
      return this.channelID;
    }

  }

  // for testing purpose
  protected SessionID getSessionID() {
    return this.channelSessionID;
  }

}
