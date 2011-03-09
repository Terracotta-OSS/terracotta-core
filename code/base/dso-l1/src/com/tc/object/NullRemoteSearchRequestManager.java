/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.metadata.NVPair;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.search.IndexQueryResult;
import com.tc.search.SearchQueryResults;
import com.tc.search.aggregator.Aggregator;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Null Manager.
 * 
 * @author Nabib El-Rahman
 */
public class NullRemoteSearchRequestManager implements RemoteSearchRequestManager {

  public SearchQueryResults query(String cachename, LinkedList queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                  int maxResults, int batchSize) {
    return null;
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    //
  }

  public void pause(NodeID remoteNode, int disconnected) {
    //
  }

  public void shutdown() {
    //
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    //
  }

  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                  final GroupID groupIDFrom, final List<IndexQueryResult> queryResults,
                                  final List<Aggregator> aggregators, final NodeID nodeID,
                                  final boolean anyCriteriaMatched) {
    //
  }

  public void addErrorResponseForQuery(SessionID sessionID, SearchRequestID requestID, GroupID groupIDFrom,
                                       String errorMessage, NodeID nodeID) {
    //
  }

}
