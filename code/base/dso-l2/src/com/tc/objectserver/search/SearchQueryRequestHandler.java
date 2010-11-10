/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.net.ClientID;
import com.tc.object.msg.SearchQueryRequestMessage;
import com.tc.objectserver.core.api.ServerConfigurationContext;

/**
 * Handles Search query messages and initiates query.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryRequestHandler extends AbstractEventHandler {

  private volatile SearchRequestManager searchRequestManager;

  /**
   * {@inheritDoc}
   */
  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof SearchQueryRequestMessage) {
      SearchQueryRequestMessage message = (SearchQueryRequestMessage) context;
      this.searchRequestManager.queryRequest((ClientID) message.getSourceNodeID(), message.getRequestID(), message
          .getCachename(), message.getQueryStack(), message.includeKeys(), message.getAttributes(), message
          .getSortAttributes(), message.getAggregators());

    } else {
      throw new AssertionError("Unknown context: " + context);
    }
  }

  @Override
  protected void initialize(ConfigurationContext context) {
    super.initialize(context);
    ServerConfigurationContext serverContext = (ServerConfigurationContext) context;
    this.searchRequestManager = serverContext.getSearchRequestManager();
  }

}
