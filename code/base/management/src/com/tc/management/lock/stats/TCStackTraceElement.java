/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import org.apache.commons.lang.builder.HashCodeBuilder;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.object.lockmanager.api.LockID;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

public class TCStackTraceElement implements TCSerializable, Serializable {
  private Collection lockStatElements;
  private LockID lockID;
  private int hashCode;

  public TCStackTraceElement() {
    return;
  }

  public TCStackTraceElement(LockID lockID, Collection lockStatElements) {
    this.lockID = lockID;
    this.lockStatElements = lockStatElements;
    
    computeHashCode();
  }
  
  private void computeHashCode() {
    HashCodeBuilder hashCodeBuilder = new HashCodeBuilder(5503, 6737);
    hashCodeBuilder.append(lockID.hashCode());
    for (Iterator i=lockStatElements.iterator(); i.hasNext(); ) {
      LockStatElement lse = (LockStatElement)i.next();
      hashCodeBuilder.append(lse.hashCode());
    }
    this.hashCode = hashCodeBuilder.toHashCode();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.lockID = new LockID(serialInput.readString());
    int length = serialInput.readInt();
    lockStatElements = new ArrayList(length);
    for (int i = 0; i < length; i++) {
      LockStatElement lse = new LockStatElement();
      lse.deserializeFrom(serialInput);
      lockStatElements.add(lse);
    }
    computeHashCode();

    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(lockID.asString());
    serialOutput.writeInt(lockStatElements.size());
    for (Iterator i=lockStatElements.iterator(); i.hasNext(); ) {
      LockStatElement lse = (LockStatElement)i.next();
      lse.serializeTo(serialOutput);
    }
  }
  
  public Collection getLockStatElements() {
    return this.lockStatElements;
  }
  
  public LockID getLockID() {
    return this.lockID;
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(lockID);
    sb.append("\n");
    for (Iterator i=lockStatElements.iterator(); i.hasNext(); ) {
      LockStatElement lse = (LockStatElement)i.next();
      sb.append(lse.toString());
      sb.append("\n");
    }
    return sb.toString();
  }
  
  public boolean equals(Object obj) {
    if (!(obj instanceof TCStackTraceElement)) { return false; }
    if (this == obj) { return true; }

    TCStackTraceElement so = (TCStackTraceElement) obj;
    if (!this.lockID.equals(so.lockID)) { return false; }
    if (this.lockStatElements.size() != so.lockStatElements.size()) { return false; }
    
    Iterator j=so.lockStatElements.iterator();
    for (Iterator i=lockStatElements.iterator(); i.hasNext(); ) {
      LockStatElement lse = (LockStatElement)i.next();
      LockStatElement lse2 = (LockStatElement)j.next();
      if (!lse.equals(lse2)) { return false; }
    }
    return true;
  }

  public int hashCode() {
    return hashCode;
  }
}
