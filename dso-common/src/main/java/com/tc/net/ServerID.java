/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.util.Assert;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;

public class ServerID implements NodeID, Serializable {

  private static final long    serialVersionUID = 1L;
  public static final ServerID NULL_ID          = new ServerID("NULL-ID", new byte[0]);
  private static final String  UNINITIALIZED    = "Uninitialized";

  private String               name;
  private byte[]               uid;

  private transient int        hash;

  public ServerID() {
    // satisfy serialization
    this.name = UNINITIALIZED;
  }

  public ServerID(String name, byte[] uid) {
    this.name = name;
    this.uid = uid;
  }

  @Override
  public int hashCode() {
    if (hash != 0) return hash;
    int lhash = 27;
    for (int i = uid.length - 1; i >= 0; i--) {
      lhash = 31 * lhash + uid[i];
    }
    hash = lhash;

    return lhash;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ServerID) {
      ServerID that = (ServerID) o;
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

  @Override
  public String toString() {
    return "NodeID[" + getName() + "]";
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

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

  public byte getNodeType() {
    return SERVER_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    ServerID target = (ServerID) n;
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
