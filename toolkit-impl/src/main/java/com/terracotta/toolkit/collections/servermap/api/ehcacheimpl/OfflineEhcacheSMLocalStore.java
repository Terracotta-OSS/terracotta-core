/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
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

  public OfflineEhcacheSMLocalStore(Ehcache localEhcache) {
    this.localEhcache = localEhcache;
  }

  @Override
  public boolean addListener(ServerMapLocalStoreListener<Object, Object> listener) {
    LOGGER.info("Ignoring addListener: " + listener + " as inner cache is not alive.");
    return false;
  }

  @Override
  public boolean removeListener(ServerMapLocalStoreListener<Object, Object> listener) {
    LOGGER.info("Ignoring removeListener: " + listener + " as inner cache is not alive.");
    return false;
  }

  @Override
  public Object get(Object key) {
    LOGGER.info("Ignoring get for key: " + key + " as inner cache is not alive.");
    return null;
  }

  @Override
  public List<Object> getKeys() {
    LOGGER.info("Ignoring getKeySet as inner cache is not alive.");
    return Collections.EMPTY_LIST;
  }

  @Override
  public Object put(Object key, Object value) {
    LOGGER.info("Ignoring put for key: " + key + ", value: " + value + " as inner cache is not alive.");
    return null;
  }

  @Override
  public Object remove(Object key) {
    LOGGER.info("Ignoring remove for key " + key + " as inner cache is not alive.");
    return null;
  }

  @Override
  public Object remove(Object key, Object value) {
    LOGGER.info("Ignoring remove for key " + key + " as inner cache is not alive.");
    return null;
  }

  @Override
  public int getMaxEntriesLocalHeap() {
    // safe to use config even when offline
    return (int) localEhcache.getCacheConfiguration().getMaxEntriesLocalHeap();
  }

  @Override
  public void setMaxEntriesLocalHeap(int newValue) {
    LOGGER.info("Ignoring setMaxEntriesLocalHeap with value: " + newValue + " as inner cache is not alive.");
  }

  @Override
  public void unpinAll() {
    LOGGER.info("Ignoring unpinAll as inner cache is not alive.");
  }

  @Override
  public boolean isPinned(Object key) {
    LOGGER.info("Ignoring isPinned for key: " + key + " as inner cache is not alive.");
    return false;
  }

  @Override
  public void setPinned(Object key, boolean pinned) {
    LOGGER.info("Ignoring setPinned for key: " + key + ", pinned: " + pinned + " as inner cache is not alive.");
  }

  @Override
  public void clear() {
    LOGGER.info("Ignoring clear as inner cache is not alive.");
  }

  @Override
  public void cleanLocalState() {
    LOGGER.info("Ignoring cleanLocalState as inner cache is not alive.");
  }

  @Override
  public long getOnHeapSizeInBytes() {
    LOGGER.info("Ignoring getOnHeapSizeInBytes as inner cache is not alive.");
    return 0;
  }

  @Override
  public long getOffHeapSizeInBytes() {
    LOGGER.info("Ignoring getOffHeapSizeInBytes as inner cache is not alive.");
    return 0;
  }

  @Override
  public int getOffHeapSize() {
    LOGGER.info("Ignoring getOffHeapSize as inner cache is not alive.");
    return 0;
  }

  @Override
  public int getOnHeapSize() {
    LOGGER.info("Ignoring getOnHeapSize as inner cache is not alive.");
    return 0;
  }

  @Override
  public int getSize() {
    LOGGER.info("Ignoring getSize as inner cache is not alive.");
    return 0;
  }

  @Override
  public void dispose() {
    // no-op
  }

  @Override
  public boolean containsKeyOnHeap(Object key) {
    LOGGER.info("Ignoring containsKeyOnHeap for key: " + key + " as inner cache is not alive.");
    return false;
  }

  @Override
  public boolean containsKeyOffHeap(Object key) {
    LOGGER.info("Ignoring containsKeyOffHeap for key: " + key + " as inner cache is not alive.");
    return false;
  }

  @Override
  public void setMaxBytesLocalHeap(long newMaxBytesLocalHeap) {
    LOGGER.info("Ignoring setMaxBytesLocalHeap with value: " + newMaxBytesLocalHeap + " as inner cache is not alive.");
  }

  @Override
  public long getMaxBytesLocalHeap() {
    // can use config even when offline
    return localEhcache.getCacheConfiguration().getMaxBytesLocalHeap();
  }

  @Override
  public void recalculateSize(Object key) {
    LOGGER.info("Ignoring recalculateSize as inner cache is not alive.");
    return;
  }

  @Override
  public boolean isLocalHeapOrMemoryTierPinned() {
    LOGGER.info("Ignoring isLocalHeapOrMemoryTierPinned as inner cache is not alive.");
    return false;
  }

}
