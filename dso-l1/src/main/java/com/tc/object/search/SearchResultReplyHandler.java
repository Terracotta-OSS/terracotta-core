/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.object.msg.SearchResponseMessage;

public class SearchResultReplyHandler extends AbstractEventHandler {

  private final SearchResultManager searchResultMgr;

  public SearchResultReplyHandler(final SearchResultManager resultManager) {
    this.searchResultMgr = resultManager;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof SearchResponseMessage) {
      final SearchResponseMessage responseMsg = (SearchResponseMessage) context;

      if (responseMsg.isError()) {
        this.searchResultMgr.addErrorResponse(responseMsg.getLocalSessionID(), responseMsg.getRequestID(),
                                              responseMsg.getGroupIDFrom(),
                                              responseMsg.getErrorMessage(), responseMsg.getSourceNodeID());
      } else {
        this.searchResultMgr.addResponse(responseMsg.getLocalSessionID(), responseMsg.getRequestID(),
                                         responseMsg.getGroupIDFrom(),
                                         responseMsg.getResults(), responseMsg.getSourceNodeID());
      }
    } else {
      throw new AssertionError("Unknown message type received from server - " + context.getClass().getName());
    }
  }

}
