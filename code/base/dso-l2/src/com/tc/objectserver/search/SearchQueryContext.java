/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Context holding search queury search information.
 * 
 * @author Nabib El-Rahman
 */
public class SearchQueryContext implements SearchEventContext {

  private final ClientID        clientID;
  private final SearchRequestID requestID;
  private final GroupID         groupIDFrom;
  private final String          cacheName;
  private final LinkedList      queryStack;
  private final boolean         includeKeys;
  private final Set<String>     attributeSet;
  private final List<NVPair>    sortAttributes;
  private final List<NVPair>    aggregators;
  private final int             maxResults;

  public SearchQueryContext(ClientID clientID, SearchRequestID requestID, GroupID groupIDFrom, String cacheName,
                            LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                            List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults) {
    this.clientID = clientID;
    this.requestID = requestID;
    this.groupIDFrom = groupIDFrom;
    this.cacheName = cacheName;
    this.queryStack = queryStack;
    this.includeKeys = includeKeys;
    this.attributeSet = attributeSet;
    this.sortAttributes = sortAttributes;
    this.aggregators = aggregators;
    this.maxResults = maxResults;
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
   * GroupID that request originated from.
   */
  public GroupID getGroupIDFrom() {
    return this.groupIDFrom;
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
   * @return sortAttributes.
   */
  public List<NVPair> getSortAttributes() {
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
   * Return maximum size of results.
   * 
   * @return integer
   */
  public int getMaxResults() {
    return this.maxResults;
  }

  /**
   * {@inheritDoc}
   */
  public Object getKey() {
    return clientID;
  }

}
