/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manager query request from the client.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchRequestManager {

  /**
   * Query request. TODO: currently just requesting an attribute and value to match against, this will change
   * drastically when query is built out.
   * 
   * @param ClientID clientID
   * @param SearchRequestID requestID
   * @param String cachename
   * @param LinkedList queryStack
   * @param boolean includeKeys
   * @param Set<String> attributeSet
   * @param Map<String,SortOperations> sortAttributes
   * @param List<NVPair> aggregators
   */
  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, LinkedList queryStack,
                           boolean includeKeys, Set<String> attributeSet, Map<String, SortOperations> sortAttributes,
                           List<NVPair> aggregators);

  /**
   * Query response.
   * 
   * @param SearchQueryContext queriedContext
   * @param List<SearchQueryResult> results
   * @param aggregatorResults
   */
  public void queryResponse(SearchQueryContext queriedContext, List<IndexQueryResult> results,
                            List<NVPair> aggregatorResults);

}
