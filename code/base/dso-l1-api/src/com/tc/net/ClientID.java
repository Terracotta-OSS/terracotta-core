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

public class ClientID implements NodeID, Serializable {

  public static final ClientID NULL_ID = new ClientID(ChannelID.NULL_ID);

  private ChannelID            channelID;

  public ClientID() {
    // To make serialization happy
  }

  // satisfy serialization
  public ClientID(ChannelID channelID) {
    this.channelID = channelID;
  }

  public boolean isNull() {
    return channelID.isNull();
  }

  public boolean equals(Object obj) {
    if (obj instanceof ClientID) {
      ClientID other = (ClientID) obj;
      return this.channelID.equals(other.channelID);
    }
    return false;
  }

  public int hashCode() {
    return channelID.hashCode();
  }

  public String toString() {
    return "ClientID[" + channelID.toLong() + "]";
  }

  public ChannelID getChannelID() {
    return channelID;
  }
  
  public long toLong() {
    return channelID.toLong();
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.channelID = new ChannelID(serialInput.readLong());
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(this.channelID.toLong());
  }

  public byte getNodeType() {
    return CLIENT_NODE_TYPE;
  }

  public int compareTo(Object o) {
    NodeID n = (NodeID) o;
    if(getNodeType() != n.getNodeType()) {
      return getNodeType() - n.getNodeType();
    }
    ClientID c = (ClientID) n;
    return this.channelID.compareTo(c.channelID);
  }

}
