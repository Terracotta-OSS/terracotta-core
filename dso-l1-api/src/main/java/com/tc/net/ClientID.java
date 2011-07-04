/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.ChannelID;

import java.io.IOException;
import java.io.Serializable;

/*
 * dev-console needs java serialization, Serializable, of ClientID.
 */
public class ClientID implements NodeID, Serializable {
  private static final int     NULL_NUMBER = -1;

  public static final ClientID NULL_ID     = new ClientID(NULL_NUMBER);
  private long                 id;

  // private ChannelID channelID;

  public ClientID() {
    // To make serialization happy
  }

  // satisfy serialization
  public ClientID(long id) {
    this.id = id;
  }

  public boolean isNull() {
    return NULL_ID.equals(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ClientID) {
      ClientID other = (ClientID) obj;
      return (this.toLong() == other.toLong());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return (int) id;
  }

  @Override
  public String toString() {
    return "ClientID[" + id + "]";
  }

  public ChannelID getChannelID() {
    return new ChannelID(id);
  }

  public long toLong() {
    return id;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readLong();
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(this.id);
  }

  public byte getNodeType() {
    return CLIENT_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    ClientID c = (ClientID) n;
    return (int) (toLong() - c.toLong());
  }

}
