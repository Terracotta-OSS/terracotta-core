/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.WaitContext;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ClientHandshakeMessage extends TCMessage {

  public List getTransactionSequenceIDs();

  public Set getObjectIDs();

  public void addLockContext(LockContext ctxt);

  public Collection getLockContexts();

  public void addWaitContext(WaitContext ctxt);

  public Collection getWaitContexts();

  public void addPendingLockContext(LockContext ctxt);

  public void addPendingTryLockContext(TryLockContext ctxt);

  public void setClientVersion(String v);

  public String getClientVersion();

  public Collection getPendingLockContexts();

  public Collection getPendingTryLockContexts();

  public void addTransactionSequenceIDs(List transactionSequenceIDs);

  public void addResentTransactionIDs(List resentTransactionIDs);

  public List getResentTransactionIDs();

  public void setIsObjectIDsRequested(boolean request);

  public boolean isObjectIDsRequested();

  public void setServerHighWaterMark(long serverHighWaterMark);

  public long getServerHighWaterMark();

}