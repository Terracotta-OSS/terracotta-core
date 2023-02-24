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
package com.tc.object.tx;

import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.bytes.TCReference;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.groups.NodeIDSerializer;
import com.tc.util.Assert;

/**
 * A class that represents a particular client transaction from the server's perspective (ie. the combination of NodeID
 * and a client TransactionID)
 */
public class ServerTransactionID implements Comparable<ServerTransactionID> {
  public static final ServerTransactionID NULL_ID = new ServerTransactionID(ClientID.NULL_ID, TransactionID.NULL_ID);

  private final TransactionID             txnID;
  private final NodeID                    sourceID;
  private final int                       hashCode;

  public ServerTransactionID(NodeID source, TransactionID txnID) {
    this.sourceID = source;
    this.txnID = txnID;

    int hash = 29;
    hash = (37 * hash) + source.hashCode();
    hash = (37 * hash) + txnID.hashCode();
    this.hashCode = hash;
  }

  public NodeID getSourceID() {
    return sourceID;
  }

  public TransactionID getClientTransactionID() {
    return txnID;
  }

  public boolean isServerGeneratedTransaction() {
    return (sourceID.getNodeType() == NodeID.SERVER_NODE_TYPE);
  }

  public boolean isNull() {
    return sourceID.isNull() && txnID.isNull();
  }

  @Override
  public String toString() {
    return new StringBuffer().append("ServerTransactionID{").append(sourceID).append(',').append(txnID).append('}')
        .toString();
  }

  @Override
  public int hashCode() {
    return this.hashCode;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ServerTransactionID) {
      ServerTransactionID other = (ServerTransactionID) obj;
      return this.sourceID.equals(other.sourceID) && this.txnID.equals(other.txnID);
    }
    return false;
  }

  /**
   * Utility method for serialization.
   */
  public byte[] getBytes() {
    TCByteBufferOutputStream out = new TCByteBufferOutputStream(64, 256);
    NodeIDSerializer nodeIDSerializer = new NodeIDSerializer(sourceID);
    nodeIDSerializer.serializeTo(out);
    out.writeLong(txnID.toLong());
    out.close();
    byte[] toRet = new byte[out.getBytesWritten()];
    int idx = 0;
    try (TCReference ref = out.accessBuffers()) {
      for (TCByteBuffer buf : ref) {
        int length = buf.limit();
        buf.get(toRet, idx, buf.limit());
        idx += length;
      }
      Assert.assertEquals(idx, out.getBytesWritten());
    }
    return toRet;
  }

  /**
   * Utility method for deserialization.
   */
  @SuppressWarnings("resource")
  public static ServerTransactionID createFrom(byte[] data) {
    try {
      TCByteBufferInputStream in = new TCByteBufferInputStream(TCByteBufferFactory.wrap(data));
      NodeIDSerializer nodeIDSerializer = new NodeIDSerializer();
      nodeIDSerializer.deserializeFrom(in);
      return new ServerTransactionID(nodeIDSerializer.getNodeID(), new TransactionID(in.readLong()));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  @Override
  public int compareTo(ServerTransactionID other) {
    int cmp = sourceID.compareTo(other.sourceID);
    if (cmp == 0) {
      return txnID.compareTo(other.txnID);
    } else {
      return cmp;
    }
  }

}
