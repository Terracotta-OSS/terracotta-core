/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;

import org.terracotta.toolkit.cache.ToolkitCacheConfigFields;
import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.store.ToolkitStoreConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitStoreConfigFields;

public class ServerMapLocalStoreConfigParameters {

  private volatile String  localStoreManagerName = ToolkitStoreConfigFieldsInternal.DEFAULT_LOCAL_STORE_MANAGER_NAME;
  private volatile String  localStoreName        = "";
  private volatile String  pinningStore          = ToolkitCacheConfigFields.DEFAULT_PINNING_STORE;

  private volatile long    maxBytesLocalHeap     = ToolkitStoreConfigFields.DEFAULT_MAX_BYTES_LOCAL_HEAP;
  private volatile long    maxBytesLocalOffheap  = ToolkitStoreConfigFields.DEFAULT_MAX_BYTES_LOCAL_OFFHEAP;
  private volatile int     maxCountLocalHeap     = ToolkitStoreConfigFields.DEFAULT_MAX_COUNT_LOCAL_HEAP;
  private volatile boolean overflowToOffheap     = ToolkitStoreConfigFields.DEFAULT_OFFHEAP_ENABLED;

  public ServerMapLocalStoreConfigParameters populateFrom(Configuration config, String name) {

    this.localStoreName = name;

    if (config.hasField(ToolkitStoreConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME)) {
      this.localStoreManagerName(config.getString(ToolkitStoreConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME));
    }

    if (config.hasField(ToolkitCacheConfigFields.PINNING_STORE_FIELD_NAME)) {
      this.pinningStore(config.getString(ToolkitCacheConfigFields.PINNING_STORE_FIELD_NAME));
    }

    if (config.hasField(ToolkitStoreConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME)) {
      this.maxBytesLocalHeap(config.getLong(ToolkitStoreConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitStoreConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME)) {
      this.maxBytesLocalOffheap(config.getLong(ToolkitStoreConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitStoreConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME)) {
      this.maxCountLocalHeap(config.getInt(ToolkitStoreConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitStoreConfigFields.OFFHEAP_ENABLED_FIELD_NAME)) {
      this.overflowToOffheap(config.getBoolean(ToolkitStoreConfigFields.OFFHEAP_ENABLED_FIELD_NAME));
    }
    return this;
  }

  public String getLocalStoreManagerName() {
    return localStoreManagerName;
  }

  public ServerMapLocalStoreConfigParameters localStoreManagerName(String newLocalStoreManagerName) {
    this.localStoreManagerName = newLocalStoreManagerName;
    return this;
  }

  public String getLocalStoreName() {
    return localStoreName;
  }

  public ServerMapLocalStoreConfigParameters localStoreName(String newLocalStoreName) {
    this.localStoreName = newLocalStoreName;
    return this;
  }

  public long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap;
  }

  public ServerMapLocalStoreConfigParameters maxBytesLocalHeap(long newMaxBytesLocalHeap) {
    this.maxBytesLocalHeap = newMaxBytesLocalHeap;
    return this;
  }

  public int getMaxCountLocalHeap() {
    return maxCountLocalHeap;
  }

  public ServerMapLocalStoreConfigParameters maxCountLocalHeap(int newMaxCountLocalHeap) {
    this.maxCountLocalHeap = newMaxCountLocalHeap;
    return this;
  }

  public long getMaxBytesLocalOffheap() {
    return maxBytesLocalOffheap;
  }

  public ServerMapLocalStoreConfigParameters maxBytesLocalOffheap(long newMaxBytesLocalOffheap) {
    this.maxBytesLocalOffheap = newMaxBytesLocalOffheap;
    return this;
  }

  public boolean isOverflowToOffheap() {
    return overflowToOffheap;
  }

  public ServerMapLocalStoreConfigParameters overflowToOffheap(boolean newOverflowToOffheap) {
    this.overflowToOffheap = newOverflowToOffheap;
    return this;
  }

  public ServerMapLocalStoreConfig buildConfig() {
    return new ServerMapLocalStoreConfig(this);
  }

  public String getPinningStore() {
    return pinningStore;
  }

  public ServerMapLocalStoreConfigParameters pinningStore(String value) {
    this.pinningStore = value;
    return this;
  }
}
