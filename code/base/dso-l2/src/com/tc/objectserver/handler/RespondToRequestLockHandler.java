/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.exception.ImplementMe;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.context.LockResponseContext;
import com.tc.objectserver.core.api.ServerConfigurationContext;

/**
 * @author steve
 */
public class RespondToRequestLockHandler extends AbstractEventHandler {
  private DSOChannelManager channelManager;
  private TCLogger          logger;

  public void handleEvent(EventContext context) {
    LockResponseContext lrc = (LockResponseContext) context;

    ChannelID cid = lrc.getChannelID();
    try {
      MessageChannel channel = channelManager.getChannel(cid);

      LockResponseMessage responseMessage = null;

      if (lrc.isLockAward()) {
        responseMessage = (LockResponseMessage) channel.createMessage(TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockAward(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockNotAwarded()) {
        responseMessage = (LockResponseMessage) channel.createMessage(TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockNotAwarded(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockRecall()) {
        responseMessage = (LockResponseMessage) channel.createMessage(TCMessageType.LOCK_RECALL_MESSAGE);
        responseMessage.initializeLockRecall(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockWaitTimeout()) {
        responseMessage = (LockResponseMessage) channel.createMessage(TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockWaitTimeout(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockInfo()) {
        responseMessage = (LockResponseMessage) channel.createMessage(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE);
        responseMessage.initializeLockInfo(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel(), lrc.getGlobalLockInfo());
      } else {
        // XXX: what kind of response context is this?
        throw new ImplementMe();
      }

      responseMessage.send();

    } catch (NoSuchChannelException e) {
      logger.info("Failed to send lock response message:" + lrc.getLockID().asString() + " to:" + cid
                  + " because the session is dead.");
      return;
    }
  }
  
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.channelManager = oscc.getChannelManager();
    this.logger = oscc.getLogger(this.getClass());
  }

}