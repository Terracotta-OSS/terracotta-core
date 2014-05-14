/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;

/**
 * Stores info on a cluster-wide notify / notifyAll.
 */
public class NotifyImpl implements Notify {

  /** Null instance of Notify */
  public static final Notify NULL = new NotifyImpl(true);

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
  public NotifyImpl(LockID lockID, ThreadID threadID, boolean all) {
    isNull = false;
    initialize(lockID, threadID, all);
  }

  /**
   * New uninitialized
   */
  public NotifyImpl() {
    isNull = false;
  }

  /**
   * New null instance
   */
  private NotifyImpl(boolean isNull) {
    this.isNull = isNull;
  }

  /**
   * @return True if this is the null instance
   */
  @Override
  public boolean isNull() {
    return this.isNull;
  }

  private void initialize(LockID l, ThreadID id, boolean isAll) {
    if (initialized) throw new AssertionError("Attempt to initialize twice");
    this.lockID = l;
    this.threadID = id;
    this.all = isAll;
    this.hashCode = calculateHashCode();
    this.initialized = true;
  }

  private int calculateHashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (all ? 1231 : 1237);
    result = prime * result + ((lockID == null) ? 0 : lockID.hashCode());
    result = prime * result + ((threadID == null) ? 0 : threadID.hashCode());
    return result;
  }

  /**
   * Serialize Notify to output
   * 
   * @param out Output stream
   */
  @Override
  public void serializeTo(TCByteBufferOutput out) {
    if (!initialized) throw new AssertionError("Attempt to serialize an uninitialized Notify.");
    new LockIDSerializer(lockID).serializeTo(out);
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
  @Override
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    LockIDSerializer lidsr = new LockIDSerializer();
    LockID lid = ((LockIDSerializer) lidsr.deserializeFrom(in)).getLockID();
    initialize(lid, new ThreadID(in.readLong()), in.readBoolean());
    return this;
  }

  @Override
  public int hashCode() {
    if (!initialized) throw new AssertionError("Called hashCode before initializing.");
    return hashCode;
  }


  @Override
  public boolean equals(Object o) {
    if (!(o instanceof NotifyImpl)) return false;
    NotifyImpl cmp = (NotifyImpl) o;
    return this.lockID.equals(cmp.lockID) && this.threadID.equals(cmp.threadID) && this.all == cmp.all;
  }

  @Override
  public String toString() {
    return getClass().getName() + "[" + lockID + ", " + threadID + ", " + "all: " + all + "]";
  }


  /**
   * @return Thread identfier
   */
  @Override
  public ThreadID getThreadID() {
    return this.threadID;
  }

  /**
   * @return Lock identifier
   */
  @Override
  public LockID getLockID() {
    return this.lockID;
  }

  /**
   * @return flag frorm using notify() vs notifyall().
   */
  @Override
  public boolean getIsAll() {
    return this.all;
  }
}
