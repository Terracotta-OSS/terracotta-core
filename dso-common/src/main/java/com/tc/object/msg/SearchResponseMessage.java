/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.GroupID;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;

public interface SearchResponseMessage extends TCMessage {

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
   * @param isGroupBy
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupIDFrom,
                                        List<IndexQueryResult> results, List<Aggregator> aggregators,
                                        boolean anyCriteriaMatched, boolean isGroupBy, long totalCount);

  /**
   * Initialize error response
   * 
   * @param searchRequestID
   * @param groupIDFrom
   * @param errorMessage
   */
  public void initSearchResponseMessage(SearchRequestID searchRequestID, GroupID groupIDFrom, String errorMessage);

  /**
   * @return List<IndexQueryResult> results.
   */
  public List<IndexQueryResult> getResults();

  public String getErrorMessage();

  public boolean isError();

}