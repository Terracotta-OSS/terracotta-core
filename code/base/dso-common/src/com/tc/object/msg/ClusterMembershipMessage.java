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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ClusterMembershipMessage extends DSOMessageBase {

  public static class EventType {
    public static final int NODE_CONNECTED    = 0;
    public static final int NODE_DISCONNECTED = 1;

    public static boolean isValidType(final int t) {
      return t >= 0 && t <= 2;
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

  private static final byte EVENT_TYPE          = 0;
  private static final byte NODE_ID             = 1;
  private static final byte ALL_NODES           = 2;

  public ClusterMembershipMessage(MessageMonitor monitor, TCByteBufferOutput out, MessageChannel channel,
                                  TCMessageType type) {
    super(monitor, out, channel, type);
  }

  public ClusterMembershipMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  private final Set allNodes = new HashSet();
  private int       eventType;
  private String    nodeId;

  public void initialize(int et, ChannelID cid, MessageChannel[] channels) {
    eventType = et;
    nodeId = toString(cid);
    for (int i = 0; i < channels.length; i++) {
      allNodes.add(toString(channels[i].getChannelID()));
    }
  }

  private String toString(ChannelID cid) {
    return String.valueOf(cid.toLong());
  }

  protected void dehydrateValues() {
    putNVPair(EVENT_TYPE, eventType);
    putNVPair(NODE_ID, nodeId);
    for (Iterator i = allNodes.iterator(); i.hasNext();) {
      putNVPair(ALL_NODES, (String) i.next());
    }
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case EVENT_TYPE:
        eventType = getIntValue();
        return true;
      case NODE_ID:
        nodeId = getStringValue();
        return true;
      case ALL_NODES:
        allNodes.add(getStringValue());
        return true;
      default:
        return false;
    }
  }

  public boolean isNodeConnectedEvent() {
    return EventType.isNodeConnected(eventType) && !getDestinationNodeId().equals(nodeId);
  }

  public boolean isNodeDisconnectedEvent() {
    return EventType.isNodeDisconnected(eventType);
  }

  public boolean isThisNodeConnected() {
    return EventType.isNodeConnected(eventType) && getDestinationNodeId().equals(nodeId);
  }

  public int getEventType() {
    return eventType;
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getDestinationNodeId() {
    return toString(getChannelID());
  }

  public Set getAllNodes() {
    return allNodes;
  }

  public String[] getAllNodeIds() {
    String[] rv = new String[allNodes.size()];
    allNodes.toArray(rv);
    Arrays.sort(rv);
    return rv;
  }

  public String print() {
    return "ClusterMemebershipEventMessage: eventType = " + EventType.toString(eventType) + ", destNodeId="
           + getDestinationNodeId() + ", nodeId=" + nodeId + ", allNodes=" + allNodes + "";
  }
}
