/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.List;

/**
 * Response message for object requests.
 * 
 * @author Nabib El-Rahman
 */
public interface SearchQueryResponseMessage extends TCMessage {

  /**
   * Search Identifier. return SearchRequestID requestID
   */
  public SearchRequestID getRequestID();

  /**
   * Initialize message.
   * 
   * @param aggregatorResults
   * @param SearchRequestID searchRequestID
   * @param List<NVPair> aggregatorResults
   */
  public void initialSearchResponseMessage(SearchRequestID searchRequestID, List<IndexQueryResult> results,
                                           List<NVPair> aggregatorResults);

  /**
   * @return List<SearchQueryResult> results.
   */
  public List<IndexQueryResult> getResults();

  /**
   * @return List<NVPair> aggregator results.
   */
  public List<NVPair> getAggregatorResults();

}
