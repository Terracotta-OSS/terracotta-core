/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
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
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.tx.WaitInvocation;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Iterator;

/**
 * Responsible for communicating to server to request/release locks
 * 
 * @author steve
 */
public class RemoteLockManagerImpl implements RemoteLockManager {

  private LockRequestMessageFactory lockRequestMessageFactory;
  private final ClientGlobalTransactionManager gtxManager;

  public RemoteLockManagerImpl(LockRequestMessageFactory lrmf, ClientGlobalTransactionManager gtxManager) {
    this.lockRequestMessageFactory = lrmf;
    this.gtxManager = gtxManager;
  }

  public void requestLock(LockID lockID, ThreadID threadID, int lockType) {
    Assert.assertTrue(LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeObtainLock(lockID, threadID, lockType);
    req.send();
  }
  
  public void tryRequestLock(LockID lockID, ThreadID threadID, int lockType) {
    Assert.assertTrue(LockLevel.isDiscrete(lockType));
    LockRequestMessage req = createRequest();
    req.initializeTryObtainLock(lockID, threadID, lockType);
    req.send();
  }

  public void releaseLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeLockRelease(lockID, threadID);
    req.send();
  }

  public void releaseLockWait(LockID lockID, ThreadID threadID, WaitInvocation call) {
    LockRequestMessage req = createRequest();
    req.initializeLockReleaseWait(lockID, threadID, call);
    req.send();
  }
  
  public void queryLock(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeQueryLock(lockID, threadID);
    req.send();
  }
  
  public void interrruptWait(LockID lockID, ThreadID threadID) {
    LockRequestMessage req = createRequest();
    req.initializeInterruptWait(lockID, threadID);
    req.send();
  }

  private LockRequestMessage createRequest() {
    // return (LockRequestMessage) channel.createMessage(TCMessageType.LOCK_REQUEST_MESSAGE);
    return lockRequestMessageFactory.newLockRequestMessage();
  }

  public void recallCommit(LockID lockID, Collection lockContext, Collection waitContext, Collection pendingRequests) {
    LockRequestMessage req = createRequest();
    req.initializeLockRecallCommit(lockID);
    for (Iterator i = lockContext.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getChannelID(), request.threadID(), request
          .lockLevel());
      req.addLockContext(ctxt);
    }

    for (Iterator i = waitContext.iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), req.getChannelID(), request.threadID(), request
          .lockLevel(), request.getWaitInvocation());
      req.addWaitContext(ctxt);
    }
    
    for (Iterator i = pendingRequests.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), req.getChannelID(), request.threadID(), request
          .lockLevel());
      req.addPendingLockContext(ctxt);
    }

    req.send();
  }

  public void flush(LockID lockID) {
    gtxManager.flush(lockID);
  }

  public boolean isTransactionsForLockFlushed(LockID lockID, LockFlushCallback callback) {
    return gtxManager.isTransactionsForLockFlushed(lockID, callback);
  }
}