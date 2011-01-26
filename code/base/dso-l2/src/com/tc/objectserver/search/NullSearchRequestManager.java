/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.ConfigurationContext;
import com.tc.object.msg.SearchQueryRequestMessage;

public class NullSearchRequestManager implements SearchRequestManager {

  public void queryRequest(SearchQueryRequestMessage request) {
    //
  }

  public void initializeContext(ConfigurationContext context) {
    //
  }

}
