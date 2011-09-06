/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.l2.context.SyncIndexesRequest;
import com.tc.l2.msg.IndexSyncAckMessage;
import com.tc.l2.objectserver.L2IndexStateManager;

public class L2IndexSyncRequestHandler extends AbstractEventHandler {

  private final L2IndexStateManager l2IndexStateManager;

  public L2IndexSyncRequestHandler(L2IndexStateManager l2IndexStateManager) {
    this.l2IndexStateManager = l2IndexStateManager;
  }

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof SyncIndexesRequest) {
      SyncIndexesRequest request = (SyncIndexesRequest) context;
      l2IndexStateManager.initiateIndexSync(request.getNodeID());
    } else if (context instanceof IndexSyncAckMessage) {
      IndexSyncAckMessage ack = (IndexSyncAckMessage) context;
      l2IndexStateManager.receivedAck(ack.messageFrom(), ack.getAmount());
    } else {
      throw new AssertionError("unexpected context: " + context);
    }
  }

}
