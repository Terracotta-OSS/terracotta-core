/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.ChannelID;
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
  private String            nodeId;

  public ClusterMembershipMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                  TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClusterMembershipMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(int et, ChannelID cid, MessageChannel[] channels) {
    eventType = et;
    nodeId = toString(cid);
  }

  private String toString(ChannelID cid) {
    return String.valueOf(cid.toLong());
  }

  protected void dehydrateValues() {
    putNVPair(EVENT_TYPE, eventType);
    putNVPair(NODE_ID, nodeId);
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case EVENT_TYPE:
        eventType = getIntValue();
        return true;
      case NODE_ID:
        nodeId = getStringValue();
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

  public String getNodeId() {
    return nodeId;
  }

  protected String describePayload() {
    return EventType.toString(eventType) + " nodeId=" + nodeId;
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

    public static String toString(int eventType) {
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
