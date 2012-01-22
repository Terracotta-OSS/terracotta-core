/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;

import java.io.IOException;

public class ClusterMembershipMessage extends DSOMessageBase {
  private static final byte EVENT_TYPE = 0;
  private static final byte NODE_ID    = 1;

  private int               eventType;
  private NodeID            nodeID;

  public ClusterMembershipMessage(final SessionID sessionID, final MessageMonitor monitor, final TCByteBufferOutputStream out, final MessageChannel channel,
                                  final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClusterMembershipMessage(final SessionID sessionID, final MessageMonitor monitor, final MessageChannel channel,
                                  final TCMessageHeader header, final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(final int et, final NodeID nodeID2, final MessageChannel[] channels) {
    eventType = et;
    nodeID = nodeID2;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(EVENT_TYPE, eventType);
    putNVPair(NODE_ID, nodeID);
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case EVENT_TYPE:
        eventType = getIntValue();
        return true;
      case NODE_ID:
        nodeID = getNodeIDValue();
        return true;
      default:
        return false;
    }
  }

  public boolean isNodeConnectedEvent() {
    return EventType.isNodeConnected(eventType);
  }

  public boolean isNodeDisconnectedEvent() {
    return EventType.isNodeDisconnected(eventType);
  }

  public int getEventType() {
    return eventType;
  }

  public NodeID getNodeId() {
    return nodeID;
  }

  @Override
  protected String describePayload() {
    return EventType.toString(eventType) + " nodeId=" + nodeID;
  }

  public static class EventType {
    public static final int NODE_CONNECTED    = 0;
    public static final int NODE_DISCONNECTED = 1;

    public static boolean isValidType(final int t) {
      return t >= NODE_CONNECTED && t <= NODE_DISCONNECTED;
    }

    public static boolean isNodeConnected(final int t) {
      return t == NODE_CONNECTED;
    }

    public static boolean isNodeDisconnected(final int t) {
      return t == NODE_DISCONNECTED;
    }

    public static String toString(final int eventType) {
      switch (eventType) {
        case NODE_CONNECTED:
          return "NODE_CONNECTED";
        case NODE_DISCONNECTED:
          return "NODE_DISCONNECTED";
        default:
          return "UNKNOWN";
      }
    }
  }
}
