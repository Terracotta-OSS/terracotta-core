/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.protocol;

import com.tc.exception.ImplementMe;
import com.tc.license.ProductID;
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

  @Override
  public ChannelIDProvider getChannelIDProvider() {
    throw new ImplementMe();
  }

  @Override
  public int getConnectAttemptCount() {
    throw new ImplementMe();
  }

  @Override
  public int getConnectCount() {
    throw new ImplementMe();
  }

  public void unrouteMessageType(final TCMessageType type) {
    throw new ImplementMe();

  }

  @Override
  public void addAttachment(final String key, final Object value, final boolean replace) {
    throw new ImplementMe();

  }

  @Override
  public void addListener(final ChannelEventListener listener) {
    throw new ImplementMe();

  }

  @Override
  public void close() {
    throw new ImplementMe();

  }

  @Override
  public TCMessage createMessage(final TCMessageType type) {
    throw new ImplementMe();
  }

  @Override
  public Object getAttachment(final String key) {
    throw new ImplementMe();
  }

  @Override
  public ChannelID getChannelID() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getRemoteNodeID() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getLocalAddress() {
    throw new ImplementMe();
  }

  @Override
  public TCSocketAddress getRemoteAddress() {
    throw new ImplementMe();
  }

  @Override
  public NodeID getLocalNodeID() {
    throw new ImplementMe();
  }

  @Override
  public boolean isClosed() {
    throw new ImplementMe();
  }

  @Override
  public boolean isConnected() {
    throw new ImplementMe();
  }

  @Override
  public boolean isOpen() {
    throw new ImplementMe();
  }

  @Override
  public NetworkStackID open() {
    throw new ImplementMe();
  }

  @Override
  public NetworkStackID open(char[] password) {
    throw new ImplementMe();
  }

  @Override
  public Object removeAttachment(final String key) {
    throw new ImplementMe();
  }

  @Override
  public void send(final TCNetworkMessage message) {
    throw new ImplementMe();

  }

  public void setRemoteNodeID(final NodeID destination) {
    throw new ImplementMe();
  }

  @Override
  public void setLocalNodeID(final NodeID source) {
    throw new ImplementMe();
  }

  public void reloadConfiguration(ConnectionAddressProvider... cap) {
    throw new ImplementMe();
  }

  @Override
  public void reopen() throws Exception {
    throw new ImplementMe();
  }

  @Override
  public ProductID getProductId() {
    return null;
  }
}
