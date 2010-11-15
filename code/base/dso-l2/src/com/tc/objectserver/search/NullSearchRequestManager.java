/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class NullSearchRequestManager implements SearchRequestManager {

  public void queryRequest(ClientID clientID, SearchRequestID requestID, String cachename, LinkedList queryStack,
                           boolean includeKeys, Set<String> attributeSet, Map<String, SortOperations> sortAttributes,
                           List<NVPair> aggregators) {
    // Do nothing
  }

  public void queryResponse(SearchQueryContext queriedContext, List<IndexQueryResult> results,
                            List<NVPair> aggregatorResults) {
    // Do nothing
  }

}
