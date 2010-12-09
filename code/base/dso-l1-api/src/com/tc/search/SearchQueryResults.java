/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.search;

import java.util.List;

public interface SearchQueryResults {

  public List<IndexQueryResult> getResults();

  public List getAggregatorResults();

  public boolean isError();

  public String getErrorMessage();

}
