/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.async.api.EventContext;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.ClientID;
import com.tc.net.GroupID;

import java.io.IOException;

public class L1RemovedGroupMessage extends AbstractGroupMessage implements EventContext {

  public static final int L1_REMOVED = 1;

  private ClientID        clientID;
  private GroupID         groupID;

  // To make serialization happy
  public L1RemovedGroupMessage() {
    super(-1);
  }

  public L1RemovedGroupMessage(ClientID clientID, GroupID groupID) {
    super(L1_REMOVED);
    this.clientID = clientID;
    this.groupID = groupID;
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    clientID = (ClientID) nodeIDSerializer.getNodeID();
    nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    groupID = (GroupID) nodeIDSerializer.getNodeID();
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    new NodeIDSerializer(clientID).serializeTo(out);
    new NodeIDSerializer(groupID).serializeTo(out);
  }

  public String toString() {
    return "L1RemovedGroupErrorMessage [ " + this.clientID + " " + this.groupID + " ]";
  }

  public ClientID getClientID() {
    return clientID;
  }

  public GroupID getGroupID() {
    return groupID;
  }

}
