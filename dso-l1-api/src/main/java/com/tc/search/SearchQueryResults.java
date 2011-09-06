/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import java.util.List;

public interface SearchQueryResults {

  List<IndexQueryResult> getResults();

  List<Object> getAggregatorResults();

  boolean isError();

  String getErrorMessage();

  boolean isFirstBatchPrefetched();

  boolean anyCriteriaMatched();

}
