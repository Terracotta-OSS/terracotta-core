/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import com.terracottatech.search.IndexQueryResult;

import java.util.List;

public interface SearchQueryResults<T extends IndexQueryResult> {

  List<T> getResults();

  List<Object> getAggregatorResults();

  boolean isError();

  String getErrorMessage();

  boolean isFirstBatchPrefetched();

  boolean anyCriteriaMatched();

}
