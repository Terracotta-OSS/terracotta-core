/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.search.SearchRequestID;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;

public interface SearchResultsRequestMessage extends SearchRequestMessage {

  /**
   * Starting offset
   */
  public int getStart();

  /**
   * Desired page size
   */
  public int getPageSize();

  /**
   * Initialize this message
   */
  public void initialize(final SearchRequestID searchRequestID, final String cacheName, final List queryStack,
                         final boolean keys, final boolean values, final Set<String> attributeSet,
                         final List<NVPair> sortAttributesMap, final List<NVPair> aggregators, int maxResults,
                         int start, int pageSize);

}
