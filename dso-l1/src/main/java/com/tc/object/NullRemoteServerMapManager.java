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

import com.tc.invalidation.Invalidations;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrinter;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class NullRemoteServerMapManager implements RemoteServerMapManager {

  @Override
  public void cleanup() {
    //
  }

  public void initialize(ClientObjectManager clientObjectManager) {
    //
  }

  @Override
  public void unpause(final NodeID remoteNode, final int disconnected) {
    //
  }

  @Override
  public void shutdown(boolean fromShutdownHook) {
    //
  }

  @Override
  public void pause(final NodeID remoteNode, final int disconnected) {
    //
  }

  @Override
  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    //
  }

  @Override
  public long getAllSize(final ObjectID[] mapIDs) {
    return -1;
  }

  @Override
  public Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    return null;
  }

  @Override
  public Set getAllKeys(ObjectID oid) {
    //
    return null;
  }

  @Override
  public void addResponseForGetAllSize(final SessionID localSessionID, final GroupID groupID,
                                       final ServerMapRequestID requestID, final Long size, final NodeID sourceNodeID) {
    //
  }

  @Override
  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final Collection<ServerMapGetValueResponse> responses, final NodeID nodeID) {
    //
  }

  @Override
  public void objectNotFoundFor(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                final NodeID nodeID) {
    //
  }

  public void initiateCachedItemEvictionFor(final TCObjectServerMap serverMap) {
    //
  }

  @Override
  public void addResponseForGetAllKeys(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                       Set keys, NodeID nodeID) {
    //
  }

  @Override
  public void processInvalidations(Invalidations invalidations) {
    //
  }

  @Override
  public void getMappingForAllKeys(final Map<ObjectID, Set<Object>> mapIdToKeysMap, final Map<Object, Object> rv) {
    //
  }

  @Override
  public PrettyPrinter prettyPrint(PrettyPrinter out) {
    return out;
  }

}
