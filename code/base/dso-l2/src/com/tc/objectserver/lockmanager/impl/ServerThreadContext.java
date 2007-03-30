/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.lockmanager.impl;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.ServerThreadID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.util.Assert;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a "open" transaction (ie. one that holds locks). It's only purpose is to hold the proper
 * bookkeeping to do reasonably fast deadlock detection. Some day this might get merged with some of form of higher
 * level server transaction class, but that really doesn't exist yet
 */
class ServerThreadContext {
  static final ServerThreadContext NULL_CONTEXT     = new ServerThreadContext(ServerThreadID.NULL_ID);

  private final static Lock[]      EMPTY_LOCK_ARRAY = new Lock[] {};
  private final boolean            isNull;
  private final Set                locksHeld        = new HashSet();
  private final ServerThreadID     id;
  private ServerThreadContext      cycle;
  private Lock                     waitingOn        = null;
  private final int                hashcode;

  ServerThreadContext(ChannelID channelID, ThreadID threadID) {
    this(new ServerThreadID(channelID, threadID));
  }

  ServerThreadContext(ServerThreadID id2) {
    Assert.assertNotNull(id2);
    this.id = id2;
    this.isNull = ServerThreadID.NULL_ID.equals(id2);
    this.hashcode = this.id.hashCode();
  }

  public String toString() {
    return "ServerThreadContext@" + System.identityHashCode(this) + "[" + id + "](HELD-LOCKS={" + locksHeld
           + "}, WAITING-ON={ " + waitingOn + "})";
  }

  public int hashCode() {
    return this.hashcode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerThreadContext) {
      ServerThreadContext other = (ServerThreadContext) obj;
      return this.id.equals(other.id);
    }
    return false;
  }

  public ServerThreadID getId() {
    return this.id;
  }

  synchronized void addLock(Lock lock) {
    boolean added = locksHeld.add(lock);
    if(!added) {
      throw new AssertionError("Lock : " + lock + " is already held : " + this);
    }
    clearWaitingOn();
  }

  synchronized boolean removeLock(Lock lock) {
    boolean removed = locksHeld.remove(lock);
    if (!removed) { throw new AssertionError(lock
                                             + " : This lock is not held in this ServerThreadContext ! Locks Held = "
                                             + locksHeld); }
    return isClear();
  }

  synchronized boolean isWaiting() {
    return this.waitingOn != null;
  }

  synchronized void setWaitingOn(Lock lock) {
    if (!(this.waitingOn == null || !this.waitingOn.equals(lock))) { throw new AssertionError("Assert Failed : "
                                                                                              + toString()
                                                                                              + " : old = " + waitingOn
                                                                                              + " : new = " + lock); }
    this.waitingOn = lock;
  }

  synchronized boolean clearWaitingOn() {
    this.waitingOn = null;
    return isClear();
  }

  synchronized boolean isClear() {
    return this.waitingOn == null && this.locksHeld.isEmpty();
  }

  synchronized Lock[] getLocksHeld() {
    return (Lock[]) this.locksHeld.toArray(EMPTY_LOCK_ARRAY);
  }

  synchronized Lock getWaitingOn() {
    return this.waitingOn;
  }

  // Deadlock cycle stuff. These methods not syncrhonized, only one thread will ever set/read
  void setCycle(ServerThreadContext other) {
    this.cycle = other;
  }

  // Deadlock cycle stuff. These methods not syncrhonized, only one thread will ever set/read
  ServerThreadContext getCycle() {
    return this.cycle;
  }

  public boolean isNull() {
    return isNull;
  }

}
