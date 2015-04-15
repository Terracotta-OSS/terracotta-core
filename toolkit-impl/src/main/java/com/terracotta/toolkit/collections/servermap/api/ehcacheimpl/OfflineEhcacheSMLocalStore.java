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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStore;
import com.terracotta.toolkit.collections.servermap.api.ServerMapLocalStoreListener;

import java.util.Collections;
import java.util.List;

public class OfflineEhcacheSMLocalStore implements ServerMapLocalStore<Object, Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EhcacheSMLocalStore.class);
  private final Ehcache       localEhcache;
  private volatile boolean    logged;

  public OfflineEhcacheSMLocalStore(Ehcache localEhcache) {
    this.localEhcache = localEhcache;
  }

  private boolean shouldLog() {
    if (logged) {
      return false;
    } else {
      logged = true;
      return true;
    }
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    if (shouldLog()) {
      LOGGER.info("Ignoring addListener: " + listener + " as inner cache is not alive.");
    }
    return false;
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    if (shouldLog()) {
      LOGGER.info("Ignoring removeListener: " + listener + " as inner cache is not alive.");
    }
    return false;
  }

  @Override
  public Object get(Object key) {
    if (shouldLog()) {
      LOGGER.info("Ignoring get as inner cache is not alive.");
    }
    return null;
  }

  @Override
  public List<Object> getKeys() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getKeySet as inner cache is not alive.");
    }
    return Collections.EMPTY_LIST;
  }

  @Override
  public Object put(Object key, Object value) {
    if (shouldLog()) {
      LOGGER.info("Ignoring put as inner cache is not alive.");
    }
    return null;
  }

  @Override
  public Object remove(Object key) {
    if (shouldLog()) {
      LOGGER.info("Ignoring remove for key as inner cache is not alive.");
    }
    return null;
  }

  @Override
  public Object remove(Object key, Object value) {
    if (shouldLog()) {
      LOGGER.info("Ignoring remove as inner cache is not alive.");
    }
    return null;
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    // safe to use config even when offline
    return (int) localEhcache.getCacheConfiguration().getMaxEntriesLocalHeap();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    if (shouldLog()) {
      LOGGER.info("Ignoring setMaxEntriesLocalHeap with value: " + newValue + " as inner cache is not alive.");
    }
  }

  @Override
  public void clear() {
    if (shouldLog()) {
      LOGGER.info("Ignoring clear as inner cache is not alive.");
    }
  }

  @Override
  public void cleanLocalState() {
    if (shouldLog()) {
      LOGGER.info("Ignoring cleanLocalState as inner cache is not alive.");
    }
  }

  @Override
  public long getOnHeapSizeInBytes() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getOnHeapSizeInBytes as inner cache is not alive.");
    }
    return 0;
  }

  @Override
  public long getOffHeapSizeInBytes() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getOffHeapSizeInBytes as inner cache is not alive.");
    }
    return 0;
  }

  @Override
  public int getOffHeapSize() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getOffHeapSize as inner cache is not alive.");
    }
    return 0;
  }

  @Override
  public int getOnHeapSize() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getOnHeapSize as inner cache is not alive.");
    }
    return 0;
  }

  @Override
  public int getSize() {
    if (shouldLog()) {
      LOGGER.info("Ignoring getSize as inner cache is not alive.");
    }
    return 0;
  }

  @Override
  public void dispose() {
    // no-op
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    if (shouldLog()) {
      LOGGER.info("Ignoring containsKeyOnHeap as inner cache is not alive.");
    }
    return false;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    if (shouldLog()) {
      LOGGER.info("Ignoring containsKeyOffHeap as inner cache is not alive.");
    }
    return false;
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    if (shouldLog()) {
      LOGGER.info("Ignoring setMaxBytesLocalHeap with value: " + newMaxBytesLocalHeap + " as inner cache is not alive.");
    }
  }

  @Override
  public long getMaxBytesLocalHeap() {
    // can use config even when offline
    return localEhcache.getCacheConfiguration().getMaxBytesLocalHeap();
  }

  @Override
  public void recalculateSize(Object key) {
    if (shouldLog()) {
      LOGGER.info("Ignoring recalculateSize as inner cache is not alive.");
    }
  }

  @Override
  public boolean isPinned() {
    if (shouldLog()) {
      LOGGER.info("Ignoring isLocalHeapOrMemoryTierPinned as inner cache is not alive.");
    }
    return false;
  }

}
