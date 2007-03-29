/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;

public class GlobalTransactionDescriptor {
  private final ServerTransactionID stxn;
  private final GlobalTransactionID gid;
  private boolean committed = false;

  public GlobalTransactionDescriptor(ServerTransactionID serverTransactionID, GlobalTransactionID gid) {
    this.stxn = serverTransactionID;
    this.gid = gid;
  }
  
  public void commitComplete() {
    committed = true;
  }
  
  public boolean isCommitted() {
    return committed;
  }

  public String toString() {
    return "GlobalTransactionDescriptor[" + stxn + "," + gid + "]";
  }

  public ChannelID getChannelID() {
    return stxn.getChannelID();
  }

  public TransactionID getClientTransactionID() {
    return stxn.getClientTransactionID();
  }

  public int hashCode() {
    return (37 * stxn.hashCode()) + gid.hashCode();
  }

  public boolean equals(Object o) {
    if (o == null) return false;
    if (!(o instanceof GlobalTransactionDescriptor)) return false;
    if (o == this) return true;
    GlobalTransactionDescriptor c = (GlobalTransactionDescriptor) o;
    return this.stxn.equals(c.stxn) && this.gid.equals(c.gid);
  }

  public ServerTransactionID getServerTransactionID() {
    return stxn;
  }

  public GlobalTransactionID getGlobalTransactionID() {
    return gid;
  }
}
