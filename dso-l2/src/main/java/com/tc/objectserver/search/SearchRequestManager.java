/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.PostInit;
import com.tc.object.msg.SearchQueryRequestMessage;

/**
 * Manage query request from the client.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchRequestManager extends PostInit {

  /**
   * Query request
   */
  public void queryRequest(SearchQueryRequestMessage request);

}
