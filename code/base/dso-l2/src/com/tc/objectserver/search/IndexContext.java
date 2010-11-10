/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class IndexContext {

  public static IndexContext     NULL_CONTEXT         = new IndexContext();

  private List<IndexQueryResult> queryResults         = Collections.EMPTY_LIST;
  private Map<String, NVPair>    aggregatorAttributes = Collections.EMPTY_MAP;
  private List<NVPair>           aggregatorResults    = Collections.EMPTY_LIST;

  public List<IndexQueryResult> getQueryResults() {
    return queryResults;
  }

  public void setQueryResults(List<IndexQueryResult> queryResults) {
    this.queryResults = queryResults;
  }

  public NVPair getAggregatorAttributesPair(String attributeName) {
    return aggregatorAttributes.get(attributeName);
  }

  public void setAggregatorAttributes(Map<String, NVPair> aggregatorAttributes) {
    this.aggregatorAttributes = aggregatorAttributes;
  }

  public List<NVPair> getAggregatorResults() {
    return aggregatorResults;
  }

  public void setAggregatorResults(List<NVPair> aggregatorResults) {
    this.aggregatorResults = aggregatorResults;
  }

}
