/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.terracottatech.search.IndexQueryResult;
import com.terracottatech.search.NVPair;
import com.terracottatech.search.aggregator.Aggregator;

import java.util.List;
import java.util.Set;

/**
 * Null Manager.
 * 
 * @author Nabib El-Rahman
 */
public class NullRemoteSearchRequestManager implements RemoteSearchRequestManager {

  @Override
  public SearchQueryResults query(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                  Set<String> attributeSet, List<NVPair> sortAttributeMap, List<NVPair> aggregators,
                                  int maxResults, int batchSize, SearchRequestID reqId, int resultSetLimit) {
    return null;
  }

  @Override
  public SearchQueryResults query(String cachename, List queryStack, Set<String> attributeSet,
                                  Set<String> groupByAttributes, List<NVPair> sortAttributeMap,
                                  List<NVPair> aggregators, int maxResults, int batchSize, SearchRequestID reqId) {
    return null;
  }

  @Override
  public void cleanup() {
    //
  }

  @Override
  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    //
  }

  @Override
  public void pause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

  @Override
  public void unpause(NodeID remoteNode, int disconnected) {
    //
  }

  @Override
  public void addResponseForQuery(final SessionID sessionID, final SearchRequestID requestID,
                                  final GroupID groupIDFrom, final List<IndexQueryResult> queryResults,
                                  long totalResCt,
                                  final List<Aggregator> aggregators, final NodeID nodeID,
                                  final boolean anyCriteriaMatched) {
    //
  }

  @Override
  public void addErrorResponseForQuery(SessionID sessionID, SearchRequestID requestID, GroupID groupIDFrom,
                                       String errorMessage, NodeID nodeID) {
    //
  }

}
