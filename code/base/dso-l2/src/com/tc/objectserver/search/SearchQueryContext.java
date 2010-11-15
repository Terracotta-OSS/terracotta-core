/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Context holding search queury search information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements MultiThreadedEventContext {

  private final ClientID                    clientID;
  private final SearchRequestID             requestID;
  private final String                      cacheName;
  private final LinkedList                  queryStack;
  private final boolean                     includeKeys;
  private final Set<String>                 attributeSet;
  private final Map<String, SortOperations> sortAttributes;
  private final List<NVPair>                aggregators;

  public SearchQueryContext(ClientID clientID, SearchRequestID requestID, String cacheName, LinkedList queryStack,
                            boolean includeKeys, Set<String> attributeSet, Map<String, SortOperations> sortAttributes,
                            List<NVPair> aggregators) {
    this.clientID = clientID;
    this.requestID = requestID;
    this.cacheName = cacheName;
    this.queryStack = queryStack;
    this.includeKeys = includeKeys;
    this.attributeSet = attributeSet;
    this.sortAttributes = sortAttributes;
    this.aggregators = aggregators;
  }

  /**
   * Query stack.
   * 
   * @return LinkedList linkedList
   */
  public LinkedList getQueryStack() {
    return this.queryStack;
  }

  /**
   * Cachename/Index name.
   * 
   * @return String string
   */
  public String getCacheName() {
    return this.cacheName;
  }

  /**
   * Return clientID.
   * 
   * @return ClientID clientID
   */
  public ClientID getClientID() {
    return this.clientID;
  }

  /**
   * SearchRequestID requestID.
   * 
   * @return SearchRequestID requestID
   */
  public SearchRequestID getRequestID() {
    return this.requestID;
  }

  /**
   * Result set should include keys.
   * 
   * @return boolean true if should return keys.
   */
  public boolean includeKeys() {
    return includeKeys;
  }

  /**
   * Attribute keys, should return values with result set.
   * 
   * @return Set<String> attributes.
   */
  public Set<String> getAttributeSet() {
    return attributeSet;
  }

  /**
   * Sorted attributes, pair of attributes if ascending, true
   * 
   * @return Map<String,SortOperations> sortAttributes.
   */
  public Map<String, SortOperations> getSortAttributes() {
    return sortAttributes;
  }

  /**
   * Attribute aggregators, returns a attribute->aggregator type pairs.
   * 
   * @return List<NVPair>
   */
  public List<NVPair> getAggregators() {
    return aggregators;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return clientID;
  }

}
