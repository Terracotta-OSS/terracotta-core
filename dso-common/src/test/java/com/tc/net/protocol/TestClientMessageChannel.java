/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.exception.ImplementMe;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageFactory;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionProvider;

/**
 * @author orion
 */

public class TestClientMessageChannel implements ClientMessageChannel {
  private boolean initConnect = true;

  public TestClientMessageChannel() {
    this(null, null, null, null);
  }

  public TestClientMessageChannel(final TCMessageFactory msgFactory, final TCMessageRouter router,
                                  final SessionProvider sessionProvider, final ConnectionAddressProvider addrProvider) {

    setLocalNodeID(ClientID.NULL_ID);
    setRemoteNodeID(GroupID.NULL_ID);
  }

  public boolean isInitConnect() {
    return this.initConnect;
  }

  public void connected() {
    this.initConnect = false;
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

  public void unrouteMessageType(final TCMessageType type) {
    throw new ImplementMe();

  }

  public void addAttachment(final String key, final Object value, final boolean replace) {
    throw new ImplementMe();

  }

  public void addListener(final ChannelEventListener listener) {
    throw new ImplementMe();

  }

  public void close() {
    throw new ImplementMe();

  }

  public TCMessage createMessage(final TCMessageType type) {
    throw new ImplementMe();
  }

  public Object getAttachment(final String key) {
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

  public Object removeAttachment(final String key) {
    throw new ImplementMe();
  }

  public void send(final TCNetworkMessage message) {
    throw new ImplementMe();

  }

  public void setRemoteNodeID(final NodeID destination) {
    throw new ImplementMe();
  }

  public void setLocalNodeID(final NodeID source) {
    throw new ImplementMe();
  }

  public void reloadConfiguration(ConnectionAddressProvider... cap) {
    throw new ImplementMe();
  }
}