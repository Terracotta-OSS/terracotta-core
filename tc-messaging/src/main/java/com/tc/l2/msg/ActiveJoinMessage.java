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
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;

import java.io.IOException;

public class ActiveJoinMessage extends AbstractGroupMessage {
  public static final int ACTIVE_JOIN         = 0x01;
  public static final int ACTIVE_LEFT         = 0x02;
  public static final int ACTIVE_REQUEST_JOIN = 0x03;

  private ServerID        serverID;

  public ActiveJoinMessage() {
    super(-1);
  }

  public ActiveJoinMessage(int type) {
    this(type, ServerID.NULL_ID);
  }

  public ActiveJoinMessage(int type, ServerID serverID) {
    super(type);
    this.serverID = serverID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    serverID = (ServerID) nodeIDSerializer.getNodeID();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(serverID);
    nodeIDSerializer.serializeTo(out);
  }

  public ServerID getServerID() {
    return serverID;
  }

  @Override
  public String toString() {
    return "ActiveJoinMessage: " + " -> " + serverID;
  }

  public static AbstractGroupMessage createActiveJoinMessage(ServerID serverID) {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_JOIN, serverID);
  }

  public static AbstractGroupMessage createActiveLeftMessage() {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_LEFT);
  }

  public static AbstractGroupMessage createActiveRequestJoinMessage(ServerID serverID) {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_REQUEST_JOIN, serverID);
  }
}
