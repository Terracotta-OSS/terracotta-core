/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.ConfigurationContext;
import com.tc.object.msg.SearchQueryRequestMessage;
import com.tc.object.msg.SearchResultsRequestMessage;

public class NullSearchRequestManager implements SearchRequestManager {

  @Override
  public void queryRequest(SearchQueryRequestMessage request) {
    //
  }

  @Override
  public void resultsRequest(SearchResultsRequestMessage request) {
    //
  }

  @Override
  public void initializeContext(ConfigurationContext context) {
    //
  }

}
