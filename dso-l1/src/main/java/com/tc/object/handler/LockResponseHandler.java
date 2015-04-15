/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
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
        this.lockManager.award(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(),
                               msg.getLockLevel());
        return;
      case RECALL:
        this.lockManager.recall(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getLockLevel(), -1);
        return;
      case RECALL_WITH_TIMEOUT:
        this.lockManager.recall(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getLockLevel(), msg.getAwardLeaseTime());
        return;
      case REFUSE:
        this.lockManager.refuse(msg.getSourceNodeID(), msg.getLocalSessionID(), msg.getLockID(), msg.getThreadID(),
                                msg.getLockLevel());
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
