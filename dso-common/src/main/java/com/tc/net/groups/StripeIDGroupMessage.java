/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.GroupID;
import com.tc.net.StripeID;
import com.tc.util.Assert;

import java.io.IOException;

/*
 * Exchange StripeID between L2 nodes
 */
public class StripeIDGroupMessage extends AbstractGroupMessage {

  public static final int STRIPEID_MESSAGE = 1;

  private GroupID         groupID;
  private StripeID        stripeID;
  private boolean         isActive;
  private boolean         remap;

  // To make serialization happy
  public StripeIDGroupMessage() {
    super(-1);
  }

  public StripeIDGroupMessage(int type, GroupID groupID, StripeID stripeID, boolean isActive, boolean isRemap) {
    super(type);
    this.groupID = groupID;
    this.stripeID = stripeID;
    this.isActive = isActive;
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
    isActive = in.readBoolean();
    remap = in.readBoolean();
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    new NodeIDSerializer(groupID).serializeTo(out);
    new NodeIDSerializer(stripeID).serializeTo(out);
    out.writeBoolean(isActive);
    out.writeBoolean(remap);
  }

  public String toString() {
    return "StripeIDGroupMessage [ " + this.stripeID + " " + this.groupID + " isActive: " + this.isActive + " isRemap:"
           + this.remap + " ]";
  }

  public GroupID getGroupID() {
    return this.groupID;
  }

  public StripeID getStripeID() {
    return this.stripeID;
  }

  public boolean isActive() {
    return this.isActive;
  }

  public boolean isRemap() {
    return this.remap;
  }

}
