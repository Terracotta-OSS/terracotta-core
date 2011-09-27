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
import com.tc.object.servermap.localcache.AbstractLocalCacheStoreValue;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheStore;
import com.tc.object.servermap.localcache.LocalCacheStoreEventualValue;
import com.tc.object.servermap.localcache.LocalCacheStoreIncoherentValue;
import com.tc.object.servermap.localcache.LocalCacheStoreStrongValue;
import com.tc.object.servermap.localcache.PutType;
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

    try {
      if (!tcObjectSelfStoreOids.contains(oid)) { return; }
      for (ServerMapLocalCache cache : localCaches.keySet()) {
        Object key = cache.getValue(oid);
        if (key != null) {
          AbstractLocalCacheStoreValue value = (AbstractLocalCacheStoreValue) cache.getValue(key);
          if (value != null && value.getValueObjectId().equals(oid)) {
            cache.getInternalStore().replace(key, value, createNewValue(value, self), PutType.NORMAL);
          }
        }
      }
    } finally {
      tcObjectStoreLock.writeLock().unlock();
    }
  }

  private AbstractLocalCacheStoreValue createNewValue(AbstractLocalCacheStoreValue oldValue, TCObjectSelf self) {
    final AbstractLocalCacheStoreValue newValue;
    if (oldValue.isEventualConsistentValue()) {
      newValue = new LocalCacheStoreEventualValue(self.getObjectID(), self);
    } else if (oldValue.isStrongConsistentValue()) {
      newValue = new LocalCacheStoreStrongValue(oldValue.getLockId(), self, oldValue.getValueObjectId());
    } else if (oldValue.isIncoherentValue()) {
      newValue = new LocalCacheStoreIncoherentValue(self.getObjectID(), self);
    } else {
      throw new AssertionError("Neither strong, incoherent or eventual");
    }
    return newValue;
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
          Object key = localCache.getValue(oid);
          if (key == null) {
            continue;
          }

          AbstractLocalCacheStoreValue localCacheStoreValue = (AbstractLocalCacheStoreValue) localCache.getValue(key);
          rv = localCacheStoreValue == null ? null : localCacheStoreValue.getValueObject();
          initTCObjectSelfIfRequired(rv);

          if (rv == null && logger.isDebugEnabled()) {
            logger.debug("XXX GetById failed when localCacheStoreValue was null for eventual, ObjectID=" + oid);
          }
          break;
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
        if (!(localStoreValue.getValueObject() instanceof TCObjectSelf)) { return; }
        TCObjectSelf self = (TCObjectSelf) localStoreValue.getValueObject();
        ObjectID valueOid = self.getObjectID();

        if (ObjectID.NULL_ID.equals(valueOid) || !tcObjectSelfStoreOids.contains(valueOid)) {
          if (logger.isDebugEnabled()) {
            logger.debug("XXX Removing TCObjectSelf from Store Failed, ObjectID=" + valueOid
                         + " , TCObjectSelfStore contains it = " + tcObjectSelfStoreOids.contains(valueOid));
          }
          return;
        }

        tcObjectSelfRemovedFromStoreCallback.removedTCObjectSelfFromStore(self);
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
