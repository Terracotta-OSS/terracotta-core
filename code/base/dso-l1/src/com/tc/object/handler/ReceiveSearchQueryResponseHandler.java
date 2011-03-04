/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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
                                                            responseMsg.getResults(), responseMsg.getAggregators(),
                                                            responseMsg.getSourceNodeID(),
                                                            responseMsg.isAnyCriteriaMatched());
      }
    } else {
      throw new AssertionError("Unknown message type received from server - " + context.getClass().getName());
    }
  }
}
