/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.metadata.NVPair;
import com.tc.object.session.SessionID;
import com.tc.search.IndexQueryResult;
import com.tc.search.SearchQueryResults;
import com.tc.search.SortOperations;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * 
 */
public interface RemoteSearchRequestManager extends ClientHandshakeCallback {

  public SearchQueryResults query(String cachename, LinkedList queryStack, boolean includeKeys,
                                  Set<String> attributeSet, Map<String, SortOperations> sortAttributeMap,
                                  List<NVPair> aggregators);

  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                  final List<IndexQueryResult> queryResults, final List<NVPair> aggregatorResults,
                                  final NodeID nodeID);

  public boolean hasRequestID(SearchRequestID requestID);

}
