/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.net.groups.NodeID;
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
    NodeID cid = lrc.getNodeID();

    try {
      LockResponseMessage responseMessage = null;

      if (lrc.isLockAward()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockAward(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockNotAwarded()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockNotAwarded(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockRecall()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RECALL_MESSAGE);
        responseMessage.initializeLockRecall(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel(), lrc.getAwardLeaseTime());
      } else if (lrc.isLockWaitTimeout()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeLockWaitTimeout(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockInfo()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE);
        responseMessage.initializeLockInfo(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel(), lrc
            .getGlobalLockInfo());
      } else {
        throw new AssertionError("Unknown lock response context : " + lrc);
      }

      send(responseMessage);

    } catch (NoSuchChannelException e) {
      logger.info("Failed to send lock response message:" + lrc.getLockID().asString() + " to:" + cid
                  + " because the session is dead.");
      return;
    }
  }

  protected LockResponseMessage createMessage(EventContext context, TCMessageType messageType)
      throws NoSuchChannelException {
    LockResponseContext lrc = (LockResponseContext) context;
    NodeID cid = lrc.getNodeID();
    MessageChannel channel = channelManager.getActiveChannel(cid);
    return (LockResponseMessage) channel.createMessage(messageType);
  }

  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.channelManager = oscc.getChannelManager();
    this.logger = oscc.getLogger(this.getClass());
  }

  //used in tests to by pass the network
  protected void send(LockResponseMessage responseMessage){
    responseMessage.send();
  }
}
