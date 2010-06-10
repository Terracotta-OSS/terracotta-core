/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.exception.ImplementMe;
import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.locks.LockID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Set;

public class TestRemoteServerMapManager implements RemoteServerMapManager {

  public ObjectID getMappingForKey(final ObjectID oid, final Object portableKey) {
    throw new ImplementMe();
  }

  public int getSize(final ObjectID oid) {
    throw new ImplementMe();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    throw new ImplementMe();
  }

  public void pause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void shutdown() {
    throw new ImplementMe();
  }

  public void unpause(final NodeID remoteNode, final int disconnected) {
    throw new ImplementMe();
  }

  public void addResponseForGetSize(final SessionID localSessionID, final ObjectID mapID,
                                    final ServerMapRequestID requestID, final Integer size, final NodeID sourceNodeID) {
    throw new ImplementMe();
  }

  public void addResponseForKeyValueMapping(final SessionID localSessionID, final ObjectID mapID,
                                            final Collection<ServerMapGetValueResponse> responses, final NodeID nodeID) {
    throw new ImplementMe();
  }

  public void addCachedItemForLock(final LockID lockID, final CachedItem item) {
    throw new ImplementMe();
  }

  public void flush(final LockID lockID) {
    throw new ImplementMe();
  }

  public void removeCachedItemForLock(final LockID lockID, final CachedItem item) {
    throw new ImplementMe();
  }

  public void clearCachedItemsForLocks(final Set<LockID> toEvict) {
    throw new ImplementMe();
  }
}
