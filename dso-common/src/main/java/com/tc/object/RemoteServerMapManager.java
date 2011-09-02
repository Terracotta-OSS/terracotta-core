/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.invalidation.InvalidationsProcessor;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.gtx.PreTransactionFlushCallback;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface RemoteServerMapManager extends ClientHandshakeCallback, PreTransactionFlushCallback,
    InvalidationsProcessor {

  public Object getMappingForKey(ObjectID mapID, Object portableKey);

  public Set getAllKeys(ObjectID mapID);

  public long getAllSize(ObjectID[] mapIDs);

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID,
                                            Collection<ServerMapGetValueResponse> responses, NodeID nodeID);

  public void addResponseForGetAllKeys(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                       Set keys, NodeID nodeID);

  public void addResponseForGetAllSize(SessionID localSessionID, GroupID groupID, ServerMapRequestID requestID,
                                       Long size, NodeID sourceNodeID);

  public void objectNotFoundFor(SessionID sessionID, ObjectID mapID, ServerMapRequestID requestID, NodeID nodeID);

  public void getMappingForAllKeys(final Map<ObjectID, Set<Object>> mapIdToKeysMap, final Map<Object, Object> rv);
}
