/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package com.tc.net.protocol.tcm;

import com.tc.util.ProductID;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.CommStackMismatchException;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.StripeID;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackID;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.JvmIDUtil;
import com.tc.net.protocol.transport.MessageTransport;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionProvider;
import com.tc.security.PwProvider;
import com.tc.util.Assert;
import com.tc.util.TCTimeoutException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;


public class ClientMessageChannelImpl extends AbstractMessageChannel implements ClientMessageChannel, ClientHandshakeMessageFactory {
  private static final TCLogger           logger           = TCLogging.getLogger(ClientMessageChannel.class);
  
  private int                             connectAttemptCount;
  private int                             connectCount;
  private volatile ChannelID              channelID = ChannelID.NULL_ID;
  private final SessionProvider           sessionProvider;
  private final PwProvider                pwProvider;
  private volatile SessionID              channelSessionID = SessionID.NULL_ID;
  private volatile ConnectionInfo         connectionInfo;

  protected ClientMessageChannelImpl(TCMessageFactory msgFactory, TCMessageRouter router,
                                     SessionProvider sessionProvider, 
                                     PwProvider pwProvider, ProductID productId) {
    super(router, logger, msgFactory, StripeID.NULL_ID, productId);

    this.pwProvider = pwProvider;
    this.sessionProvider = sessionProvider;
    this.sessionProvider.initProvider();
  }

  @Override
  public void reset() {
    init();
  }

  protected void init() {
    ChannelStatus status = getStatus();
    status.reset();
    this.sendLayer.reset();
    setLocalNodeID(ClientID.NULL_ID);
  }

  @Override
  public NetworkStackID open(ConnectionInfo info) throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    return open(info, null);
  }

  @Override
  public NetworkStackID open(ConnectionInfo info, char[] pw) throws TCTimeoutException, UnknownHostException, IOException,
      MaxConnectionsExceededException, CommStackMismatchException {
    final ChannelStatus status = getStatus();

    synchronized (status) {
      connectionInfo = info;
      if (status.isOpen()) { throw new IllegalStateException("Channel already open"); }
      // initialize the connection ID, using the local JVM ID
      String username = null;
      if (connectionInfo.getSecurityInfo().hasCredentials()) {
        username = connectionInfo.getSecurityInfo().getUsername();
        Assert.assertNotNull("TCSecurityManager", pwProvider);
        Assert.assertNotNull("Password", pw);
      }
      final ConnectionID cid = new ConnectionID(JvmIDUtil.getJvmID(), (((ClientID) getLocalNodeID()).toLong()),
                                                username, pw, getProductId());
      ((MessageTransport) this.sendLayer).initConnectionID(cid);
      final NetworkStackID id = this.sendLayer.open(info);
      this.channelID = new ChannelID(id.toLong());
      setLocalNodeID(new ClientID(id.toLong()));
      this.channelSessionID = this.sessionProvider.getSessionID();
      channelOpened();
      return id;
    }
  }

  @Override
  public void reopen() throws Exception {
    reset();
    open(this.connectionInfo, getPassword());
  }

  public char[] getPassword() {
    char[] password = null;
    if (connectionInfo != null && connectionInfo.getSecurityInfo().hasCredentials()) {
      Assert.assertNotNull("TCSecurityManager should not be null", pwProvider);
      // use user-password of first server in the group
      password = pwProvider.getPasswordForTC(connectionInfo.getSecurityInfo().getUsername(), connectionInfo.getHostname(),
                                           connectionInfo.getPort());
      Assert.assertNotNull("password is null from securityInfo " + connectionInfo.getSecurityInfo(), password);
    }
    return password;
  }

  @Override
  public ChannelID getChannelID() {
    final ChannelStatus status = getStatus();
    synchronized (status) {
      if (!status.isOpen()) {
        logger.warn("Attempt to get the channel ID of an unopened channel - " + this.channelID);
      }
      return this.channelID;
    }
  }

  @Override
  public int getConnectCount() {
    return this.connectCount;
  }

  @Override
  public int getConnectAttemptCount() {
    return this.connectAttemptCount;
  }

  /*
   * Session message filter. To drop old session msgs when session changed.
   */
  @Override
  public void send(TCNetworkMessage message) throws IOException {
// used to do session filtering here.  This wreaks havoc on upper layers to silently drop 
// messages on the floor.  send everything through
    super.send(message);
  }

  @Override
  public void notifyTransportConnected(MessageTransport transport) {
    long channelIdLong = transport.getConnectionId().getChannelID();
    this.channelID = new ChannelID(channelIdLong);
    setLocalNodeID(new ClientID(channelIdLong));
    super.notifyTransportConnected(transport);
    this.connectCount++;
  }

  @Override
  public void notifyTransportDisconnected(MessageTransport transport, boolean forcedDisconnect) {
    this.channelSessionID = this.sessionProvider.nextSessionID();
    logger.info("ClientMessageChannel moves to " + this.channelSessionID + " for remote node " + getRemoteNodeID());
    super.notifyTransportDisconnected(transport, forcedDisconnect);
  }

  @Override
  public void notifyTransportConnectAttempt(MessageTransport transport) {
    super.notifyTransportConnectAttempt(transport);
    this.connectAttemptCount++;
  }

  @Override
  public void notifyTransportClosed(MessageTransport transport) {
    //
  }

  @Override
  public ClientID getClientID() {
    return (ClientID) getLocalNodeID();
  }

  @Override
  public ClientHandshakeMessage newClientHandshakeMessage(String uuid, String name, String clientVersion, boolean isEnterpriseClient, boolean isDiagnostic) {
    final ClientHandshakeMessage rv = (ClientHandshakeMessage) createMessage(TCMessageType.CLIENT_HANDSHAKE_MESSAGE);
    rv.setClientVersion(clientVersion);
    rv.setDiagnosticClient(isDiagnostic);
    rv.setEnterpriseClient(isEnterpriseClient);
    rv.setClientPID(getPID());
    rv.setUUID(uuid);
    rv.setName(name);
    return rv;
  }

  @Override
  public ClientHandshakeMessageFactory getClientHandshakeMessageFactory() {
    return this;
  }

  private int getPID() {
    String vmName = ManagementFactory.getRuntimeMXBean().getName();
    int index = vmName.indexOf('@');

    if (index < 0) { throw new RuntimeException("unexpected format: " + vmName); }

    return Integer.parseInt(vmName.substring(0, index));
  }
}
