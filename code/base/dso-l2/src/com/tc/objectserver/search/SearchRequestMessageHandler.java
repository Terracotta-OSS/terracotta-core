/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.object.msg.SearchQueryRequestMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;

/**
 * All search request are processed through this handler. Every context should implement
 * {@link MultiThreadedEventContext} so that order can be maintained per client.
 * 
 * @author Nabib El-Rahman
 */
public class SearchRequestMessageHandler extends AbstractEventHandler {

  private SearchRequestManager searchRequestManager;

  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof SearchQueryRequestMessage) {
      SearchQueryRequestMessage msg = (SearchQueryRequestMessage) context;
      this.searchRequestManager.queryRequest((ClientID) msg.getClientID(), msg.getRequestID(), msg.getCachename(), msg
          .getQueryStack(), msg.includeKeys(), msg.getAttributes(), msg.getSortAttributes(), msg.getAggregators());

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
