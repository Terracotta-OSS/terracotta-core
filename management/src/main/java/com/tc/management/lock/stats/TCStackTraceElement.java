/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.lock.stats;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockIDSerializer;

import java.io.IOException;
import java.io.Serializable;

public class TCStackTraceElement implements TCSerializable, Serializable {
  private LockStatElement lockStatElement;
  private LockID          lockID;
  private int             hashCode;

  public TCStackTraceElement() {
    return;
  }

  public TCStackTraceElement(LockID lockID, LockStatElement lockStatElement) {
    this.lockID = lockID;
    this.lockStatElement = lockStatElement;

    computeHashCode();
  }

  private void computeHashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(5503, 6737);
    hashCodeBuilder.append(lockID.hashCode());
    hashCodeBuilder.append(lockStatElement.hashCode());
    this.hashCode = hashCodeBuilder.toHashCode();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    LockIDSerializer lidsr = new LockIDSerializer();
    this.lockID = ((LockIDSerializer) lidsr.deserializeFrom(serialInput)).getLockID();
    lockStatElement = new LockStatElement();
    lockStatElement.deserializeFrom(serialInput);
    computeHashCode();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    new LockIDSerializer(lockID).serializeTo(serialOutput);
    lockStatElement.serializeTo(serialOutput);
  }

  public LockStatElement getLockStatElement() {
    return this.lockStatElement;
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(lockID);
    sb.append("\n");
    sb.append(lockStatElement.toString());
    sb.append("\n");
    return sb.toString();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof TCStackTraceElement)) { return false; }
    if (this == obj) { return true; }

    TCStackTraceElement so = (TCStackTraceElement) obj;
    if (!this.lockID.equals(so.lockID)) { return false; }
    if (!this.lockStatElement.equals(so.lockStatElement)) { return false; }

    return true;
  }

  public int hashCode() {
    return hashCode;
  }
}
