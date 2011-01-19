/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.metadata.NVPair;
import com.tc.object.session.SessionID;
import com.tc.search.IndexQueryResult;
import com.tc.search.SearchQueryResults;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * 
 * 
 */
public interface RemoteSearchRequestManager extends ClientHandshakeCallback {

  public SearchQueryResults query(String cachename, LinkedList queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                  int maxResults);

  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID, GroupID groupIDFrom,
                                  final List<IndexQueryResult> queryResults, final List<NVPair> aggregatorResults,
                                  final NodeID nodeID);

  public void addErrorResponseForQuery(final SessionID sessionID, final SearchRequestID requestID, GroupID groupIDFrom,
                                       final String errorMessage, final NodeID nodeID);

  public boolean hasRequestID(SearchRequestID requestID);

}
