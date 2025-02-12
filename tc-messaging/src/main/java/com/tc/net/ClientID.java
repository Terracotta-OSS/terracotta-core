/*
 *  Copyright Terracotta, Inc.
 *  Copyright IBM Corp. 2024, 2025
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
  private static final long    serialVersionUID = 1L;

  private static final int     NULL_NUMBER      = -1;

  public static final ClientID NULL_ID          = new ClientID(NULL_NUMBER);
  private long                 id;

  // private ChannelID channelID;

  public ClientID() {
    // To make serialization happy
  }

  // satisfy serialization
  public ClientID(long id) {
    this.id = id;
  }

  @Override
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

  @Override
  public ClientID deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    this.id = serialInput.readLong();
    return this;
  }

  @Override
  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeLong(this.id);
  }

  @Override
  public byte getNodeType() {
    return CLIENT_NODE_TYPE;
  }

  @Override
  public int compareTo(NodeID n) {
    if (getNodeType() != n.getNodeType()) { return getNodeType() - n.getNodeType(); }
    ClientID c = (ClientID) n;
    return (int) (toLong() - c.toLong());
  }

  public static ClientID readFrom(TCByteBufferInput serialInput) throws IOException {
    return new ClientID(serialInput.readLong());
  }
}
