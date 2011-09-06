/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.search.IndexQueryResult;
import com.tc.search.aggregator.Aggregator;

import java.util.Collections;
import java.util.List;

public class SearchResult {

  public static final SearchResult     NULL_RESULT = new SearchResult(Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                                                                      false);

  private final List<IndexQueryResult> queryResults;
  private final List<Aggregator>       aggregators;
  private final boolean                anyCriteriaMatch;

  public SearchResult(List<IndexQueryResult> queryResults, List<Aggregator> aggregators, boolean anyCriteriaMatch) {
    this.queryResults = queryResults;
    this.aggregators = aggregators;
    this.anyCriteriaMatch = anyCriteriaMatch;
  }

  public List<IndexQueryResult> getQueryResults() {
    return queryResults;
  }

  public List<Aggregator> getAggregators() {
    return aggregators;
  }

  public boolean isAnyCriteriaMatch() {
    return anyCriteriaMatch;
  }
}
