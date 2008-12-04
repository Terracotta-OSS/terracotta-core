/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.Assert;

public class ObjectSyncResetMessage extends AbstractGroupMessage {

  public static final int REQUEST_RESET     = 0x00;
  public static final int OPERATION_SUCCESS = 0x01;

  // To make serialization happy
  public ObjectSyncResetMessage() {
    super(-1);
  }

  public ObjectSyncResetMessage(int type) {
    super(type);
  }

  public ObjectSyncResetMessage(int type, MessageID reqID) {
    super(type, reqID);
  }

  protected void basicDeserializeFrom(TCByteBufferInput in) {
    Assert.assertTrue(getType() == REQUEST_RESET || getType() == OPERATION_SUCCESS);
  }

  protected void basicSerializeTo(TCByteBufferOutput out) {
    Assert.assertTrue(getType() == REQUEST_RESET || getType() == OPERATION_SUCCESS);
  }

  public boolean isResetRequest() {
    return getType() == REQUEST_RESET;
  }

}
