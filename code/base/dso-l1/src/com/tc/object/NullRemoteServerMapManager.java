/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;

import java.util.Collection;
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

  public void removeCachedItemForLock(final LockID lockID, final CachedItem item) {
    //
  }

  public int getSize(final ObjectID mapID) {
    return -1;
  }

  public Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    return null;
  }
  
  public Set getAllKeys(ObjectID oid) {
    //
    return null;
  }

  public void flush(final LockID lockID) {
    //
  }

  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final Collection<ServerMapGetValueResponse> responses, final NodeID nodeID) {
    //
  }

  public void addResponseForGetSize(final SessionID localSessionID, final ObjectID mapID,
                                    final ServerMapRequestID requestID, final Integer size, final NodeID sourceNodeID) {
    //
  }

  public void objectNotFoundFor(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                final NodeID nodeID) {
    //
  }

  public void addCachedItemForLock(final LockID lockID, final CachedItem item) {
    //
  }

  public void clearCachedItemsForLocks(final Set<LockID> toEvict) {
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

}
