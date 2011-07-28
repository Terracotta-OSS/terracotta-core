/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public final class NullRemoteServerMapManager implements RemoteServerMapManager {
  public void unpause(final NodeID remoteNode, final int disconnected) {
    //
  }

  public void shutdown() {
    //
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    //
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    //
  }

  public void removeCachedItem(final Object id, final CachedItem item) {
    //
  }

  public long getAllSize(final ObjectID[] mapIDs) {
    return -1;
  }

  public Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    return null;
  }

  public Set getAllKeys(ObjectID oid) {
    //
    return null;
  }

  public void flush(final Object id) {
    //
  }

  public void addResponseForGetAllSize(final SessionID localSessionID, final GroupID groupID,
                                       final ServerMapRequestID requestID, final Long size, final NodeID sourceNodeID) {
    //
  }

  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final Collection<ServerMapGetValueResponse> responses, final NodeID nodeID) {
    //
  }

  public void objectNotFoundFor(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                final NodeID nodeID) {
    //
  }

  public void addCachedItem(final Object id, final CachedItem item) {
    //
  }

  public void clearCachedItemsForLocks(final Set<LockID> toEvict, boolean waitforRecallComplete) {
    //
  }

  public void initiateCachedItemEvictionFor(final TCObjectServerMap serverMap) {
    //
  }

  public void expired(final TCObjectServerMap serverMap, final CachedItem ci) {
    //
  }

  public void addResponseForGetAllKeys(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                       Set keys, NodeID nodeID) {
    //
  }

  public void getMappingForAllKeys(final Map<ObjectID, Set<Object>> mapIdToKeysMap, final Map<Object, Object> rv) {
    //
  }

}
