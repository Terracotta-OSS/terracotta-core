/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class GroupID implements NodeID {
  private static final int    UNINITIALIZED = -1;
  private static final int    NULL_NUMBER   = -2;
  public static final GroupID NULL_ID       = new GroupID(NULL_NUMBER);

  private int                 groupNumber;

  public GroupID() {
    // To make serialization happy
    groupNumber = UNINITIALIZED;
  }

  // satisfy serialization
  public GroupID(int groupNumber) {
    this.groupNumber = groupNumber;
  }

  public int getGroupNumber() {
    return groupNumber;
  }

  public boolean isNull() {
    return (groupNumber == UNINITIALIZED);
  }

  public boolean equals(Object obj) {
    if (obj instanceof GroupID) {
      GroupID other = (GroupID) obj;
      return (this.getGroupNumber() == other.getGroupNumber());
    }
    return false;
  }

  public int hashCode() {
    return groupNumber;
  }

  public String toString() {
    return "GroupID[" + groupNumber + "]";
  }

  public void readExternal(ObjectInput in) throws IOException {
    groupNumber = in.readInt();
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    Assert.assertTrue(getGroupNumber() != UNINITIALIZED);
    out.writeInt(getGroupNumber());
  }

  /**
   * FIXME::Two difference serialization mechanisms are implemented since these classes are used with two different
   * implementation of comms stack.
   */
  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    groupNumber = serialInput.readInt();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    Assert.assertTrue(getGroupNumber() != UNINITIALIZED);
    serialOutput.writeInt(getGroupNumber());
  }

  public byte getType() {
    return L2_NODE_TYPE;
  }

  public int compareTo(Object o) {
    GroupID n = (GroupID) o;
    if (getType() != n.getType()) { return getType() - n.getType(); }
    return getGroupNumber() - n.getGroupNumber();
  }

}
