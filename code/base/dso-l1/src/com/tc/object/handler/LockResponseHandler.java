/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ClientConfigurationContext;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;

/**
 * @author steve
 */
public class LockResponseHandler extends AbstractEventHandler {
  private static final TCLogger                 logger = TCLogging.getLogger(LockResponseHandler.class);
  private com.tc.object.locks.ClientLockManager lockManager;
  private final SessionManager                  sessionManager;

  public LockResponseHandler(final SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    final LockResponseMessage msg = (LockResponseMessage) context;
    final SessionID sessionID = msg.getLocalSessionID();
    if (!this.sessionManager.isCurrentSession(msg.getSourceNodeID(), sessionID)) {
      logger.warn("Ignoring " + msg + " from a previous session:" + sessionID + ", " + this.sessionManager);
      return;
    }

    switch (msg.getResponseType()) {
      case AWARD:
        this.lockManager.award(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(), msg
            .getLockLevel());
        return;
      case RECALL:
        this.lockManager.recall(msg.getLockID(), msg.getLockLevel(), -1);
        return;
      case RECALL_WITH_TIMEOUT:
        this.lockManager.recall(msg.getLockID(), msg.getLockLevel(), msg.getAwardLeaseTime());
        return;
      case REFUSE:
        this.lockManager.refuse(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(), msg
            .getLockLevel());
        return;
      case WAIT_TIMEOUT:
        this.lockManager.notified(msg.getLockID(), msg.getThreadID());
        return;
      case INFO:
        this.lockManager.info(msg.getLockID(), msg.getThreadID(), msg.getContexts());
        return;
    }
    logger.error("Unknown lock response message: " + msg);
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ClientConfigurationContext ccc = (ClientConfigurationContext) context;
    this.lockManager = ccc.getLockManager();
  }

}
