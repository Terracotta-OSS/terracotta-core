/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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

  public byte[] getUID() {
    return uid;
  }

  public String getName() {
    Assert.assertTrue(!this.name.equals(UNINITIALIZED));
    return name;
  }

  @Override
  public String toString() {
    return "NodeID[" + getName() + "]";
  }

  @Override
  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  @Override
  public ServerID deserializeFrom(TCByteBufferInput in) throws IOException {
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

  @Override
  public void serializeTo(TCByteBufferOutput out) {
    Assert.assertTrue(!this.name.equals(UNINITIALIZED));
    out.writeString(this.name);
    int length = this.uid.length;
    out.writeInt(length);
    out.write(this.uid);
  }

  @Override
  public byte getNodeType() {
    return SERVER_NODE_TYPE;
  }

  @Override
  public int compareTo(NodeID n) {
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
