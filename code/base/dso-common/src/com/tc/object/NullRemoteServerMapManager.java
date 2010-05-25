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

public class NullRemoteServerMapManager implements RemoteServerMapManager {

  public void addCachedItemForLock(LockID lockID, CachedItem item) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void addResponseForGetSize(SessionID localSessionID, ObjectID mapID, ServerMapRequestID requestID,
                                    Integer size, NodeID sourceNodeID) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void addResponseForKeyValueMapping(SessionID localSessionID, ObjectID mapID,
                                            Collection<ServerMapGetValueResponse> responses, NodeID nodeID) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }


  public Object getMappingForKey(ObjectID oid, Object portableKey) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public int getSize(ObjectID mapID) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void removeCachedItemForLock(LockID lockID, CachedItem item) {
    throw new UnsupportedOperationException("Unsupported feature in open-source version, need enterprise terracotta");
  }

  public void flush(LockID lockID) {
    // ignore
  }

  public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
    // ignore
  }

  public void pause(NodeID remoteNode, int disconnected) {
    // ignore
  }

  public void shutdown() {
    // ignore
  }

  public void unpause(NodeID remoteNode, int disconnected) {
    // ignore
  }

}
