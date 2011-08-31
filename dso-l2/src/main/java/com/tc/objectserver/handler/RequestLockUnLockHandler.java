/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.locks.RecallBatchContext;
import com.tc.object.locks.ThreadID;
import com.tc.object.msg.LockRequestMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.locks.LockManager;

import java.util.Collection;
import java.util.LinkedList;

/**
 * Makes the request for a lock on behalf of a client
 * 
 * @author steve
 */
public class RequestLockUnLockHandler extends AbstractEventHandler {
  private LockManager lockManager;

  public void handleEvent(EventContext context) {
    LockRequestMessage lrm = (LockRequestMessage) context;

    LockID lid = lrm.getLockID();
    NodeID cid = lrm.getSourceNodeID();
    ThreadID tid = lrm.getThreadID();

    switch (lrm.getRequestType()) {
      case LOCK:
        lockManager.lock(lid, (ClientID) cid, tid, lrm.getLockLevel());
        return;
      case TRY_LOCK:
        lockManager.tryLock(lid, (ClientID) cid, tid, lrm.getLockLevel(), lrm.getTimeout());
        return;
      case UNLOCK:
        lockManager.unlock(lid, (ClientID) cid, tid);
        return;
      case WAIT:
        lockManager.wait(lid, (ClientID) cid, tid, lrm.getTimeout());
        return;
      case RECALL_COMMIT:
        Collection<ClientServerExchangeLockContext> contexts = lrm.getContexts();
        lockManager.recallCommit(lid, (ClientID) cid, contexts);
        return;
      case QUERY:
        lockManager.queryLock(lid, (ClientID) cid, tid);
        return;
      case INTERRUPT_WAIT:
        lockManager.interrupt(lid, (ClientID) cid, tid);
        return;
      case BATCHED_RECALL_COMMIT:
        LinkedList<RecallBatchContext> recallContexts = lrm.getRecallBatchedContexts();
        for (RecallBatchContext recallContext : recallContexts) {
          Collection<ClientServerExchangeLockContext> lockState = recallContext.getContexts();
          lockManager.recallCommit(recallContext.getLockID(), (ClientID) cid, lockState);
        }
        return;
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.lockManager = oscc.getLockManager();
  }
}
