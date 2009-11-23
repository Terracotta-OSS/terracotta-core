/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.util.Assert;
import com.tc.util.State;

import java.io.IOException;

/*
 * Exchange StripeID between L2 nodes
 */
public class StripeIDGroupMessage extends AbstractGroupMessage {

  public static final int STRIPEID_MESSAGE = 1;

  private GroupID         groupID;
  private StripeID        stripeID;
  private State           senderState;
  private boolean         remap;

  // To make serialization happy
  public StripeIDGroupMessage() {
    super(-1);
  }

  public StripeIDGroupMessage(int type, GroupID groupID, StripeID stripeID, State senderState, boolean isRemap) {
    super(type);
    this.groupID = groupID;
    this.stripeID = stripeID;
    this.senderState = senderState;
    this.remap = isRemap;
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    groupID = (GroupID) nodeIDSerializer.getNodeID();
    nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    stripeID = (StripeID) nodeIDSerializer.getNodeID();
    senderState = new State(in.readString());
    remap = in.readBoolean();
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    new NodeIDSerializer(groupID).serializeTo(out);
    new NodeIDSerializer(stripeID).serializeTo(out);
    out.writeString(senderState.getName());
    out.writeBoolean(remap);
  }

  public String toString() {
    return "StripeIDGroupMessage [ " + this.stripeID + " " + this.groupID + " " + this.senderState + " isRemap:"
           + this.remap + " ]";
  }

  public GroupID getGroupID() {
    return this.groupID;
  }

  public StripeID getStripeID() {
    return this.stripeID;
  }

  public State getSenderState() {
    return this.senderState;
  }

  public boolean isRemap() {
    return this.remap;
  }

}
