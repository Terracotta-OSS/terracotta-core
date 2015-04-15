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
