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
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import java.io.IOException;

/**
 *
 */
public class ReplicationMessageAck extends AbstractGroupMessage {
  //message types  
  public static final int INVALID               = 0; // Sent to replicate a request on the passive
  public static final int RECEIVED                = 2; // Means that the replicated action has been received by the passive
  public static final int COMPLETED                = 3; // response that the replicated action completed
  public static final int START_SYNC                = 4; // Sent from the passive when it wants the active to start passive sync.

  // Factory methods.
  public static ReplicationMessageAck createSyncRequestMessage() {
    return new ReplicationMessageAck(START_SYNC);
  }

  public static ReplicationMessageAck createReceivedAck(MessageID requestToAck) {
    return new ReplicationMessageAck(RECEIVED, requestToAck);
  }

  public static ReplicationMessageAck createCompletedAck(MessageID requestToAck) {
    return new ReplicationMessageAck(COMPLETED, requestToAck);
  }


  public ReplicationMessageAck() {
    super(INVALID);
  }

//  this type requests passive sync from the active  
  private ReplicationMessageAck(int type) {
    super(type);
  }
  
  private ReplicationMessageAck(int type, MessageID requestID) {
    super(type, requestID);
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    // Do nothing - no instance variables.
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    // Do nothing - no instance variables.
  }
}
