/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.toolkit.collections.servermap.api;

public class ServerMapLocalStoreConfig {

  private final String  localStoreManagerName;
  private final String  localStoreName;
  private final boolean pinnedInLocalMemory;
  private final long    maxBytesLocalHeap;
  private final long    maxBytesLocalOffheap;
  private final int     maxCountLocalHeap;
  private final boolean overflowToOffheap;

  public ServerMapLocalStoreConfig(ServerMapLocalStoreConfigParameters parameters) {
    this.localStoreManagerName = parameters.getLocalStoreManagerName();
    this.localStoreName = parameters.getLocalStoreName();
    if (localStoreName == null || localStoreName.equals("")) {
      //
      throw new AssertionError("Name for the local store cannot be null or empty");
    }
    this.maxBytesLocalHeap = parameters.getMaxBytesLocalHeap();
    this.maxBytesLocalOffheap = parameters.getMaxBytesLocalOffheap();
    this.maxCountLocalHeap = parameters.getMaxCountLocalHeap();
    this.overflowToOffheap = parameters.isOverflowToOffheap();
    this.pinnedInLocalMemory = parameters.isPinnedInLocalMemory();
  }

  public String getLocalStoreManagerName() {
    return localStoreManagerName;
  }

  public String getLocalStoreName() {
    return localStoreName;
  }

  public long getMaxBytesLocalHeap() {
    return maxBytesLocalHeap;
  }

  public long getMaxBytesLocalOffheap() {
    return maxBytesLocalOffheap;
  }

  public int getMaxCountLocalHeap() {
    return maxCountLocalHeap;
  }

  public boolean isOverflowToOffheap() {
    return overflowToOffheap;
  }

  public boolean isPinnedInLocalMemory() {
    return pinnedInLocalMemory;
  }
}
