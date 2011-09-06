/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.io.TCSerializable;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.List;

/**
 * Results query results from the index.
 * 
 * @author Nabib El-Rahman
 */
public interface IndexQueryResults extends TCSerializable {

  /**
   * Query results, based on criteria.
   * 
   * @return results
   */
  List<IndexQueryResult> getQueryResults();

  /**
   * Aggregator results.
   * 
   * @return results
   */
  List<NVPair> getAggregatorResults();

}
