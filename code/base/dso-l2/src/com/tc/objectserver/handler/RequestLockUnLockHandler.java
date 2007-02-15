/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.impl.NullSink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.lockmanager.api.ThreadID;
import com.tc.object.msg.LockRequestMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.lockmanager.api.LockManager;

/**
 * Makes the request for a lock on behalf of a client
 * 
 * @author steve
 */
public class RequestLockUnLockHandler extends AbstractEventHandler {
  private static final TCLogger logger    = TCLogging.getLogger(RequestLockUnLockHandler.class);
  public static final Sink      NULL_SINK = new NullSink();

  private LockManager           lockManager;
  private Sink                  lockResponseSink;

  public void handleEvent(EventContext context) {
    LockRequestMessage lrm = (LockRequestMessage) context;

    LockID lid = lrm.getLockID();
    ChannelID cid = lrm.getChannelID();
    ThreadID tid = lrm.getThreadID();
    if (lrm.isObtainLockRequest()) {
      lockManager.requestLock(lid, cid, tid, lrm.getLockLevel(), lockResponseSink);
    } else if (lrm.isTryObtainLockRequest()) {
      lockManager.tryRequestLock(lid, cid, tid, lrm.getLockLevel(), lockResponseSink);
    } else if (lrm.isReleaseLockRequest()) {
      if (lrm.isWaitRelease()) {
        lockManager.wait(lid, cid, tid, lrm.getWaitInvocation(), lockResponseSink);
      } else {
        lockManager.unlock(lid, cid, tid);
      }
    } else if (lrm.isRecallCommitLockRequest()) {
      lockManager.recallCommit(lid, cid, lrm.getLockContexts(), lrm.getWaitContexts(), lrm.getPendingLockContexts(),
                               lockResponseSink);
    } else if (lrm.isQueryLockRequest()) {
      lockManager.queryLock(lid, cid, tid, lockResponseSink);
    } else if (lrm.isInterruptWaitRequest()) {
      lockManager.interrupt(lid, cid, tid);
    } else {
      logger.error("Unknown lock request message: " + lrm);
    }
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.lockManager = oscc.getLockManager();
    this.lockResponseSink = oscc.getStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE).getSink();
  }
}