/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.msg;

import com.tc.async.api.OrderedEventContext;
import com.tc.net.groups.AbstractGroupMessage;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ObjectSyncCompleteMessage extends AbstractGroupMessage implements OrderedEventContext {

  public static final int OBJECT_SYNC_COMPLETE = 0x00;
  private long            sequence;

  // To make serialization happy
  public ObjectSyncCompleteMessage() {
    super(-1);
  }

  public ObjectSyncCompleteMessage(int type, long sequence) {
    super(type);
    this.sequence = sequence;
  }

  protected void basicReadExternal(int msgType, ObjectInput in) throws IOException {
    Assert.assertEquals(OBJECT_SYNC_COMPLETE, msgType);
    this.sequence = in.readLong();
  }

  protected void basicWriteExternal(int msgType, ObjectOutput out) throws IOException {
    Assert.assertEquals(OBJECT_SYNC_COMPLETE, msgType);
    out.writeLong(this.sequence);
  }

  public long getSequenceID() {
    return this.sequence;
  }

}
