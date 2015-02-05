/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class ClientHandshakeAckMessageImpl extends DSOMessageBase implements ClientHandshakeAckMessage {

  private static final byte      PERSISTENT_SERVER = 1;
  private static final byte      ALL_NODES         = 2;
  private static final byte      THIS_NODE_ID      = 3;
  private static final byte      SERVER_VERSION    = 4;

  private final Set<NodeID>      allNodes          = new HashSet<NodeID>();
  private boolean                persistentServer;
  private ClientID               thisNodeId;
  private String                 serverVersion;

  public ClientHandshakeAckMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                       TCByteBufferOutputStream out, MessageChannel channel,
                                       TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeAckMessageImpl(SessionID sessionID, MessageMonitor monitor,
                                       MessageChannel channel, TCMessageHeader header,
                                       TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(PERSISTENT_SERVER, persistentServer);

    for (NodeID nodeID : allNodes) {
      putNVPair(ALL_NODES, nodeID);
    }

    putNVPair(THIS_NODE_ID, thisNodeId);
    putNVPair(SERVER_VERSION, serverVersion);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case PERSISTENT_SERVER:
        persistentServer = getBooleanValue();
        return true;
      case ALL_NODES:
        allNodes.add(getNodeIDValue());
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

  @Override
  public void initialize(boolean persistent, Set<? extends NodeID> allNodeIDs, ClientID thisNodeID,
                         String sv) {
    this.persistentServer = persistent;
    this.allNodes.addAll(allNodeIDs);

    this.thisNodeId = thisNodeID;
    this.serverVersion = sv;
  }

  @Override
  public boolean getPersistentServer() {
    return persistentServer;
  }

  @Override
  public ClientID[] getAllNodes() {
    return allNodes.toArray(new ClientID[] {});
  }

  @Override
  public ClientID getThisNodeId() {
    return thisNodeId;
  }

  @Override
  public String getServerVersion() {
    return serverVersion;
  }
}
