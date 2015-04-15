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

public interface MemoryUsage {

  public long getFreeMemory();

  public String getDescription();

  /* If -Xmx flag is not specified, this might not be correct or consistent over time */
  public long getMaxMemory();

  public long getUsedMemory();

  public int getUsedPercentage();

  /**
   * @return - the number of times GC was executed (on this memory pool, if the usage is for a specific memory pool)
   *         since the beginning. -1 if this is not supported.
   */
  public long getCollectionCount();

  /**
   * @return -1 if not supported
   */
  public long getCollectionTime();
}
