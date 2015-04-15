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
package com.terracotta.toolkit.collections.servermap.api;

import org.terracotta.toolkit.config.Configuration;
import org.terracotta.toolkit.internal.store.ConfigFieldsInternal;
import org.terracotta.toolkit.store.ToolkitConfigFields;

public class ServerMapLocalStoreConfigParameters {

  private volatile String  localStoreManagerName = ConfigFieldsInternal.DEFAULT_LOCAL_STORE_MANAGER_NAME;
  private volatile String  localStoreName        = "";
  private volatile boolean pinnedInLocalMemory   = ToolkitConfigFields.DEFAULT_PINNED_IN_LOCAL_MEMORY;

  private volatile long    maxBytesLocalHeap     = ToolkitConfigFields.DEFAULT_MAX_BYTES_LOCAL_HEAP;
  private volatile long    maxBytesLocalOffheap  = ToolkitConfigFields.DEFAULT_MAX_BYTES_LOCAL_OFFHEAP;
  private volatile int     maxCountLocalHeap     = ToolkitConfigFields.DEFAULT_MAX_COUNT_LOCAL_HEAP;
  private volatile boolean overflowToOffheap     = ToolkitConfigFields.DEFAULT_OFFHEAP_ENABLED;

  public ServerMapLocalStoreConfigParameters populateFrom(Configuration config, String name) {

    this.localStoreName = name;

    if (config.hasField(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME)) {
      this.localStoreManagerName(config.getString(ConfigFieldsInternal.LOCAL_STORE_MANAGER_NAME_NAME));
    }

    if (config.hasField(ToolkitConfigFields.PINNED_IN_LOCAL_MEMORY_FIELD_NAME)) {
      this.pinnedInLocalMemory(config.getBoolean(ToolkitConfigFields.PINNED_IN_LOCAL_MEMORY_FIELD_NAME));
    }

    if (config.hasField(ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME)) {
      this.maxBytesLocalHeap(config.getLong(ToolkitConfigFields.MAX_BYTES_LOCAL_HEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME)) {
      this.maxBytesLocalOffheap(config.getLong(ToolkitConfigFields.MAX_BYTES_LOCAL_OFFHEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME)) {
      this.maxCountLocalHeap(config.getInt(ToolkitConfigFields.MAX_COUNT_LOCAL_HEAP_FIELD_NAME));
    }

    if (config.hasField(ToolkitConfigFields.OFFHEAP_ENABLED_FIELD_NAME)) {
      this.overflowToOffheap(config.getBoolean(ToolkitConfigFields.OFFHEAP_ENABLED_FIELD_NAME));
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

  public boolean isPinnedInLocalMemory() {
    return pinnedInLocalMemory;
  }

  public ServerMapLocalStoreConfigParameters pinnedInLocalMemory(boolean pinnedLocalMemory) {
    this.pinnedInLocalMemory = pinnedLocalMemory;
    return this;
  }
}
