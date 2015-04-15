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
package com.terracotta.toolkit.collections.servermap.api.ehcacheimpl;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Status;
import net.sf.ehcache.event.CacheEventListenerAdapter;
import net.sf.ehcache.terracotta.InternalEhcache;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class EhcacheSMLocalStore implements ServerMapLocalStore<Object, Object> {

  private final InternalEhcache            localStoreCache;
  private final Lock                       writeLock;
  private final Lock                       readLock;
  private boolean                          running = true;
  private final OfflineEhcacheSMLocalStore offlineStore;
  private final OnlineEhcacheSMLocalStore  onlineStore;

  public EhcacheSMLocalStore(InternalEhcache localStoreCache) {

    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    writeLock = lock.writeLock();
    readLock = lock.readLock();

    this.localStoreCache = localStoreCache;
    this.localStoreCache.getCacheEventNotificationService().registerListener(new CacheEventListenerAdapter() {
      @Override
      public void dispose() {
        cacheDisposed();
      }
    });
    // Statistics not required for local caches.
    offlineStore = new OfflineEhcacheSMLocalStore(localStoreCache);
    onlineStore = new OnlineEhcacheSMLocalStore(localStoreCache);
  }

  private void cacheDisposed() {
    writeLock.lock();
    try {
      running = false;
    } finally {
      writeLock.unlock();
    }
  }

  private ServerMapLocalStore getActiveStore() {
    if (running && localStoreCache.getStatus() == Status.STATUS_ALIVE) {
      return onlineStore;
    } else {
      return offlineStore;
    }
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    readLock.lock();
    try {
      return getActiveStore().addListener(listener);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    readLock.lock();
    try {
      return getActiveStore().removeListener(listener);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object get(Object key) {
    readLock.lock();
    try {
      return getActiveStore().get(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public List<Object> getKeys() {
    readLock.lock();
    try {
      return getActiveStore().getKeys();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object put(Object key, Object value) throws ServerMapLocalStoreFullException {
    readLock.lock();
    try {
      return getActiveStore().put(key, value);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object remove(Object key) {
    readLock.lock();
    try {
      return getActiveStore().remove(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public Object remove(Object key, Object value) {
    readLock.lock();
    try {
      return getActiveStore().remove(key, value);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    readLock.lock();
    try {
      return getActiveStore().getMaxEntriesLocalHeap();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    readLock.lock();
    try {
      getActiveStore().setMaxEntriesLocalHeap(newValue);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void clear() {
    readLock.lock();
    try {
      getActiveStore().clear();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void cleanLocalState() {
    readLock.lock();
    try {
      getActiveStore().cleanLocalState();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public long getOnHeapSizeInBytes() {
    readLock.lock();
    try {
      return getActiveStore().getOnHeapSizeInBytes();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public long getOffHeapSizeInBytes() {
    readLock.lock();
    try {
      return getActiveStore().getOffHeapSizeInBytes();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getOffHeapSize() {
    readLock.lock();
    try {
      return getActiveStore().getOffHeapSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getOnHeapSize() {
    readLock.lock();
    try {
      return getActiveStore().getOnHeapSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public int getSize() {
    readLock.lock();
    try {
      return getActiveStore().getSize();
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void dispose() {
    cacheDisposed();
    localStoreCache.dispose();
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    readLock.lock();
    try {
      return getActiveStore().containsKeyOnHeap(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    readLock.lock();
    try {
      return getActiveStore().containsKeyOffHeap(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    readLock.lock();
    try {
      getActiveStore().setMaxBytesLocalHeap(newMaxBytesLocalHeap);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public long getMaxBytesLocalHeap() {
    readLock.lock();
    try {
      return getActiveStore().getMaxBytesLocalHeap();
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Used in tests
   */
  Ehcache getLocalEhcache() {
    return localStoreCache;
  }

  @Override
  public void recalculateSize(Object key) {
    readLock.lock();
    try {
      getActiveStore().recalculateSize(key);
    } finally {
      readLock.unlock();
    }
  }

  @Override
  public boolean isPinned() {
    readLock.lock();
    try {
      return getActiveStore().isPinned();
    } finally {
      readLock.unlock();
    }
  }

}
