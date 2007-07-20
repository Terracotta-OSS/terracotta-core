/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
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
  private static final TCLogger       logger = TCLogging.getLogger(ClientMessageChannel.class);
  private final TCMessageFactory      msgFactory;
  private int                         connectAttemptCount;
  private int                         connectCount;
  private ChannelID                   channelID;
  private final ChannelIDProviderImpl cidProvider;
  private final SessionProvider       sessionProvider;
  private SessionID                   channelSessionID = SessionID.NULL_ID;

  protected ClientMessageChannelImpl(TCMessageFactory msgFactory, TCMessageRouter router, SessionProvider sessionProvider) {
    super(router, logger, msgFactory);
    this.msgFactory = msgFactory;
    this.cidProvider = new ChannelIDProviderImpl();
    this.sessionProvider = sessionProvider;
   }

  public NetworkStackID open() throws TCTimeoutException, UnknownHostException, IOException, MaxConnectionsExceededException {
    final ChannelStatus status = getStatus();

    synchronized (status) {
      if (status.isOpen()) { throw new IllegalStateException("Channel already open"); }
      NetworkStackID id = this.sendLayer.open();
      getStatus().open();
      this.channelID = new ChannelID(id.toLong());
      this.cidProvider.setChannelID(this.channelID);
      this.channelSessionID = sessionProvider.getSessionID();
      return id;
    }
  }

  public void addClassMapping(TCMessageType type, Class msgClass) {
    msgFactory.addClassMapping(type, msgClass);
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
    return connectCount;
  }

  public int getConnectAttemptCount() {
    return this.connectAttemptCount;
  }
  
  /*
   * Session message filter. 
   * To drop old session msgs when session changed. 
   */
  public void send (final TCNetworkMessage message) {
    if (channelSessionID == ((DSOMessageBase)message).getLocalSessionID()) {
     super.send(message);  
    } else {
      logger.info("Drop old message: "+ ((DSOMessageBase)message).getMessageType());
    }
  }

  public void notifyTransportConnected(MessageTransport transport) {
    super.notifyTransportConnected(transport);
    connectCount++;
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    // Move channel to new session
    channelSessionID = sessionProvider.nextSessionID();
    this.fireTransportDisconnectedEvent();
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    super.notifyTransportConnectAttempt(transport);
    connectAttemptCount++;
  }

  public void notifyTransportClosed(MessageTransport transport) {
    //
  }

  public ChannelIDProvider getChannelIDProvider() {
    return cidProvider;
  }

  private static class ChannelIDProviderImpl implements ChannelIDProvider {

    private ChannelID channelID = ChannelID.NULL_ID;
    
    private synchronized void setChannelID(ChannelID channelID) {
      this.channelID = channelID;
    }
    
    public synchronized ChannelID getChannelID() {
      return this.channelID;
    }
    
  }
  
}