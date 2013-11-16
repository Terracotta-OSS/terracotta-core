/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.object.SearchRequestID;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.SearchBuilder;

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
   * Query stack to search
   * 
   * @return List linkedlist
   */
  public List getQueryStack();

  /**
   * Return set of attributes ask for.
   * 
   * @return Set<String>
   */
  public Set<String> getAttributes();

  /**
   * Return set of attributes to group results by
   */
  public Set<String> getGroupByAttributes();

  /**
   * Return a map of sort attributes
   */
  public List<NVPair> getSortAttributes();

  /**
   * Return a map of attribute aggregators
   * 
   * @return List<NVPair>
   */
  public List<NVPair> getAggregators();

  /**
   * Result should include keys
   * 
   * @return boolean
   */
  public boolean includeKeys();

  /**
   * Result should include values
   * 
   * @return boolean
   */
  public boolean includeValues();

  /**
   * Return maximum results size. return integer
   */
  public int getMaxResults();

  /**
   * Return the desired result set batch size
   */
  public int getValuePrefetchSize();

  /**
   * Return true if the server should start prefetch for the first batch
   */
  public boolean isPrefetchFirstBatch();

  /**
   * How many results to return with query response. If actual returned result count exceeds this value, the rest must
   * be fetched using result paging. Set to {@link SearchBuilder.Search#BATCH_SIZE_UNLIMITED} to get all results in one
   * shot in response message regardless of actual hit count.
   */
  public int getResultPrefetchLimit();
}
