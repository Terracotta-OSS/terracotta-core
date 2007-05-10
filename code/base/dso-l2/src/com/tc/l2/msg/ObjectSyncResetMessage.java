/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.net.groups.AbstractGroupMessage;
import com.tc.net.groups.MessageID;
import com.tc.util.Assert;

import java.io.ObjectInput;
import java.io.ObjectOutput;

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
    super(type,reqID);
  }

  protected void basicReadExternal(int msgType, ObjectInput in) {
    Assert.assertTrue(msgType == REQUEST_RESET || msgType == OPERATION_SUCCESS);
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) {
    Assert.assertTrue(msgType == REQUEST_RESET || msgType == OPERATION_SUCCESS);
  }
  
  public boolean isResetRequest() {
    return getType() == REQUEST_RESET;
  }

}
