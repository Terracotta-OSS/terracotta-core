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
 */
package com.tc.net.groups;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ServerID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;
import com.tc.util.Assert;

import java.io.IOException;

/**
 * @author EY
 */
public class TCGroupHandshakeMessage extends DSOMessageBase {
  private final static byte MESSAGE_TYPE         = 1;
  private final static byte NODE_ID              = 2;
  private final static byte HANDSHAKE_MESSAGE_ID = 3;
  private final static byte VERSION_ID           = 4;
  private final static byte WEIGHTS_ID           = 5;
  private final static int  HANDSHAKE_ACK        = 2;
  private final static int  HANDSHAKE_OK         = 1;
  private final static int  HANDSHAKE_DENY       = 0;
  private byte              messageType;
  private ServerID          nodeID;
  private int               message;
  private String            version;
  private long[]            weights;

  public TCGroupHandshakeMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutputStream out,
                                 MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public TCGroupHandshakeMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                 TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  public void initializeAck() {
    this.messageType = HANDSHAKE_MESSAGE_ID;
    this.message = HANDSHAKE_ACK;
  }

  public void initializeOk() {
    this.messageType = HANDSHAKE_MESSAGE_ID;
    this.message = HANDSHAKE_OK;
  }

  public void initializeDeny() {
    this.messageType = HANDSHAKE_MESSAGE_ID;
    this.message = HANDSHAKE_DENY;
  }

  public boolean isOkMessage() {
    Assert.eval(this.messageType == HANDSHAKE_MESSAGE_ID);
    return (this.message == HANDSHAKE_OK);
  }
  
  public boolean isAckMessage() {
    Assert.eval(this.messageType == HANDSHAKE_MESSAGE_ID);
    return (this.message == HANDSHAKE_ACK);
  }

  public ServerID getNodeID() {
    Assert.eval(this.messageType == NODE_ID);
    return this.nodeID;
  }

  public void initializeNodeID(ServerID aNodeID, String ver, long[] weightsArray) {
    this.messageType = NODE_ID;
    this.nodeID = aNodeID;
    this.version = ver;
    this.weights = weightsArray;
  }

  public String getVersion() {
    return version;
  }

  public long[] getWeights() {
    return weights;
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(MESSAGE_TYPE, messageType);
    switch (messageType) {
      case NODE_ID:
        putNVPair(VERSION_ID, version);
        putNVPair(WEIGHTS_ID, weights.length);
        for (long weight : weights) {
          getOutputStream().writeLong(weight);
        }
        putNVPair(NODE_ID, nodeID);
        return;
      case HANDSHAKE_MESSAGE_ID:
        putNVPair(HANDSHAKE_MESSAGE_ID, message);
        return;
      default:
        throw new AssertionError();
    }
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case MESSAGE_TYPE:
        messageType = getByteValue();
        return true;
      case NODE_ID:
        nodeID = (ServerID) getNodeIDValue();
        return true;
      case HANDSHAKE_MESSAGE_ID:
        message = getIntValue();
        return true;
      case VERSION_ID:
        version = getStringValue();
        return true;
      case WEIGHTS_ID:
        weights = new long[getIntValue()];
        for (int i = 0; i < weights.length; i++) {
          weights[i] = getLongValue();
        }
        return true;
      default:
        return false;
    }
  }

}