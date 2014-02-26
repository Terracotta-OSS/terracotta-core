/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.search;

import com.tc.abortable.AbortedOperationException;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;

public interface SearchResultManager extends ClientHandshakeCallback {
  void addResponse(final SessionID sessionID, final SearchRequestID requestID, final GroupID group,
                   final List<IndexQueryResult> queryResults, final NodeID nodeID);

  void addErrorResponse(final SessionID sessionID, final SearchRequestID requestID, final GroupID group,
                                       final String errorMessage, final NodeID nodeID);

  SearchQueryResults<IndexQueryResult> loadResults(String cachename, SearchRequestID reqId, List queryStack, boolean includeKeys,
                                                   boolean includeValues, Set<String> attributeSet,
                                                   List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                                   int maxResults, int start, int pageSize,
                                                   GroupID from) throws AbortedOperationException;

  void releaseResults(SearchRequestID request);

  int getOpenResultSetCount();

  void resultSetReceived(SearchRequestID id, long size);
}
