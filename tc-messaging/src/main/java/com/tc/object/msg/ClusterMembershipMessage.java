/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.tc.object.msg;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.net.core.ProductID;

import java.io.IOException;

public class ClusterMembershipMessage extends DSOMessageBase {
  private static final byte EVENT_TYPE = 0;
  private static final byte NODE_ID    = 1;
  private static final byte PRODUCT_ID = 2;

  private int               eventType;
  private NodeID            nodeID;
  private ProductID         productId;

  public ClusterMembershipMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out, MessageChannel channel,
                                  TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ClusterMembershipMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                  TCMessageHeader header, TCByteBufferInputStream data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initialize(int type, NodeID nid, ProductID pid) {
    this.eventType = type;
    this.nodeID = nid;
    this.productId = pid;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(EVENT_TYPE, eventType);
    putNVPair(NODE_ID, nodeID);
    putNVPair(PRODUCT_ID, productId.name());
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case EVENT_TYPE:
        eventType = getIntValue();
        return true;
      case NODE_ID:
        nodeID = getNodeIDValue();
        return true;
      case PRODUCT_ID:
        productId = ProductID.valueOf(getStringValue());
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

  public ProductID getProductId() {
    return productId;
  }

  public static class EventType {
    public static final int NODE_CONNECTED    = 0;
    public static final int NODE_DISCONNECTED = 1;

    public static boolean isValidType(int t) {
      return t >= NODE_CONNECTED && t <= NODE_DISCONNECTED;
    }

    public static boolean isNodeConnected(int t) {
      return t == NODE_CONNECTED;
    }

    public static boolean isNodeDisconnected(int t) {
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
