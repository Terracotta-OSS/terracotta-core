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

import com.tc.abortable.AbortedOperationException;
import com.tc.invalidation.InvalidationsProcessor;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;
import com.tc.text.PrettyPrintable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RemoteServerMapManager extends ClientHandshakeCallback,
    InvalidationsProcessor, PrettyPrintable {

  public Object getMappingForKey(ObjectID mapID, Object portableKey) throws AbortedOperationException;

  public Set getAllKeys(ObjectID mapID) throws AbortedOperationException;

  public long getAllSize(ObjectID[] mapIDs) throws AbortedOperationException;

  public void getMappingForAllKeys(final Map<ObjectID, Set<Object>> mapIdToKeysMap, final Map<Object, Object> rv)
      throws AbortedOperationException;

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID,
                                            Collection<ServerMapGetValueResponse> responses, NodeID nodeID);

  public void addResponseForGetAllKeys(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                       Set keys, NodeID nodeID);

  public void addResponseForGetAllSize(SessionID localSessionID, GroupID groupID, ServerMapRequestID requestID,
                                       Long size, NodeID sourceNodeID);

  public void objectNotFoundFor(SessionID sessionID, ObjectID mapID, ServerMapRequestID requestID, NodeID nodeID);
}
