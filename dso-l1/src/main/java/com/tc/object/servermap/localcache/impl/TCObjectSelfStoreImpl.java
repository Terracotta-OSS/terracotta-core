/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.invalidation.Invalidations;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.TCObjectSelf;
import com.tc.object.TCObjectSelfCallback;
import com.tc.object.TCObjectSelfStore;
import com.tc.object.TCObjectSelfStoreValue;
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.PutType;
import com.tc.object.servermap.localcache.RemoveType;
import com.tc.object.servermap.localcache.ServerMapLocalCache;
import com.tc.util.ObjectIDSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class TCObjectSelfStoreImpl implements TCObjectSelfStore {
  private final TCObjectSelfStoreObjectIDSet                   tcObjectSelfStoreOids = new TCObjectSelfStoreObjectIDSet();
  private final ReentrantReadWriteLock                         tcObjectStoreLock     = new ReentrantReadWriteLock();
  private volatile TCObjectSelfCallback                        tcObjectSelfRemovedFromStoreCallback;
  private final Map<ObjectID, TCObjectSelf>                    tcObjectSelfTempCache = new HashMap<ObjectID, TCObjectSelf>();

  private final ConcurrentHashMap<ServerMapLocalCache, Object> localCaches;

  private static final TCLogger                                logger                = TCLogging
                                                                                         .getLogger(TCObjectSelfStoreImpl.class);

  public TCObjectSelfStoreImpl(ConcurrentHashMap<ServerMapLocalCache, Object> localCaches) {
    this.localCaches = localCaches;
  }

  public void updateLocalCache(ObjectID oid, TCObjectSelf self) {
    tcObjectStoreLock.writeLock().lock();

    if (!tcObjectSelfStoreOids.contains(oid)) { return; }

    try {
      for (ServerMapLocalCache cache : localCaches.keySet()) {
        Object value = cache.getKeyOrValueForObjectID(oid);
        if (value instanceof TCObjectSelfWrapper) {
          // non eventual case
          updateLocalCacheNonEventual(oid, self, cache, value);
          return;
        } else if (value != null) {
          // eventual case
          updateLocalCacheEventual(oid, self, cache, value);
          return;
        }
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public void updateLocalCacheEventual(ObjectID oid, TCObjectSelf self, ServerMapLocalCache cache, Object valueFetched) {
    Object key = valueFetched;
    AbstractLocalCacheStoreValue value = cache.getValue(key);
    if (value != null && value.getObjectId().equals(oid)) {
      LocalCacheStoreEventualValue newEventualValue = new LocalCacheStoreEventualValue(oid, value, value.getMapID());
      cache.getInternalStore().replace(key, value, newEventualValue, PutType.NORMAL);
    }
  }

  public void updateLocalCacheNonEventual(ObjectID oid, TCObjectSelf self, ServerMapLocalCache cache,
                                          Object valueFetched) {
    cache.getInternalStore()
        .replace(oid, valueFetched, new TCObjectSelfWrapper(self), PutType.PINNED_NO_SIZE_INCREMENT);
  }

  public Object getById(ObjectID oid) {
    long timePrev = System.currentTimeMillis();
    long startTime = timePrev;
    while (true) {
      Object rv = null;
      tcObjectStoreLock.readLock().lock();
      try {
        TCObjectSelf self = tcObjectSelfTempCache.get(oid);
        if (self != null) { return self; }

        if (!tcObjectSelfStoreOids.contains(oid)) {
          if (logger.isDebugEnabled()) {
            logger.debug("XXX GetById failed at TCObjectSelfStoreIDs, ObjectID=" + oid);
          }
          return null;
        }

        for (ServerMapLocalCache localCache : this.localCaches.keySet()) {
          Object object = localCache.getKeyOrValueForObjectID(oid);
          if (object == null) {
            continue;
          }
          if (object instanceof TCObjectSelfStoreValue) {
            rv = ((TCObjectSelfStoreValue) object).getTCObjectSelf();
            initTCObjectSelfIfRequired(rv);
            return rv;
          } else {
            AbstractLocalCacheStoreValue localCacheStoreValue = localCache.getValue(object);
            rv = localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
            initTCObjectSelfIfRequired(rv);

            if (rv == null && logger.isDebugEnabled()) {
              logger.debug("XXX GetById failed when localCacheStoreValue was null for eventual, ObjectID=" + oid);
            }
            break;
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("XXX GetById failed when it couldn't find in any stores, ObjectID=" + oid);
        }
      } finally {
        tcObjectStoreLock.readLock().unlock();
      }

      if (rv != null) { return rv; }

      long currTime = System.currentTimeMillis();
      if ((currTime - timePrev) > (15 * 1000)) {
        timePrev = currTime;
        logger.warn("Still waiting to get the Object from local cache, ObjectID=" + oid + " , times spent="
                    + ((currTime - startTime) / 1000) + "seconds");
      }
      waitUntilNotified();
      // Retry to get the object id
    }
  }

  private void waitUntilNotified() {
    boolean isInterrupted = false;
    try {
      // since i know I am going to wait, let me wait on client lock manager instead of this condition
      synchronized (this.tcObjectSelfRemovedFromStoreCallback) {
        this.tcObjectSelfRemovedFromStoreCallback.wait(1000);
      }
    } catch (InterruptedException e) {
      isInterrupted = true;
    } finally {
      if (isInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void initTCObjectSelfIfRequired(Object rv) {
    if (rv instanceof TCObjectSelf) {
      initializeTCObjectSelfIfRequired((TCObjectSelf) rv);
    }
  }

  public void initializeTCObjectSelfIfRequired(TCObjectSelf tcoSelf) {
    if (tcoSelf != null) {
      tcObjectSelfRemovedFromStoreCallback.initializeTCClazzIfRequired(tcoSelf);
    }
  }

  public Object getByIdFromCache(ObjectID oid, ServerMapLocalCache localCache) {
    tcObjectStoreLock.readLock().lock();
    try {
      TCObjectSelf self = tcObjectSelfTempCache.get(oid);
      if (self != null) { return self; }

      if (!tcObjectSelfStoreOids.contains(oid)) { return null; }

      Object object = localCache.getKeyOrValueForObjectID(oid);
      if (object == null) { return null; }
      final Object rv;
      if (object instanceof TCObjectSelfStoreValue) {
        rv = ((TCObjectSelfStoreValue) object).getTCObjectSelf();
      } else {
        // for eventual value invalidation, use any of them to look up the value
        AbstractLocalCacheStoreValue localCacheStoreValue = localCache.getValue(object);
        rv = localCacheStoreValue == null ? null : localCacheStoreValue.asEventualValue().getValue();
      }
      initTCObjectSelfIfRequired(rv);
      return rv;
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void addTCObjectSelfTemp(TCObjectSelf tcObjectSelf) {
    tcObjectStoreLock.writeLock().lock();
    try {
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Adding TCObjectSelf to temp cache, ObjectID=" + tcObjectSelf.getObjectID());
      }
      this.tcObjectSelfTempCache.put(tcObjectSelf.getObjectID(), tcObjectSelf);
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public boolean addTCObjectSelf(L1ServerMapLocalCacheStore store, AbstractLocalCacheStoreValue localStoreValue,
                                 Object tcoself, final boolean isNew) {
    tcObjectStoreLock.writeLock().lock();
    try {
      if (tcoself instanceof TCObject) {
        // no need of instanceof check if tcoself is declared as TCObject only... skipping for tests.. refactor later
        ObjectID oid = ((TCObject) tcoself).getObjectID();
        if (logger.isDebugEnabled()) {
          logger.debug("XXX Adding TCObjectSelf to Store if necessary, ObjectID=" + oid);
        }

        if (isNew || (tcObjectSelfTempCache.containsKey(oid) && !tcObjectSelfStoreOids.contains(oid))) {
          tcObjectSelfStoreOids.add(localStoreValue.isEventualConsistentValue(), oid);
          if (!localStoreValue.isEventualConsistentValue()) {
            store.put(((TCObject) tcoself).getObjectID(), new TCObjectSelfWrapper(tcoself),
                      PutType.PINNED_NO_SIZE_INCREMENT);
          } // else no need to store another mapping as for eventual already oid->localCacheEventualValue mapping
          // exists,
          // and actual value is present in the localCacheEventualValue
          return true;
        } else {
          return false;
        }
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }

    return true;
  }

  public void removeTCObjectSelfTemp(TCObjectSelf objectSelf, boolean notifyServer) {
    if (objectSelf == null) { return; }

    if (notifyServer) {
      if (logger.isDebugEnabled()) {
        logger.debug("XXX Removing TCObjectSelf from temp cache, ObjectID=" + objectSelf.getObjectID());
      }

      tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(objectSelf);
    }

    // Tiny race left to resolve here ...
    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfTempCache.remove(objectSelf.getObjectID());
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public void removeTCObjectSelf(AbstractLocalCacheStoreValue localStoreValue) {
    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      tcObjectStoreLock.writeLock().lock();

      try {
        ObjectID valueOid = localStoreValue.getObjectId();
        if (!ObjectID.NULL_ID.equals(valueOid)) {
          tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(valueOid);
          tcObjectSelfStoreOids.remove(localStoreValue.isEventualConsistentValue(), valueOid);
        }
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }
    }
  }

  public void removeTCObjectSelf(ServerMapLocalCache serverMapLocalCache, AbstractLocalCacheStoreValue localStoreValue) {
    synchronized (tcObjectSelfRemovedFromStoreCallback) {
      Object removed = null;
      ObjectID valueOid = localStoreValue.getObjectId();
      tcObjectStoreLock.writeLock().lock();
      try {
        if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) {
          if (logger.isDebugEnabled()) {
            logger.debug("XXX Removing TCObjectSelf from Store Failed, ObjectID=" + valueOid
                         + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid));
          }
          return;
        }

        // some asertions... can be removed?
        if (localStoreValue.isEventualConsistentValue()) {
          removed = localStoreValue.asEventualValue().getValue();
        } else {
          Object object = serverMapLocalCache.getInternalStore().get(valueOid);
          if (object != null) {
            if (!(object instanceof TCObjectSelfStoreValue)) { throw new AssertionError(
                                                                                        "Object mapped by oid is not TCObjectSelfStoreValue, oid: "
                                                                                            + valueOid + ", value: "
                                                                                            + object); }
            removed = serverMapLocalCache.getInternalStore().remove(valueOid, RemoveType.NO_SIZE_DECREMENT);
            removed = ((TCObjectSelfStoreValue) removed).getTCObjectSelf();
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("XXX Removing TCObjectSelf from Store, ObjectID=" + valueOid
                       + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid)
                       + " removed==null " + (removed == null));
        }

        // TODO: remove the cast to TCObjectSelf, right now done to appease unit tests
        // to avoid deadlock, do this outside lock
        if (removed instanceof TCObjectSelf) {
          if (!((TCObjectSelf) removed).getObjectID().equals(valueOid)) { throw new AssertionError(
                                                                                                   "valueOid "
                                                                                                       + valueOid
                                                                                                       + " and "
                                                                                                       + ((TCObjectSelf) removed)
                                                                                                           .getObjectID()
                                                                                                       + " do not match. isEventual="
                                                                                                       + localStoreValue
                                                                                                           .isEventualConsistentValue()); }

          this.tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore((TCObjectSelf) removed);
        }

        tcObjectSelfStoreOids.remove(localStoreValue.isEventualConsistentValue(), valueOid);
      } finally {
        tcObjectStoreLock.writeLock().unlock();
      }

      this.tcObjectSelfRemovedFromStoreCallback.notifyAll();
    }
  }

  public void addAllObjectIDsToValidate(Invalidations invalidations) {
    tcObjectStoreLock.writeLock().lock();
    try {
      tcObjectSelfStoreOids.addAllObjectIDsToValidate(invalidations);
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  public int size() {
    tcObjectStoreLock.readLock().lock();
    try {
      return tcObjectSelfStoreOids.size();
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void addAllObjectIDs(Set oids) {
    tcObjectStoreLock.readLock().lock();
    try {
      tcObjectSelfStoreOids.addAll(oids);
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public boolean contains(ObjectID objectID) {
    tcObjectStoreLock.readLock().lock();
    try {
      return this.tcObjectSelfTempCache.containsKey(objectID) || this.tcObjectSelfStoreOids.contains(objectID);
    } finally {
      tcObjectStoreLock.readLock().unlock();
    }
  }

  public void initializeTCObjectSelfStore(TCObjectSelfCallback callback) {
    this.tcObjectSelfRemovedFromStoreCallback = callback;
  }

  private static class TCObjectSelfStoreObjectIDSet {
    private final ObjectIDSet nonEventualIds = new ObjectIDSet();
    private final ObjectIDSet eventualIds    = new ObjectIDSet();

    public void add(boolean isEventual, ObjectID id) {
      if (isEventual) {
        eventualIds.add(id);
      } else {
        nonEventualIds.add(id);
      }
    }

    public void remove(boolean isEventual, ObjectID id) {
      if (isEventual) {
        eventualIds.remove(id);
      } else {
        nonEventualIds.remove(id);
      }
    }

    public int size() {
      return eventualIds.size() + nonEventualIds.size();
    }

    public boolean contains(ObjectID id) {
      return eventualIds.contains(id) || nonEventualIds.contains(id);
    }

    public void addAllObjectIDsToValidate(Invalidations invalidations) {
      for (ObjectID id : eventualIds) {
        invalidations.add(ObjectID.NULL_ID, id);
      }
    }

    public void addAll(Set oids) {
      oids.addAll(eventualIds);
      oids.addAll(nonEventualIds);
    }
  }
}
