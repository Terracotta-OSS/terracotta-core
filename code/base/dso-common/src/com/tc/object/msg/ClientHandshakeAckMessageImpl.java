/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClientHandshakeAckMessageImpl extends DSOMessageBase implements ClientHandshakeAckMessage {

  private static final byte PERSISTENT_SERVER = 1;
  private static final byte ALL_NODES         = 2;
  private static final byte THIS_NODE_ID      = 3;
  private static final byte SERVER_VERSION    = 4;

  private final Set         allNodes          = new HashSet();
  private boolean           persistentServer;
  private String            thisNodeId;
  private String            serverVersion;

  public ClientHandshakeAckMessageImpl(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                       MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClientHandshakeAckMessageImpl(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(PERSISTENT_SERVER, persistentServer);

    for (Iterator i = allNodes.iterator(); i.hasNext();) {
      putNVPair(ALL_NODES, (String) i.next());
    }

    putNVPair(THIS_NODE_ID, thisNodeId);
    putNVPair(SERVER_VERSION, serverVersion);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case PERSISTENT_SERVER:
        persistentServer = getBooleanValue();
        return true;
      case ALL_NODES:
        allNodes.add(getStringValue());
        return true;
      case THIS_NODE_ID:
        thisNodeId = getStringValue();
        return true;
      case SERVER_VERSION:
        serverVersion = getStringValue();
        return true;
      default:
        return false;
    }
  }

  public void initialize(boolean persistent, Set allNodeIDs, String thisNodeID, String sv) {
    this.persistentServer = persistent;
    this.allNodes.addAll(allNodeIDs);

    this.thisNodeId = thisNodeID;
    this.serverVersion = sv;
  }

  public boolean getPersistentServer() {
    return persistentServer;
  }

  public String[] getAllNodes() {
    return (String[]) allNodes.toArray(new String[] {});
  }

  public String getThisNodeId() {
    return thisNodeId;
  }

  public String getServerVersion() {
    return serverVersion;
  }
}
