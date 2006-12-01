/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

public class Notify implements TCSerializable {

  public static final Notify NULL = new Notify(true);

  private LockID             lockID;
  private ThreadID           threadID;
  private boolean            all;
  private boolean            initialized;
  private int                hashCode;
  private final boolean      isNull;

  public Notify(LockID lockID, ThreadID threadID, boolean all) {
    isNull = false;
    initialize(lockID, threadID, all);
  }

  public Notify() {
    isNull = false;
  }

  private Notify(boolean isNull) {
    this.isNull = isNull;
  }

  public boolean isNull() {
    return this.isNull;
  }

  private void initialize(LockID l, ThreadID id, boolean isAll) {
    if (initialized) throw new AssertionError("Attempt to initialize twice");
    this.lockID = l;
    this.threadID = id;
    this.all = isAll;
    hashCode = new HashCodeBuilder(5503, 6737).append(lockID).append(threadID).append(isAll).toHashCode();
    initialized = true;
  }

  public void serializeTo(TCByteBufferOutput out) {
    if (!initialized) throw new AssertionError("Attempt to serialize an uninitialized Notify.");
    out.writeString(this.lockID.asString());
    out.writeLong(this.threadID.toLong());
    out.writeBoolean(this.all);
  }

  public Object deserializeFrom(TCByteBufferInputStream in) throws IOException {
    initialize(new LockID(in.readString()), new ThreadID(in.readLong()), in.readBoolean());
    return this;
  }

  public int hashCode() {
    if (!initialized) throw new AssertionError("Called hashCode before initializing.");
    return hashCode;
  }

  public boolean equals(Object o) {
    if (!(o instanceof Notify)) return false;
    Notify cmp = (Notify) o;
    return this.lockID.equals(cmp.lockID) && this.threadID.equals(cmp.threadID) && this.all == cmp.all;
  }

  public String toString() {
    return getClass().getName() + "[" + lockID + ", " + threadID + ", " + "all: " + all + "]";
  }

  public ThreadID getThreadID() {
    return this.threadID;
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public boolean getIsAll() {
    return this.all;
  }
}
