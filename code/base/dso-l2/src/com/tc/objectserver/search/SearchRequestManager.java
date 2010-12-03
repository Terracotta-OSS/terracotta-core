/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.LinkedList;
import java.util.List;
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
   * @param clientID
   * @param requestID
   * @param groupIDFrom
   * @param cachename
   * @param queryStack
   * @param includeKeys
   * @param attributeSet
   * @param sortAttributes
   * @param aggregators
   * @param maxResults
   */
  public void queryRequest(ClientID clientID, SearchRequestID requestID, GroupID groupIDFrom, String cachename,
                           LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                           List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults);

  /**
   * Query response.
   * 
   * @param SearchQueryContext queriedContext
   * @param List<SearchQueryResult> results
   * @param aggregatorResults
   */
  public void queryResponse(SearchQueryContext queriedContext, List<IndexQueryResult> results,
                            List<NVPair> aggregatorResults);

  /**
   * Query error response
   */
  public void queryErrorResponse(SearchQueryContext sqc, String message);

}
