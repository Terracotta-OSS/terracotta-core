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
package com.tc.runtime;

class Jdk15MemoryUsage implements MemoryUsage {

  private final long   max;
  private final long   free;
  private final long   used;
  private final int    usedPercentage;
  private final String desc;
  private final long   collectionCount;
  private final long   collectionTime;

  public Jdk15MemoryUsage(java.lang.management.MemoryUsage stats, String desc, long collectionCount, long collectionTime) {
    long statsMax = stats.getMax();
    if (statsMax <= 0) {
      this.max = stats.getCommitted();
    } else {
      this.max = statsMax;
    }
    this.used = stats.getUsed();
    this.free = this.max - this.used;
    this.usedPercentage = (int) (this.used * 100 / this.max);
    this.desc = desc;
    this.collectionCount = collectionCount;
    this.collectionTime = collectionTime;
  }

  // CollectionCount is not supported
  public Jdk15MemoryUsage(java.lang.management.MemoryUsage usage, String desc) {
    this(usage, desc, -1, -1);
  }

  @Override
  public String getDescription() {
    return desc;
  }

  @Override
  public long getFreeMemory() {
    return free;
  }

  @Override
  public int getUsedPercentage() {
    return usedPercentage;
  }

  @Override
  public long getMaxMemory() {
    return max;
  }

  @Override
  public long getUsedMemory() {
    return used;
  }

  @Override
  public String toString() {
    return "Jdk15MemoryUsage ( max = " + max + ", used = " + used + ", free = " + free + ", used % = " + usedPercentage
           + ", collectionCount = " + collectionCount + " )";
  }

  @Override
  public long getCollectionCount() {
    return collectionCount;
  }

  @Override
  public long getCollectionTime() {
    return collectionTime;
  }
}
