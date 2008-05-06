/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tx;

import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.net.groups.NodeIDImpl;
import com.tc.net.groups.NodeIDSerializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * A class that represents a particular client transaction from the server's perspective (ie. the combination of NodeID
 * and a client TransactionID)
 */
public class ServerTransactionID implements Comparable {
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
    // TODO::Move this logic away from here
    return (sourceID instanceof NodeIDImpl);
  }

  public boolean isNull() {
    return sourceID.isNull() && txnID.isNull();
  }

  public String toString() {
    return new StringBuffer().append("ServerTransactionID{").append(sourceID).append(',').append(txnID).append('}')
        .toString();
  }

  public int hashCode() {
    return this.hashCode;
  }

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
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(64);
      TCObjectOutputStream tco = new TCObjectOutputStream(baos);
      NodeIDSerializer.writeNodeID(sourceID, tco);
      tco.writeLong(txnID.toLong());
      tco.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

  /**
   * Utility method for deserialization.
   */
  public static ServerTransactionID createFrom(byte[] data) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      TCObjectInputStream tci = new TCObjectInputStream(bais);
      return new ServerTransactionID(NodeIDSerializer.readNodeID(tci), new TransactionID(tci.readLong()));
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  public int compareTo(Object o) {
    ServerTransactionID other = (ServerTransactionID) o;
    int cmp = sourceID.compareTo(other.sourceID);
    if (cmp == 0) {
      return txnID.compareTo(other.txnID);
    } else {
      return cmp;
    }

  }
}
