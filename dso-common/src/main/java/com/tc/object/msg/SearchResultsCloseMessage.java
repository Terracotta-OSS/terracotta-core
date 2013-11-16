/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.SearchRequestID;

public interface SearchResultsCloseMessage extends SearchRequestMessage {
  public void initialize(final String name, SearchRequestID requestId);
}
