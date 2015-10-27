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
import com.tc.io.TCSerializable;
import com.tc.net.GroupID;
import com.tc.net.StripeID;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * helper class to hide the serialization and de-serialization of Map<GroupID, StripeID> implementations from external
 * world.
 */
public class GroupToStripeMapSerializer implements TCSerializable<GroupToStripeMapSerializer> {

  private final Map<GroupID, StripeID> groupToStripeMap;

  public GroupToStripeMapSerializer() {
    groupToStripeMap = new HashMap<GroupID, StripeID>();
  }

  public GroupToStripeMapSerializer(Map<GroupID, StripeID> groupToStripeMap) {
    this.groupToStripeMap = groupToStripeMap;
  }

  public Map<GroupID, StripeID> getMap() {
    return groupToStripeMap;
  }

  @Override
  public GroupToStripeMapSerializer deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int size = serialInput.readInt();
    for (int i = 0; i < size; ++i) {
      NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
      nodeIDSerializer.deserializeFrom(serialInput);
      GroupID gid = (GroupID) nodeIDSerializer.getNodeID();

      nodeIDSerializer = new NodeIDSerializer();
      nodeIDSerializer.deserializeFrom(serialInput);
      StripeID stripeID = (StripeID) nodeIDSerializer.getNodeID();
      groupToStripeMap.put(gid, stripeID);
    }
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(groupToStripeMap.size());
    for (Map.Entry<GroupID, StripeID> entry : groupToStripeMap.entrySet()) {
      new NodeIDSerializer(entry.getKey()).serializeTo(serialOutput);
      new NodeIDSerializer(entry.getValue()).serializeTo(serialOutput);
    }
  }

}
