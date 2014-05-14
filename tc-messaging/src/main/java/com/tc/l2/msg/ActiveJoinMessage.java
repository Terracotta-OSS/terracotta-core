/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.GroupID;
import com.tc.net.ServerID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;

import java.io.IOException;

import com.tc.net.groups.GroupMessage;

public class ActiveJoinMessage extends AbstractGroupMessage {
  public static final int ACTIVE_JOIN         = 0x01;
  public static final int ACTIVE_LEFT         = 0x02;
  public static final int ACTIVE_REQUEST_JOIN = 0x03;

  private GroupID         groupID;
  private ServerID        serverID;

  public ActiveJoinMessage() {
    super(-1);
  }

  public ActiveJoinMessage(int type, GroupID groupID) {
    this(type, groupID, ServerID.NULL_ID);
  }

  public ActiveJoinMessage(int type, GroupID groupID, ServerID serverID) {
    super(type);
    this.groupID = groupID;
    this.serverID = serverID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    groupID = (GroupID) nodeIDSerializer.getNodeID();
    nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    serverID = (ServerID) nodeIDSerializer.getNodeID();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(groupID);
    nodeIDSerializer.serializeTo(out);
    nodeIDSerializer = new NodeIDSerializer(serverID);
    nodeIDSerializer.serializeTo(out);
  }

  public GroupID getGroupID() {
    return groupID;
  }

  public ServerID getServerID() {
    return serverID;
  }

  @Override
  public String toString() {
    return "ActiveJoinMessage: " + groupID + " -> " + serverID;
  }

  public static GroupMessage createActiveJoinMessage(GroupID groupID, ServerID serverID) {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_JOIN, groupID, serverID);
  }

  public static GroupMessage createActiveLeftMessage(GroupID groupID) {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_LEFT, groupID);
  }

  public static GroupMessage createActiveRequestJoinMessage(GroupID groupID, ServerID serverID) {
    return new ActiveJoinMessage(ActiveJoinMessage.ACTIVE_REQUEST_JOIN, groupID, serverID);
  }
}
