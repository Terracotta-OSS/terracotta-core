/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class NodeIDImpl implements NodeID {

  public static final NodeID  NULL_ID       = new NodeIDImpl("NULL-ID", new byte[0]);

  private static final String UNINITIALIZED = "Uninitialized";

  private String              name;
  private byte[]              uid;

  private transient int       hash;

  public NodeIDImpl() {
    // satisfy serialization
    this.name = UNINITIALIZED;
  }

  public NodeIDImpl(String name, byte[] uid) {
    this.name = name;
    this.uid = uid;
  }

  public int hashCode() {
    if (hash != 0) return hash;
    int lhash = 27;
    for (int i = uid.length - 1; i >= 0; i--) {
      lhash = 31 * lhash + uid[i];
    }
    hash = lhash;

    return lhash;
  }

  public boolean equals(Object o) {
    if (o instanceof NodeIDImpl) {
      NodeIDImpl that = (NodeIDImpl) o;
      return Arrays.equals(that.uid, this.uid);
    }
    return false;
  }

  byte[] getUID() {
    return uid;
  }

  public String getName() {
    Assert.assertTrue(this.name != UNINITIALIZED);
    return name;
  }

  public String toString() {
    return "NodeID[" + getName() + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  /**
   * FIXME::Two difference serialization mechanisms are implemented since these classes are used with two different
   * implementation of comms stack.
   */
  public void readExternal(ObjectInput in) throws IOException {
    this.name = in.readUTF();
    int length = in.readInt();
    this.uid = new byte[length];
    int off = 0;
    while (length > 0) {
      int read = in.read(this.uid, off, length);
      off += read;
      length -= read;
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeUTF(this.name);
    int length = this.uid.length;
    out.writeInt(length);
    out.write(this.uid);
  }

  /**
   * FIXME::Two difference serialization mechanisms are implemented since these classes are used with two different
   * implementation of comms stack.
   */
  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    this.name = in.readString();
    int length = in.readInt();
    this.uid = new byte[length];
    int off = 0;
    while (length > 0) {
      int read = in.read(this.uid, off, length);
      off += read;
      length -= read;
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput out) {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeString(this.name);
    int length = this.uid.length;
    out.writeInt(length);
    out.write(this.uid);
  }

  public byte getType() {
    return L2_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if (getType() != n.getType()) { return getType() - n.getType(); }
    NodeIDImpl target = (NodeIDImpl) n;
    byte[] targetUid = target.getUID();
    int length = uid.length;
    int diff = length - targetUid.length;
    if (diff != 0) return (diff);
    for (int i = 0; i < length; ++i) {
      diff = uid[i] - targetUid[i];
      if (diff != 0) return (diff);
    }
    return 0;
  }
}
