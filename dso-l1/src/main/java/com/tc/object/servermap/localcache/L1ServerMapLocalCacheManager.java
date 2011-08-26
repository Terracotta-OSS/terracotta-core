/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache;

import com.tc.invalidation.Invalidations;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.bytecode.Manager;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LocksRecallService;

import java.util.Map;
import java.util.Set;

/**
 * A Global cache manager which contains info about all the LocalCache present in the L1.<br>
 * This acts a multiplexer between RemoteServerMapManager, HandshakeManager and the LocalCaches present
 */
public interface L1ServerMapLocalCacheManager extends LocksRecallService, TCObjectSelfStore {

  /**
   * Create a local cache for use or return already created local cache for the mapId
   */
  public ServerMapLocalCache getOrCreateLocalCache(ObjectID mapId, ClientObjectManager objectManager, Manager manager,
                                                   boolean localCacheEnabled);

  /**
   * flush the entries from the LocalCache associated with the given map id.<br>
   * This is used in the process of invalidations
   */
  public void removeEntriesForObjectId(ObjectID mapID, Set<ObjectID> set);

  /**
   * Used when a lock recall happens<br>
   * All the local cache entries associated with this lock id will be removed
   */
  public void removeEntriesForLockId(LockID lockID);

  /**
   * Handshake manager tries to get hold of all the objects present in the local caches
   */
  public void addAllObjectIDsToValidate(Invalidations invalidations);

  /**
   * Remember the mapId associated with the valueLockId
   */
  public void rememberMapIdForValueLockId(LockID valueLockId, ObjectID mapID);

  /**
   * Add a listener to the store.
   * 
   * @param mapID
   * @param maxElementsInMemory
   */
  public void addStoreListener(L1ServerMapLocalCacheStore store, ObjectID mapID);

  /**
   * Shut down all local caches
   */
  public void shutdown();

  public void evictElements(Map evictedElements);

  public void setLockManager(ClientLockManager lockManager);
}
