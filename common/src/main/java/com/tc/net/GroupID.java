/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;

import java.io.IOException;
import java.io.Serializable;

/**
 * In active-active, a GroupID identifies a group of servers with one
 * active server and some number of passive servers.  An object resides
 * on a particular group, hence the method ObjectID#getGroupID().
 */
public class GroupID implements NodeID, Serializable {
  private static final int    NULL_NUMBER       = -1;
  private static final int    ALL_GROUPS_NUMBER = Integer.MIN_VALUE;

  public static final GroupID NULL_ID           = new GroupID(NULL_NUMBER);
  public static final GroupID ALL_GROUPS        = new GroupID(ALL_GROUPS_NUMBER);

  private int                 groupNumber;

  public GroupID() {
    groupNumber = NULL_NUMBER;
  }

  public GroupID(int groupNumber) {
    this.groupNumber = groupNumber;
  }

  public final int toInt() {
    return groupNumber;
  }

  public boolean isNull() {
    return (groupNumber == NULL_NUMBER);
  }

  public boolean equals(Object obj) {
    if (obj instanceof GroupID) {
      GroupID other = (GroupID) obj;
      return (this.toInt() == other.toInt());
    }
    return false;
  }

  public int hashCode() {
    return groupNumber;
  }

  public String toString() {
    return "GroupID[" + groupNumber + "]";
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    groupNumber = serialInput.readInt();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeInt(toInt());
  }

  public byte getNodeType() {
    return GROUP_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    GroupID g = (GroupID) o;
    return toInt() - g.toInt();
  }

}
