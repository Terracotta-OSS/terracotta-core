/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The class represents a query request from the client. the cachename is to identify the index and the query string is
 * our client side query in string form.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryRequestMessage extends TCMessage, MultiThreadedEventContext {

  /**
   * ClientID
   */
  public NodeID getClientID();

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
   * @param attributeSet
   * @param sortAttributeMap
   * @param aggregators
   * @param maxResults
   */
  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final GroupID groupFrom,
                                          final String cacheName, final LinkedList queryStack, final boolean keys,
                                          final Set<String> attributeSet,
                                          final Map<String, SortOperations> sortAttributesMap,
                                          final List<NVPair> aggregators, int maxResults);

  /**
   * Name of cache to query against.
   * 
   * @return String string.
   */
  public String getCachename();

  /**
   * Query stack to search
   * 
   * @return LinkedList linkedlist
   */
  public LinkedList getQueryStack();

  /**
   * Return set of attributes ask for.
   * 
   * @return Set<String>
   */
  public Set<String> getAttributes();

  /**
   * Return a map of sort attributes
   * 
   * @return Map<String, SortOperations>
   */
  public Map<String, SortOperations> getSortAttributes();

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
   * Return maximum results size. return integer
   */
  public int getMaxResults();

}
