/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tx;

import java.io.Serializable;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;

/**
 * A class that represents a particular client transaction from the server's perspective (ie. the combination of
 * ChannelID and a client TransactionID)
 */
public class ServerTransactionID implements Serializable {
  public static final ServerTransactionID NULL_ID = new ServerTransactionID(ChannelID.NULL_ID, TransactionID.NULL_ID);

  private final TransactionID             txnID;
  private final ChannelID                 channelID;
  private final int                       hashCode;

  public ServerTransactionID(ChannelID channelID, TransactionID txnID) {
    this.channelID = channelID;
    this.txnID = txnID;

    int hash = 29;
    hash = (37 * hash) + channelID.hashCode();
    hash = (37 * hash) + txnID.hashCode();
    this.hashCode = hash;
  }

  public ChannelID getChannelID() {
    return channelID;
  }

  public TransactionID getClientTransactionID() {
    return txnID;
  }
  
  public boolean isNull() {
    return channelID.isNull() && txnID.isNull();
  }

  public String toString() {
    return new StringBuffer().append("ServerTransactionID{").append(channelID).append(',').append(txnID).append('}')
        .toString();
  }

  public int hashCode() {
    return this.hashCode;
  }

  public boolean equals(Object obj) {
    if (obj instanceof ServerTransactionID) {
      ServerTransactionID other = (ServerTransactionID) obj;
      return this.channelID.equals(other.channelID) && this.txnID.equals(other.txnID);
    }
    return false;
  }
}
