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
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.StripeID;
import com.tc.util.Assert;

import java.io.IOException;

/*
 * Exchange StripeID between L2 nodes
 */
public class StripeIDGroupMessage extends AbstractGroupMessage {

  public static final int STRIPEID_MESSAGE = 1;

  private StripeID        stripeID;
  private boolean         isActive;
  private boolean         remap;

  // To make serialization happy
  public StripeIDGroupMessage() {
    super(-1);
  }

  public StripeIDGroupMessage(int type, StripeID stripeID, boolean isActive, boolean isRemap) {
    super(type);
    this.stripeID = stripeID;
    this.isActive = isActive;
    this.remap = isRemap;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    stripeID = (StripeID) nodeIDSerializer.getNodeID();
    isActive = in.readBoolean();
    remap = in.readBoolean();
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertEquals(STRIPEID_MESSAGE, getType());
    new NodeIDSerializer(stripeID).serializeTo(out);
    out.writeBoolean(isActive);
    out.writeBoolean(remap);
  }

  @Override
  public String toString() {
    return "StripeIDGroupMessage [ " + this.stripeID + " isActive: " + this.isActive + " isRemap:"
           + this.remap + " ]";
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
