/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.ClientID;
import com.tc.object.tx.TimerSpec;
import com.tc.object.tx.TimerSpecFactory;

import java.io.IOException;

/**
 * Client/Server intermediate format to hold distributed wait(..) invocation information.
 */
public class WaitContext extends LockContext {

  private static final TimerSpecFactory waitInvocationFactory = new TimerSpecFactory();

  private TimerSpec                     timerSpec;

  public WaitContext(LockID lockID, ClientID cid, ThreadID threadID, int lockLevel, String lockType, TimerSpec timerSpec) {
    super(lockID, cid, threadID, lockLevel, lockType);
    this.timerSpec = timerSpec;
  }

  public WaitContext() {
    return;
  }

  public TimerSpec getTimerSpec() {
    return timerSpec;
  }

  public void serializeTo(TCByteBufferOutput output) {
    super.serializeTo(output);
    output.writeByte(timerSpec.getSignature().getArgCount());
    output.writeLong(timerSpec.getMillis());
    output.writeInt(timerSpec.getNanos());
  }

  public Object deserializeFrom(TCByteBufferInput input) throws IOException {
    super.deserializeFrom(input);
    timerSpec = waitInvocationFactory.newTimerSpec(input.readByte(), input.readLong(), input.readInt());
    return this;
  }
}
