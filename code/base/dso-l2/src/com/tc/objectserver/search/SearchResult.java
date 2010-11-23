/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.Collections;
import java.util.List;

public class SearchResult {

  public static final SearchResult     NULL_RESULT = new SearchResult(Collections.EMPTY_LIST, Collections.EMPTY_LIST);

  private final List<IndexQueryResult> queryResults;
  private final List<NVPair>           aggregatorResults;

  public SearchResult(List<IndexQueryResult> queryResults, List<NVPair> aggregatorResults) {
    this.queryResults = queryResults;
    this.aggregatorResults = aggregatorResults;
  }

  public List<IndexQueryResult> getQueryResults() {
    return queryResults;
  }

  public List<NVPair> getAggregatorResults() {
    return aggregatorResults;
  }
}
