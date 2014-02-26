/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.search.SearchRequestID;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;

/**
 * The class represents a query request from the client. the cachename is to identify the index
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryRequestMessage extends SearchRequestMessage {

  /**
   * Initialize message.
   * 
   * @param searchRequestID
   * @param groupFrom
   * @param cacheName
   * @param queryStack
   * @param keys
   * @param values
   * @param attributeSet
   * @param sortAttributeMap
   * @param aggregators
   * @param maxResults
   */
  public void initializeSearchRequestMessage(final SearchRequestID searchRequestID,
                                             final String cacheName, final List queryStack, final boolean keys,
                                             final boolean values, final Set<String> attributeSet,
                                             Set<String> groupByAttrs, final List<NVPair> sortAttributesMap,
                                             final List<NVPair> aggregators, int maxResults, int batchSize,
                                             boolean prefetchFirstBatch, int resultPrefetchLimit);

  /**
   * Return set of attributes to group results by
   */
  public Set<String> getGroupByAttributes();

  /**
   * Return the desired result set batch size
   */
  public int getValuePrefetchSize();

  /**
   * Return true if the server should start prefetch for the first batch
   */
  public boolean isPrefetchFirstBatch();

}
