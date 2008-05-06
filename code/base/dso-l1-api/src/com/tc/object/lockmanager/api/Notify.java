/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

/**
 * Stores info on a cluster-wide notify / notifyAll.
 */
public class Notify implements TCSerializable {

  /** Null instance of Notify */
  public static final Notify NULL = new Notify(true);

  private LockID             lockID;
  private ThreadID           threadID;
  private boolean            all;
  private boolean            initialized;
  private int                hashCode;
  private final boolean      isNull;

  /**
   * New initialized
   * 
   * @param lockID Lock identifier
   * @param threadID Thread identifier
   * @param all Whether notify or notifyAll
   */
  public Notify(LockID lockID, ThreadID threadID, boolean all) {
    isNull = false;
    initialize(lockID, threadID, all);
  }

  /**
   * New uninitialized
   */
  public Notify() {
    isNull = false;
  }

  /**
   * New null instance
   */
  private Notify(boolean isNull) {
    this.isNull = isNull;
  }

  /**
   * @return True if this is the null instance
   */
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

  /**
   * Serialize Notify to output
   * 
   * @param out Output stream
   */
  public void serializeTo(TCByteBufferOutput out) {
    if (!initialized) throw new AssertionError("Attempt to serialize an uninitialized Notify.");
    out.writeString(this.lockID.asString());
    out.writeLong(this.threadID.toLong());
    out.writeBoolean(this.all);
  }

  /**
   * Deserialize Notify from in
   * 
   * @param in Input stream
   * @return This object
   * @throws IOException If error reading in
   */
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
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

  /**
   * @return Thread identfier
   */
  public ThreadID getThreadID() {
    return this.threadID;
  }

  /**
   * @return Lock identifier
   */
  public LockID getLockID() {
    return this.lockID;
  }

  /**
   * @return flag frorm using notify() vs notifyall().
   */
  public boolean getIsAll() {
    return this.all;
  }
}
