/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.async.api.Sink;
import com.tc.exception.ImplementMe;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.GroupID;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.object.session.SessionProvider;

/**
 * @author orion
 */

public class TestClientMessageChannel implements ClientMessageChannel {
  private boolean                             initConnect      = true;

  public TestClientMessageChannel() {
    this(null, null, null, null, null);
  }
  
  public TestClientMessageChannel(TCMessageFactory msgFactory, TCMessageRouter router,
                                     SessionProvider sessionProvider, ConnectionAddressProvider addrProvider,
                                     ClientGroupMessageChannel multiplex) {

    setLocalNodeID(ClientID.NULL_ID);
    setRemoteNodeID(GroupID.NULL_ID);
  }

  public boolean isInitConnect() {
    return initConnect;
  }

  public void connected() {
    initConnect = false;
  }

  public void addClassMapping(TCMessageType type, Class msgClass) {
    throw new ImplementMe();
    
  }

  public ChannelIDProvider getChannelIDProvider() {
    throw new ImplementMe();
  }

  public int getConnectAttemptCount() {
    throw new ImplementMe();
  }

  public int getConnectCount() {
    throw new ImplementMe();
  }

  public void routeMessageType(TCMessageType messageType, Sink destSink, Sink hydrateSink) {
    throw new ImplementMe();
    
  }

  public void routeMessageType(TCMessageType type, TCMessageSink sink) {
    throw new ImplementMe();
    
  }

  public void unrouteMessageType(TCMessageType type) {
    throw new ImplementMe();
    
  }

  public void addAttachment(String key, Object value, boolean replace) {
    throw new ImplementMe();
    
  }

  public void addListener(ChannelEventListener listener) {
    throw new ImplementMe();
    
  }

  public void close() {
    throw new ImplementMe();
    
  }

  public TCMessage createMessage(TCMessageType type) {
    throw new ImplementMe();
  }

  public Object getAttachment(String key) {
    throw new ImplementMe();
  }

  public ChannelID getChannelID() {
    throw new ImplementMe();
  }

  public NodeID getRemoteNodeID() {
    throw new ImplementMe();
  }

  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  public NodeID getLocalNodeID() {
    throw new ImplementMe();
  }

  public boolean isClosed() {
    throw new ImplementMe();
  }

  public boolean isConnected() {
    throw new ImplementMe();
  }

  public boolean isOpen() {
    throw new ImplementMe();
  }

  public NetworkStackID open() {
    throw new ImplementMe();
  }

  public Object removeAttachment(String key) {
    throw new ImplementMe();
  }

  public void send(TCNetworkMessage message) {
    throw new ImplementMe();
    
  }

  public void setRemoteNodeID(NodeID destination) {
    throw new ImplementMe();
  }

  public void setLocalNodeID(NodeID source) {
    throw new ImplementMe();
  }

}