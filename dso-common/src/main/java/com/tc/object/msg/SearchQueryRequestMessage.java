/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;

import java.util.List;
import java.util.Set;

/**
 * The class represents a query request from the client. the cachename is to identify the index
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryRequestMessage extends TCMessage, MultiThreadedEventContext {

  /**
   * ClientID
   */
  public ClientID getClientID();

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * GroupID message is from.
   */
  public GroupID getGroupIDFrom();

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
  public void initializeSearchRequestMessage(final SearchRequestID searchRequestID, final GroupID groupFrom,
                                             final String cacheName, final List queryStack, final boolean keys,
                                             final boolean values, final Set<String> attributeSet,
                                             final List<NVPair> sortAttributesMap, final List<NVPair> aggregators,
                                             int maxResults, int batchSize, boolean prefetchFirstBatch);

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCacheName();

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
  public int getBatchSize();

  /**
   * Return true if the server should start prefetch for the first batch
   */
  public boolean isPrefetchFirstBatch();
}
