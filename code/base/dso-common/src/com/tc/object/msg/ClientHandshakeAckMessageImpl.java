/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ClientHandshakeAckMessageImpl extends DSOMessageBase implements ClientHandshakeAckMessage {

  private static final byte   PERSISTENT_SERVER = 1;
  private static final byte   ALL_NODES         = 2;
  private static final byte   THIS_NODE_ID      = 3;
  private static final byte   SERVER_VERSION    = 4;

  private final Set<ClientID> allNodes          = new HashSet<ClientID>();
  private boolean             persistentServer;
  private ClientID            thisNodeId;
  private String              serverVersion;

  public ClientHandshakeAckMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                       final TCByteBufferOutputStream out, final MessageChannel channel,
                                       final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeAckMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                       final MessageChannel channel, final TCMessageHeader header,
                                       final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(PERSISTENT_SERVER, persistentServer);

    for (ClientID clientID : allNodes) {
      putNVPair(ALL_NODES, clientID);
    }

    putNVPair(THIS_NODE_ID, thisNodeId);
    putNVPair(SERVER_VERSION, serverVersion);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case PERSISTENT_SERVER:
        persistentServer = getBooleanValue();
        return true;
      case ALL_NODES:
        allNodes.add((ClientID) getNodeIDValue());
        return true;
      case THIS_NODE_ID:
        thisNodeId = (ClientID) getNodeIDValue();
        return true;
      case SERVER_VERSION:
        serverVersion = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(final boolean persistent, final Set<ClientID> allNodeIDs, final ClientID thisNodeID,
                         final String sv) {
    this.persistentServer = persistent;
    this.allNodes.addAll(allNodeIDs);

    this.thisNodeId = thisNodeID;
    this.serverVersion = sv;
  }

  public boolean getPersistentServer() {
    return persistentServer;
  }

  public ClientID[] getAllNodes() {
    return allNodes.toArray(new ClientID[] {});
  }

  public ClientID getThisNodeId() {
    return thisNodeId;
  }

  public String getServerVersion() {
    return serverVersion;
  }

}
