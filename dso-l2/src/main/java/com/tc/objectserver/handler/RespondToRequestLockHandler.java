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
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.ThreadID;
import com.tc.object.locks.ServerLockContext.State;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.locks.LockResponseContext;

import java.util.Collection;
import java.util.Iterator;

/**
 * @author steve
 */
public class RespondToRequestLockHandler extends AbstractEventHandler {
  private DSOChannelManager channelManager;
  private TCLogger          logger;

  @Override
  public void handleEvent(EventContext context) {
    LockResponseContext lrc = (LockResponseContext) context;
    NodeID cid = lrc.getNodeID();

    try {
      LockResponseMessage responseMessage = null;

      if (lrc.isLockAward()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeAward(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockNotAwarded()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeRefuse(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockRecall()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RECALL_MESSAGE);
        responseMessage.initializeRecallWithTimeout(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel(), lrc
            .getAwardLeaseTime());
      } else if (lrc.isLockWaitTimeout()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_RESPONSE_MESSAGE);
        responseMessage.initializeWaitTimeout(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());
      } else if (lrc.isLockInfo()) {
        responseMessage = createMessage(context, TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE);
        responseMessage.initializeLockInfo(lrc.getLockID(), lrc.getThreadID(), lrc.getLockLevel());

        Collection<ClientServerExchangeLockContext> list = lrc.getGlobalLockInfo();
        for (Iterator<ClientServerExchangeLockContext> iter = list.iterator(); iter.hasNext();) {
          responseMessage.addContext(iter.next());
        }

        for (int i = 0; i < lrc.getNumberOfPendingRequests(); i++) {
          responseMessage.addContext(new ClientServerExchangeLockContext(lrc.getLockID(), ClientID.NULL_ID,
                                                                         ThreadID.NULL_ID, State.PENDING_READ));
        }

      } else {
        throw new AssertionError("Unknown lock response context : " + lrc);
      }

      send(responseMessage);

    } catch (NoSuchChannelException e) {
      logger.info("Failed to send lock message:" + lrc + " to:" + cid + " because the session is dead.");
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

  @Override
  public void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.channelManager = oscc.getChannelManager();
    this.logger = oscc.getLogger(this.getClass());
  }

  // used in tests to by pass the network
  protected void send(LockResponseMessage responseMessage) {
    responseMessage.send();
  }
}
