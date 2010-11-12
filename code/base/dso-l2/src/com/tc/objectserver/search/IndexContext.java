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

  public static final IndexContext  NULL_CONTEXT         = new IndexContext();

  private List<IndexQueryResult>    queryResults         = Collections.EMPTY_LIST;
  private Map<String, List<NVPair>> aggregatorAttributes = Collections.EMPTY_MAP;
  private List<NVPair>              aggregatorResults    = Collections.EMPTY_LIST;

  public List<IndexQueryResult> getQueryResults() {
    return queryResults;
  }

  public void setQueryResults(List<IndexQueryResult> queryResults) {
    this.queryResults = queryResults;
  }

  public NVPair getAggregatorAttributesPair(String key, String attributeName) {
    List<NVPair> attributes = aggregatorAttributes.get(key);
    for (NVPair pair : attributes) {
      if (attributeName.equals(pair.getName())) { return pair; }
    }
    return null;
  }

  public void setAggregatorAttributes(Map<String, List<NVPair>> aggregatorAttributesMap) {
    this.aggregatorAttributes = aggregatorAttributesMap;
  }

  public List<NVPair> getAggregatorResults() {
    return aggregatorResults;
  }

  public void setAggregatorResults(List<NVPair> aggregatorResults) {
    this.aggregatorResults = aggregatorResults;
  }

}
