/*
 *  Copyright Terracotta, Inc.
 *  Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
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
