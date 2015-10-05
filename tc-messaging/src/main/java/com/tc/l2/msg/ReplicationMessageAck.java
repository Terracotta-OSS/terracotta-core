/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.MessageID;
import java.io.IOException;

/**
 *
 */
public class ReplicationMessageAck extends ReplicationMessage {

  public ReplicationMessageAck() {
  }
  
  public ReplicationMessageAck(MessageID requestID) {
    super(requestID);
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {

  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {

  }
}
