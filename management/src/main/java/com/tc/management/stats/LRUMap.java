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
package com.tc.management.stats;

import java.util.LinkedHashMap;
import java.util.Map;

public class LRUMap extends LinkedHashMap {
  private final static int NO_LIMIT = -1;

  private final int maxSize;

  public LRUMap() {
    this(NO_LIMIT);
  }

  public LRUMap(int maxSize) {
    super(100, 0.75f, true);
    this.maxSize = maxSize;
  }

  protected boolean removeEldestEntry(Map.Entry eldest) {
    if (maxSize != NO_LIMIT) {
      return size() >= this.maxSize;
    } else {
      return false;
    }
  }
}
