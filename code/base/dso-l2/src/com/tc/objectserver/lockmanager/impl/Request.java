/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.async.api.Sink;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;

public class Request {
  private final ServerThreadContext threadContext;
  private final Sink                lockResponseSink;
  private final ChannelID           channelID;
  private final ThreadID            threadID;
  private long                      timestamp;
  private final int                 lockLevel;
  private final int                 hashcode;

  /**
   * Create a new lock response
   * 
   * @param threadContext the open transaction associated with this request
   * @param lockLevel the lock level that will be in lock response message to the client
   * @param lockResponseSink the sink that accepts the lock response events
   */
  public Request(ServerThreadContext txn, int lockLevel, Sink lockResponseSink) {
    this.timestamp = System.currentTimeMillis();
    ServerThreadID id = txn.getId();

    this.channelID = id.getChannelID();
    this.threadID = id.getClientThreadID();
    this.threadContext = txn;
    this.lockResponseSink = lockResponseSink;
    this.lockLevel = lockLevel;
    this.hashcode = txn.hashCode();
  }

  public ChannelID getRequesterID() {
    return this.channelID;
  }

  public ThreadID getSourceID() {
    return this.threadID;
  }

  public long getTimestamp() {
    return timestamp;
  }

  public int getLockLevel() {
    return lockLevel;
  }

  void execute(LockID id) {
    lockResponseSink.add(Lock.createLockAwardResponseContext(id, threadContext.getId(), lockLevel));
  }

  ServerThreadContext getThreadContext() {
    return this.threadContext;
  }

  public int hashCode() {
    return hashcode;
  }

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o instanceof Request) {
      Request other = (Request) o;
      return (this.threadContext.equals(other.threadContext) && (this.lockLevel == other.lockLevel));
    }
    return false;
  }

  Sink getLockResponseSink() {
    return this.lockResponseSink;
  }

  public String toString() {
    return "Request" + "@" + System.identityHashCode(this) + "[" + channelID + "," + threadID + ",level="
           + LockLevel.toString(lockLevel) + "]";
  }

}