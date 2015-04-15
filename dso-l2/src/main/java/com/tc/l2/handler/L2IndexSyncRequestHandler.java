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
