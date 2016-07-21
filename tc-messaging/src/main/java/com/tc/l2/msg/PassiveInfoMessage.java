package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.NodeID;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.NodeIDSerializer;

import java.io.IOException;

/**
 * @author vmad
 */
public class PassiveInfoMessage extends AbstractGroupMessage {

  public static final int PASSIVE_JOIN         = 0x01;
  public static final int PASSIVE_LEFT         = 0x02;
  public static final int PASSIVE_CLEANUP      = 0x03;
  
  private NodeID passiveID;
  
  public PassiveInfoMessage() {
    super(-1);
  }

  public PassiveInfoMessage(int type, NodeID passiveID) {
    super(type);
    this.passiveID = passiveID;
  }
  
  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    passiveID = nodeIDSerializer.getNodeID();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(passiveID);
    nodeIDSerializer.serializeTo(out);
  }

  public NodeID getPassiveID() {
    return passiveID;
  }
  
  @Override
  public String toString() {
    return "PassiveJoinedBroadcastMessage => passive server id: " + passiveID;
  }
  
  public static PassiveInfoMessage createPassiveJoinedMessage(NodeID passiveID) {
    return new PassiveInfoMessage(PASSIVE_JOIN, passiveID);
  }

  public static PassiveInfoMessage createPassiveLeftMessage(NodeID passiveID) {
    return new PassiveInfoMessage(PASSIVE_LEFT, passiveID);
  }

  public static PassiveInfoMessage createPassiveCleanupMessage() {
    return new PassiveInfoMessage(PASSIVE_CLEANUP, null);
  }
}
