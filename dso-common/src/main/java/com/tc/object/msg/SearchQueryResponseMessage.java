/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.SearchRequestID;
import com.tc.search.IndexQueryResult;
import com.tc.search.aggregator.Aggregator;

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
   * Originating request groupID.
   */
  public GroupID getGroupIDFrom();

  /**
   * Initialize message.
   * 
   * @param searchRequestID
   * @param groupIDFrom
   * @param anyCriteriaMatched
   * @param aggregatorResults
   * @param aggregatorResults
   * @param anyCriteriaMatched
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupIDFrom,
                                        List<IndexQueryResult> results, List<Aggregator> aggregators,
                                        boolean anyCriteriaMatched);

  /**
   * Initialize error response
   * 
   * @param searchRequestID
   * @param groupIDFrom
   * @param errorMessage
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupIDFrom, String errorMessage);

  /**
   * @return List<SearchQueryResult> results.
   */
  public List<IndexQueryResult> getResults();

  /**
   * @return List<NVPair> aggregator results.
   */
  public List<Aggregator> getAggregators();

  public String getErrorMessage();

  public boolean isError();

  public boolean isAnyCriteriaMatched();

}
