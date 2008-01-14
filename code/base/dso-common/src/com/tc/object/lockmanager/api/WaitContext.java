/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.ClientID;
import com.tc.object.tx.WaitInvocation;
import com.tc.object.tx.WaitInvocationFactory;

import java.io.IOException;

/**
 * Client/Server intermediate format to hold distributed wait(..) invocation information.
 */
public class WaitContext extends LockContext {

  private static final WaitInvocationFactory waitInvocationFactory = new WaitInvocationFactory();

  private WaitInvocation                     waitInvocation;

  public WaitContext(LockID lockID, ClientID cid, ThreadID threadID, int lockLevel, String lockType, WaitInvocation waitInvocation) {
    super(lockID, cid, threadID, lockLevel, lockType);
    this.waitInvocation = waitInvocation;
  }

  public WaitContext() {
    return;
  }

  public WaitInvocation getWaitInvocation() {
    return waitInvocation;
  }

  public void serializeTo(TCByteBufferOutput output) {
    super.serializeTo(output);
    output.writeByte(waitInvocation.getSignature().getArgCount());
    output.writeLong(waitInvocation.getMillis());
    output.writeInt(waitInvocation.getNanos());
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    super.deserializeFrom(input);
    waitInvocation = waitInvocationFactory.newWaitInvocation(input.readByte(), input.readLong(), input.readInt());
    return this;
  }
}
