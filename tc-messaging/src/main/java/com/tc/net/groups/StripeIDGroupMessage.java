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

  @Override
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

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    new NodeIDSerializer(groupID).serializeTo(out);
    new NodeIDSerializer(stripeID).serializeTo(out);
    out.writeBoolean(isActive);
    out.writeBoolean(remap);
  }

  @Override
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
