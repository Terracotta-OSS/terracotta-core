/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.objectserver.lockmanager.api.LockAwardContext;
import com.tc.util.Assert;

public class Holder implements LockAwardContext {
  private final ServerThreadContext threadContext;
  private final LockID              lockID;
  private final ChannelID           channelID;
  private final ThreadID            threadID;
  private final long                timeout;
  private final long                timestamp;
  private int                       lockLevel;
  private Sink                      sink;

  public Holder(LockID lockID, ServerThreadContext txn, long timeout) {
    this.timestamp = System.currentTimeMillis();
    this.lockID = lockID;
    this.threadContext = txn;
    this.channelID = txn.getId().getChannelID();
    this.threadID = txn.getId().getClientThreadID();
    this.timeout = timeout;
    this.lockLevel = LockLevel.NIL_LOCK_LEVEL;
  }

  public synchronized boolean isUpgrade() {
    return LockLevel.isRead(lockLevel) && LockLevel.isWrite(lockLevel);
  }

  synchronized int addLockLevel(int level) {
    if (LockLevel.isGreedy(level)) {
      Assert.assertEquals(getThreadID(), ThreadID.VM_ID);
      level = LockLevel.makeNotGreedy(level);
    }
    return this.lockLevel |= level;
  }

  synchronized int removeLockLevel(int level) {
    return this.lockLevel ^= level;
  }

  public synchronized int getLockLevel() {
    return this.lockLevel;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public LockID getLockID() {
    return lockID;
  }

  public long getTimeout() {
    return this.timeout;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public boolean isGreedy() {
    return (getThreadID().equals(ThreadID.VM_ID));
  }

  public long getTimestamp() {
    return timestamp;
  }

  public String toString() {
    return "Holder" + "@" + System.identityHashCode(this) + "[" + channelID + "," + threadID + ",level="
           + LockLevel.toString(getLockLevel()) + ",timeout=" + timeout + "]";
  }

  ServerThreadContext getThreadContext() {
    return threadContext;
  }

  public void setSink(Sink lockResponseSink) {
    Assert.assertTrue(isGreedy());
    sink = lockResponseSink;
  }

  public Sink getSink() {
    Assert.assertTrue(isGreedy());
    return sink;
  }
}