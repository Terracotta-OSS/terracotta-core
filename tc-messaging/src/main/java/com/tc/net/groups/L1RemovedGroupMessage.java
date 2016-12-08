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
import com.tc.net.ClientID;

import java.io.IOException;

public class L1RemovedGroupMessage extends AbstractGroupMessage {

  public static final int L1_REMOVED = 1;

  private ClientID        clientID;

  // To make serialization happy
  public L1RemovedGroupMessage() {
    super(-1);
  }

  public L1RemovedGroupMessage(ClientID clientID) {
    super(L1_REMOVED);
    this.clientID = clientID;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
    clientID = (ClientID) nodeIDSerializer.getNodeID();
    nodeIDSerializer = new NodeIDSerializer();
    nodeIDSerializer.deserializeFrom(in);
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    new NodeIDSerializer(clientID).serializeTo(out);
  }

  @Override
  public String toString() {
    return "L1RemovedGroupErrorMessage [ " + this.clientID + " ]";
  }

  public ClientID getClientID() {
    return clientID;
  }
}
