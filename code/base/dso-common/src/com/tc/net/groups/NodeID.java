/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.util.Assert;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

public class NodeID implements Externalizable {

  public static final NodeID  NULL_ID       = new NodeID("NULL-ID", new byte[0]);

  private static final String UNINITIALIZED = "Uninitialized";

  private String              name;
  private byte[]              uid;
  private byte[]              instanceID;

  private transient int       hash;

  public NodeID() {
    // satisfy serialization
    this.name = UNINITIALIZED;
  }

  public NodeID(String name, byte[] uid) {
    this(name, uid, uid);
  }

  public NodeID(String name, byte[] uid, byte[] instanceID) {
    this.name = name;
    this.uid = uid;
    this.instanceID = instanceID;
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
    if (o instanceof NodeID) {
      NodeID that = (NodeID) o;
      return Arrays.equals(that.uid, this.uid);
    }
    return false;
  }

  public byte[] getUID() {
    return uid;
  }
  
  public byte[] getInstanceID() {
    return instanceID;
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

  public void readExternal(ObjectInput in) throws IOException {
    this.name = in.readUTF();
    int length = in.readInt();
    this.uid = new byte[length];
    for (int i = length - 1; i >= 0; i--) {
      uid[i] = in.readByte();
    }
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    Assert.assertTrue(this.name != UNINITIALIZED);
    out.writeUTF(this.name);
    int length = this.uid.length;
    out.writeInt(length);
    for (int i = length - 1; i >= 0; i--) {
      out.writeByte(this.uid[i]);
    }
  }

  /**
   * HACK::FIXME::TODO This method is a quick hack to brick NodeIDs to ChannelIDs. This mapping is only valid for the
   * current VM. The ChannelIDs are given out in the range -100 to Integer.MIN_VALUE to not clash with the regular
   * client channelID. This definitely needs some cleanup
   */
  public ChannelID toChannelID() {
    return NodeIDChannelIDConverter.getChannelIDFor(this);
  }
}
