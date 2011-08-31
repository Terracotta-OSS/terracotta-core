/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.object.msg.SearchQueryRequestMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;

/**
 * All search queries are processed through this handler.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestMessageHandler extends AbstractEventHandler {

  private SearchRequestManager searchRequestManager;

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof SearchQueryRequestMessage) {
      SearchQueryRequestMessage msg = (SearchQueryRequestMessage) context;
      this.searchRequestManager.queryRequest(msg);
    } else {
      throw new AssertionError("Unknown context " + context);
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.searchRequestManager = serverContext.getSearchRequestManager();
  }

}
