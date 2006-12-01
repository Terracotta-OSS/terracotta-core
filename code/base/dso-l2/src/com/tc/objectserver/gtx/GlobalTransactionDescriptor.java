/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

// TODO:: Change name -- we dont even need this anymore
public class GlobalTransactionDescriptor implements Serializable {
  private final ServerTransactionID stxn;

  public GlobalTransactionDescriptor(ServerTransactionID serverTransactionID) {
    this.stxn = serverTransactionID;
  }

  public String toString() {
    return "GlobalTransactionDescriptor[" + stxn +  "]";
  }

  public ChannelID getChannelID() {
    return stxn.getChannelID();
  }

  public TransactionID getClientTransactionID() {
    return stxn.getClientTransactionID();
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }

  public int hashCode() {
    return stxn.hashCode();
  }

  public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof GlobalTransactionDescriptor)) return false;
    if (o == this) return true;
    GlobalTransactionDescriptor c = (GlobalTransactionDescriptor) o;
    return this.stxn.equals(c.stxn);
  }

  public ServerTransactionID getServerTransactionID() {
    return stxn;
  }
}
