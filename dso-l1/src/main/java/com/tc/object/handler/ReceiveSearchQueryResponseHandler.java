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
import com.tc.async.api.EventContext;
import com.tc.object.RemoteSearchRequestManager;
import com.tc.object.msg.SearchQueryResponseMessage;

public class ReceiveSearchQueryResponseHandler extends AbstractEventHandler {

  private final RemoteSearchRequestManager remoteSearchRequestManager;

  public ReceiveSearchQueryResponseHandler(final RemoteSearchRequestManager remoteSearchRequestManager) {
    this.remoteSearchRequestManager = remoteSearchRequestManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof SearchQueryResponseMessage) {
      final SearchQueryResponseMessage responseMsg = (SearchQueryResponseMessage) context;

      if (responseMsg.isError()) {
        this.remoteSearchRequestManager.addErrorResponseForQuery(responseMsg.getLocalSessionID(),
                                                                 responseMsg.getRequestID(),
                                                                 responseMsg.getGroupIDFrom(),
                                                                 responseMsg.getErrorMessage(),
                                                                 responseMsg.getSourceNodeID());
      } else {
        this.remoteSearchRequestManager.addResponseForQuery(responseMsg.getLocalSessionID(),
                                                            responseMsg.getRequestID(), responseMsg.getGroupIDFrom(),
                                                            responseMsg.getResults(),
                                                            responseMsg.getTotalResultCount(),
                                                            responseMsg.getAggregators(),
                                                            responseMsg.getSourceNodeID(),
                                                            responseMsg.isAnyCriteriaMatched());
      }
    } else {
      throw new AssertionError("Unknown message type received from server - " + context.getClass().getName());
    }
  }
}
