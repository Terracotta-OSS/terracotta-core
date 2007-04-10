/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.State;

public class GlobalTransactionDescriptor {

  private static final State        INIT            = new State("INIT");
  private static final State        APPLY_COMPLETE  = new State("APPLY_COMPLETE");
  private static final State        COMMIT_COMPLETE = new State("COMMIT_COMPLETE");

  private final ServerTransactionID stxn;
  private final GlobalTransactionID gid;
  private State                     state;

  public GlobalTransactionDescriptor(ServerTransactionID serverTransactionID, GlobalTransactionID gid) {
    this.stxn = serverTransactionID;
    this.gid = gid;
    this.state = INIT;
  }

  public void applyComplete() {
    if (this.state != INIT) { throw new AssertionError("Not in valid state : " + state); }
    this.state = APPLY_COMPLETE;
  }

  public void commitComplete() {
    if (this.state == COMMIT_COMPLETE) { throw new AssertionError("Already commited : " + state); }
    this.state = COMMIT_COMPLETE;
  }

  public boolean isApplied() {
    return this.state != INIT;
  }

  public boolean isCommitted() {
    return this.state == COMMIT_COMPLETE;
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
