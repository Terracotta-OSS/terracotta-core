/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.lockmanager.impl;

import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockFlushCallback;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.LockLevel;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.RemoteLockManager;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.lockmanager.api.TryLockContext;
import com.tc.object.lockmanager.api.TryLockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.tx.TimerSpec;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

/**
 * Responsible for communicating to server to request/release locks
 */
public class RemoteLockManagerImpl implements RemoteLockManager {

  private LockRequestMessageFactory            lockRequestMessageFactory;
  private final ClientGlobalTransactionManager gtxManager;

  public RemoteLockManagerImpl(LockRequestMessageFactory lrmf, ClientGlobalTransactionManager gtxManager) {
    this.lockRequestMessageFactory = lrmf;
    this.gtxManager = gtxManager;
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType, String lockObjectType) {
    Assert.assertTrue(LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeObtainLock(lockID, threadID, lockType, lockObjectType);
    send(req);
  }

  // used for tests
  protected void send(LockRequestMessage req) {
    req.send();
  }

  public void tryRequestLock(LockID lockID, ThreadID threadID, TimerSpec timeout, int lockType, String lockObjectType) {
    Assert.assertTrue(LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeTryObtainLock(lockID, threadID, timeout, lockType, lockObjectType);
    send(req);
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeLockRelease(lockID, threadID);
    send(req);
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, TimerSpec call) {
    LockRequestMessage req = createRequest();
    req.initializeLockReleaseWait(lockID, threadID, call);
    send(req);
  }

  public void queryLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeQueryLock(lockID, threadID);
    send(req);
  }

  public void interrruptWait(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeInterruptWait(lockID, threadID);
    send(req);
  }

  private LockRequestMessage createRequest() {
    // return (LockRequestMessage) channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
    return lockRequestMessageFactory.newLockRequestMessage();
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests,
                           Collection pendingTryLockRequests) {
    LockRequestMessage req = createRequest();
    req.initializeLockRecallCommit(lockID);
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getClientID(), request.threadID(), request.lockLevel(), request.lockType());
      req.addLockContext(ctxt);
    }

    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), req.getClientID(), request.threadID(), request.lockLevel(), request.lockType(),
                                         request.getTimerSpec());
      req.addWaitContext(ctxt);
    }

    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getClientID(), request.threadID(), request.lockLevel(), request.lockType());
      req.addPendingLockContext(ctxt);
    }

    for (Iterator i = pendingTryLockRequests.iterator(); i.hasNext();) {
      TryLockRequest request = (TryLockRequest) i.next();
      LockContext ctxt = new TryLockContext(request.lockID(), req.getClientID(), request.threadID(), request
          .lockLevel(), request.lockType(), request.getTimerSpec());
      req.addPendingTryLockContext(ctxt);
    }

    send(req);
  }

  public void flush(LockID lockID) {
    gtxManager.flush(lockID);
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return gtxManager.isTransactionsForLockFlushed(lockID, callback);
  }
}
