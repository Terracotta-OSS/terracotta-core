/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.search;

import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.SearchRequestID;
import com.tc.object.metadata.NVPair;
import com.tc.search.IndexQueryResult;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class NullSearchRequestManager implements SearchRequestManager {

  public void queryRequest(ClientID clientID, SearchRequestID requestID, GroupID groupIDFrom, String cachename,
                           LinkedList queryStack, boolean includeKeys, Set<String> attributeSet,
                           List<NVPair> sortAttributes, List<NVPair> aggregators, int maxResults) {
    // Do nothing
  }

  public void queryResponse(SearchQueryContext queriedContext, List<IndexQueryResult> results,
                            List<NVPair> aggregatorResults) {
    // Do nothing
  }

  public void queryErrorResponse(SearchQueryContext sqc, String message) {
    // Do nothing
  }
}
