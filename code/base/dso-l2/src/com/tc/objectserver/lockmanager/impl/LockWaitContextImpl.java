/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.tx.WaitInvocation;
import com.tc.objectserver.lockmanager.api.LockWaitContext;
import com.tc.util.Assert;

public class LockWaitContextImpl implements LockWaitContext {
  private final ServerThreadContext threadContext;
  private final ChannelID           channelID;
  private final ThreadID            threadID;
  private final Sink                lockResponseSink;
  private final WaitInvocation      call;
  private final Lock                lock;
  private long                      timestamp;
  private final int                 lockLevel;

  public LockWaitContextImpl(ServerThreadContext threadContext, Lock lock, WaitInvocation call, int lockLevel,
                             Sink lockResponseSink) {
    this.lockLevel = lockLevel;
    Assert.assertNoNullElements(new Object[] { threadContext, lock, call, lockResponseSink });
    this.timestamp = System.currentTimeMillis();
    ServerThreadID id = threadContext.getId();
    this.threadContext = threadContext;
    this.channelID = id.getChannelID();
    this.threadID = id.getClientThreadID();
    this.lockResponseSink = lockResponseSink;
    this.call = call;
    this.lock = lock;
  }

  public int hashCode() {
    return this.threadContext.hashCode();
  }

  public boolean equals(Object obj) {
    if (obj instanceof LockWaitContextImpl) {
      LockWaitContextImpl other = (LockWaitContextImpl) obj;
      return this.threadContext.equals(other.threadContext);
    }
    return false;
  }

  public String toString() {
    return "LockWaitContex@" + System.identityHashCode(this) + "[" + channelID + "," + threadID + "," + call + ","
           + lock.getLockID() + "]";
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public ThreadID getThreadID() {
    return threadID;
  }

  public ServerThreadContext getThreadContext() {
    return threadContext;
  }

  public WaitInvocation getWaitInvocation() {
    return this.call;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public boolean wasUpgrade() {
    return lockLevel == (LockLevel.READ | LockLevel.WRITE);
  }

  public int lockLevel() {
    return lockLevel;
  }

  Lock getLock() {
    return lock;
  }

  public Sink getLockResponseSink() {
    return lockResponseSink;
  }

  public void waitTimeout() {
    this.lock.waitTimeout(this);
  }

}
