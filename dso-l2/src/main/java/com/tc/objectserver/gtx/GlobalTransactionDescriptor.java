/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.gtx;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.tx.ServerTransactionID;
import com.tc.object.tx.TransactionID;
import com.tc.util.State;

public class GlobalTransactionDescriptor {

  private static final State        INIT            = new State("INIT");
  private static final State        APPLY_INITIATED = new State("APPLY_INITIATED");
  private static final State        COMMIT_COMPLETE = new State("COMMIT_COMPLETE");

  private final ServerTransactionID stxn;
  private final GlobalTransactionID gid;
  private volatile State            state;

  public GlobalTransactionDescriptor(ServerTransactionID serverTransactionID, GlobalTransactionID gid) {
    this.stxn = serverTransactionID;
    this.gid = gid;
    this.state = INIT;
  }

  public void saveStateFrom(GlobalTransactionDescriptor old) {
    this.state = old.state;
  }

  public void commitComplete() {
    if (this.state == COMMIT_COMPLETE) { throw new AssertionError("Already commited : " + this + " state = " + state); }
    this.state = COMMIT_COMPLETE;
  }

  public boolean initiateApply() {
    boolean toInitiate = (this.state == INIT);
    if (toInitiate) {
      this.state = APPLY_INITIATED;
    }
    return toInitiate;
  }

  public boolean isCommitted() {
    return this.state == COMMIT_COMPLETE;
  }

  public String toString() {
    return "GlobalTransactionDescriptor[" + stxn + "," + gid + "," + state + "]";
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

  public boolean complete() {
    return (state == COMMIT_COMPLETE);
  }
}
