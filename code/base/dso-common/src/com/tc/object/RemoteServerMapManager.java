/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.net.NodeID;
import com.tc.object.cache.CachedItem;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.locks.LockID;
import com.tc.object.session.SessionID;

import java.util.Collection;
import java.util.Set;

public interface RemoteServerMapManager extends ClientHandshakeCallback {

  public Object getMappingForKey(ObjectID oid, Object portableKey);

  public int getSize(ObjectID mapID);

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID,
                                            Collection<ServerMapGetValueResponse> responses, NodeID nodeID);

  public void addResponseForGetSize(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                    Integer size, NodeID sourceNodeID);
  
  public void objectNotFoundFor(SessionID sessionID, ObjectID mapID, ServerMapRequestID requestID,
                                              NodeID nodeID);

  public void addCachedItemForLock(LockID lockID, CachedItem item);

  public void removeCachedItemForLock(LockID lockID, CachedItem item);

  public void flush(LockID lockID);

  public void clearCachedItemsForLocks(Set<LockID> toEvict);

  public void initiateCachedItemEvictionFor(final TCObjectServerMap serverMap);
}
