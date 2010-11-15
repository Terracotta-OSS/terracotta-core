/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
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
   * Initialize message.
   * 
   * @param SearchRequestID searchRequestID
   * @param String cacheName
   * @param LinkedList queryStack
   * @param boolean keys
   * @param Set<String> attributeSet
   * @param Map<String,SortOperations> sortAttributeMap
   * @param List<NVPair> aggregators
   */
  public void initialSearchRequestMessage(final SearchRequestID searchRequestID, final String cacheName,
                                          final LinkedList queryStack, final boolean keys,
                                          final Set<String> attributeSet,
                                          final Map<String, SortOperations> sortAttributesMap,
                                          final List<NVPair> aggregators);

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

}
