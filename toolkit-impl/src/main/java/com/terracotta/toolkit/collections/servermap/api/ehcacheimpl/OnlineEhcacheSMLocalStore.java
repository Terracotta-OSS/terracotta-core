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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PinningConfiguration;
import net.sf.ehcache.config.PinningConfiguration.Store;
import net.sf.ehcache.terracotta.InternalEhcache;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreFullException;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.ArrayList;
import java.util.List;

public class OnlineEhcacheSMLocalStore implements ServerMapLocalStore<Object, Object> {

  private final InternalEhcache                localStoreCache;
  private final EhcacheSMLocalStoreUTF8Encoder encoder;

  public OnlineEhcacheSMLocalStore(InternalEhcache localStoreCache) {
    this.localStoreCache = localStoreCache;
    // enable Encoding only when Offheap is enabled.
    this.encoder = new EhcacheSMLocalStoreUTF8Encoder(localStoreCache.getCacheConfiguration().isOverflowToOffHeap());
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return localStoreCache.getCacheEventNotificationService()
        .registerListener(new EhcacheSMLocalStoreListenerAdapter(listener, encoder));
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    return localStoreCache.getCacheEventNotificationService()
        .unregisterListener(new EhcacheSMLocalStoreListenerAdapter(listener, encoder));
  }

  @Override
  public Object get(Object key) {
    Element element = localStoreCache.get(encode(key));
    return element == null ? null : element.getObjectValue();
  }

  @Override
  public List<Object> getKeys() {
    List encodedKeys = this.localStoreCache.getKeys();
    List rv = new ArrayList(encodedKeys.size());
    for (Object key : encodedKeys) {
      rv.add(decode(key));
    }
    return rv;
  }

  @Override
  public Object put(Object key, Object value) throws ServerMapLocalStoreFullException {
    try {
      Object encodedKey = encode(key);
      Element oldElement = localStoreCache.removeAndReturnElement(encodedKey);
      localStoreCache.put(new Element(encodedKey,value));
      return oldElement == null ? null : oldElement.getObjectValue();
    } catch (CacheException e) {
      handleCacheException(e);
      throw e;
    }
  }

  @Override
  public Object remove(Object key) {
    Element element = localStoreCache.removeAndReturnElement(encode(key));
    return element == null ? null : element.getObjectValue();
  }

  @Override
  public Object remove(Object key, Object value) {
    Object encodedKey = encode(key);
    Element element = localStoreCache.get(encodedKey);
    if (element == null || !value.equals(element.getObjectValue())) { return null; }
    if ( localStoreCache.removeElement(element) ) { 
      return element.getObjectValue(); 
    }
    return null;
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    return (int) localStoreCache.getCacheConfiguration().getMaxEntriesLocalHeap();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    localStoreCache.getCacheConfiguration().setMaxEntriesLocalHeap(newValue);
  }

  private void handleCacheException(CacheException ce) throws ServerMapLocalStoreFullException, CacheException {
    Throwable rootCause = getRootCause(ce);
    if (rootCause.getClass().getName().contains("OversizeMappingException")
        || rootCause.getClass().getName().contains("CrossPoolEvictionException")) {
      throw new ServerMapLocalStoreFullException();
    } else {
      throw ce;
    }
  }

  private Throwable getRootCause(Throwable throwable) {
    Throwable t = throwable;
    if (t == null) { throw new AssertionError("Tried to find the root cause of null"); }
    while (t.getCause() != null) {
      t = t.getCause();
    }
    return t;
  }

  @Override
  public void clear() {
    localStoreCache.removeAll();
  }

  @Override
  public void cleanLocalState() {
    // no need to notify listeners
    localStoreCache.removeAll(true);
  }

  @Override
  public long getOnHeapSizeInBytes() {
    return localStoreCache.calculateInMemorySize();
  }

  @Override
  public long getOffHeapSizeInBytes() {
    return localStoreCache.calculateOffHeapSize();
  }

  @Override
  public int getOffHeapSize() {
    return (int) localStoreCache.getOffHeapStoreSize();
  }

  @Override
  public int getOnHeapSize() {
    return (int) localStoreCache.getMemoryStoreSize();
  }

  @Override
  public int getSize() {
    return localStoreCache.getSize();
  }

  @Override
  public void dispose() {
    //
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    return localStoreCache.isElementInMemory(encode(key));
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    // Offheap has everything in the local cache, so we just need to verify that the key is anywhere in the cache
    CacheConfiguration conf = localStoreCache.getCacheConfiguration();
    if (conf.isOverflowToOffHeap()) { return localStoreCache.isKeyInCache(encode(key)); }
    return false;
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    localStoreCache.getCacheConfiguration().setMaxBytesLocalHeap(newMaxBytesLocalHeap);
  }

  @Override
  public long getMaxBytesLocalHeap() {
    return localStoreCache.getCacheConfiguration().getMaxBytesLocalHeap();
  }

  @Override
  public void recalculateSize(Object key) {
    localStoreCache.recalculateSize(encode(key));
  }

  @Override
  public boolean isPinned() {
    PinningConfiguration pinningConfiguration = localStoreCache.getCacheConfiguration().getPinningConfiguration();
    if (pinningConfiguration == null) {
      return false;
    } else {
      return pinningConfiguration.getStore() == Store.LOCALMEMORY;
    }
  }

  public Object encode(Object key) {
    return encoder.encodeKey(key);
  }

  public Object decode(Object key) {
    return encoder.decodeKey(key);
  }

}
